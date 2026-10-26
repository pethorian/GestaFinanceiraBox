package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.CategoriaDto;
import br.com.gestafinanceira.api.dto.Dtos.CategoriaResumoDto;
import br.com.gestafinanceira.api.dto.Dtos.ResumoGastosDto;
import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.api.dto.Dtos.LancamentoDto;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.GrupoGasto;
import br.com.gestafinanceira.domain.modelo.GrupoLancamento;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import br.com.gestafinanceira.infra.persistencia.Entidades.Categoria;
import br.com.gestafinanceira.infra.persistencia.Entidades.Lancamento;
import br.com.gestafinanceira.infra.persistencia.CategoriaRepo;
import br.com.gestafinanceira.infra.persistencia.LancamentoRepo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LancamentoService {
    /** Um lançamento parcelado tem ao menos 2 parcelas; valores menores são ajustados. */
    static final int PARCELAS_MINIMAS = 2;

    private final LancamentoRepo lancamentos;
    private final CategoriaRepo categorias;
    private final Clock clock;

    public LancamentoService(LancamentoRepo lancamentos, CategoriaRepo categorias, Clock clock) {
        this.lancamentos = lancamentos;
        this.categorias = categorias;
        this.clock = clock;
    }

    /**
     * Expiração preguiçosa e idempotente: todo gasto PONTUAL cujo mês inicial é anterior ao mês de hoje
     * ("ultrapassou") recebe soft delete com motivo EXPIRADO. No próprio mês o gasto ainda aparece.
     * Recorrentes e parcelados nunca são tocados. Chamada no início de toda leitura/cálculo.
     */
    public int expirarPontuaisVencidos() {
        return lancamentos.expirarPontuaisAnteriores(YearMonth.now(clock).atDay(1), LocalDateTime.now(clock));
    }

    public List<LancamentoDto> listar() {
        expirarPontuaisVencidos();
        return lancamentos.findAllByDeletadoEmIsNullOrderByIdAsc().stream().map(LancamentoService::paraDto).toList();
    }

    public LancamentoDto buscar(Long id) {
        expirarPontuaisVencidos();
        return paraDto(entidade(id));
    }

    public LancamentoDto criar(LancamentoDto d) {
        Lancamento l = new Lancamento();
        aplicar(l, d);
        return paraDto(lancamentos.save(l));
    }

    public LancamentoDto atualizar(Long id, LancamentoDto d) {
        Lancamento l = entidade(id);
        aplicar(l, d);
        return paraDto(lancamentos.save(l));
    }

    /** Exclusão lógica (motivo USUARIO): a linha continua no banco. */
    public void remover(Long id) {
        expirarPontuaisVencidos();
        Lancamento l = entidade(id);
        l.deletadoEm = LocalDateTime.now(clock);
        l.motivoExclusao = "USUARIO";
        lancamentos.save(l);
    }

    /** Nova linha de gasto com os valores padrão definidos pelo servidor (mês atual, primeira categoria). */
    public LancamentoDto criarNovo() {
        return criarNovo(TipoLancamento.RECORRENTE);
    }

    /** Idem, com o tipo escolhido (a aba Gastos eventuais cria PONTUAL). */
    public LancamentoDto criarNovo(TipoLancamento tipo) {
        String categoria = categorias.findAllByOrderByOrdemAscNomeAsc().stream().findFirst()
                .map(c -> c.nome).orElse("Outros");
        return criar(new LancamentoDto(null, "Novo gasto", categoria, tipo,
                BigDecimal.ZERO, YearMonth.now(clock), 0));
    }

    public List<GastoCalculo> paraCalculo() {
        expirarPontuaisVencidos();
        return lancamentos.findAllByDeletadoEmIsNullOrderByIdAsc().stream()
                .map(l -> new GastoCalculo(l.tipo, l.valor, YearMonth.from(l.mesInicial), l.parcelas))
                .toList();
    }

    public void removerTodos() {
        lancamentos.excluirTodosAtivos(LocalDateTime.now(clock), "USUARIO");
    }

    /** Lançamentos padrão, com início relativo ao mês atual (equivale a dadosIniciais() do frontend). */
    public void restaurarPadrao() {
        removerTodos();
        regruparCategoriasPadrao();
        YearMonth m = YearMonth.now(clock);
        criarPadrao("Aluguel", "Moradia", TipoLancamento.RECORRENTE, 2200, m, 0);
        criarPadrao("Condomínio e contas", "Casa", TipoLancamento.RECORRENTE, 850, m, 0);
        criarPadrao("Telefone e internet", "Telefone", TipoLancamento.RECORRENTE, 190, m, 0);
        criarPadrao("Mercado e delivery", "Alimentação", TipoLancamento.RECORRENTE, 1500, m, 0);
        criarPadrao("Gastos pessoais", "Pessoal", TipoLancamento.RECORRENTE, 900, m, 0);
        criarPadrao("Notebook", "Pessoal", TipoLancamento.PARCELADO, 480, m.plusMonths(1), 10);
        criarPadrao("Viagem de fim de ano", "Lazer", TipoLancamento.PONTUAL, 3200, m.plusMonths(2), 0);
    }

    private void criarPadrao(String desc, String cat, TipoLancamento tipo, int valor, YearMonth mes, int parcelas) {
        criar(new LancamentoDto(null, desc, cat, tipo, BigDecimal.valueOf(valor), mes, parcelas));
    }

    /** Média mensal por categoria nos 12 meses a partir de inicio (padrão: mês atual). */
    public ResumoGastosDto resumoPorCategoria(YearMonth inicio) {
        return resumoPorCategoria(inicio, null);
    }

    /** Idem, só com os lançamentos do grupo (null = todos). */
    public ResumoGastosDto resumoPorCategoria(YearMonth inicio, GrupoLancamento grupo) {
        expirarPontuaisVencidos();
        YearMonth ini = inicio != null ? inicio : YearMonth.now(clock);
        Map<String, BigDecimal> totais = new LinkedHashMap<>();
        for (Lancamento l : lancamentos.findAllByDeletadoEmIsNullOrderByIdAsc()) {
            if (grupo != null && !grupo.contem(l.tipo)) continue;
            GastoCalculo g = new GastoCalculo(l.tipo, l.valor, YearMonth.from(l.mesInicial), l.parcelas);
            BigDecimal soma = BigDecimal.ZERO;
            for (int i = 0; i < 12; i++) soma = soma.add(g.valorEm(ini.plusMonths(i)));
            totais.merge(l.categoria, soma, BigDecimal::add);
        }
        List<CategoriaResumoDto> cats = totais.entrySet().stream()
                .map(e -> new CategoriaResumoDto(e.getKey(),
                        Dinheiro.arredondar(e.getValue().divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP))))
                .sorted(Comparator.comparing(CategoriaResumoDto::mediaMensal).reversed())
                .toList();
        BigDecimal total = cats.stream().map(CategoriaResumoDto::mediaMensal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumoGastosDto(ini, Dinheiro.arredondar(total), cats);
    }

    // ---- categorias ---------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CategoriaDto> listarCategorias() {
        return categorias.findAllByOrderByOrdemAscNomeAsc().stream()
                .map(c -> new CategoriaDto(c.nome, c.ordem, c.grupo)).toList();
    }

    public CategoriaDto criarCategoria(CategoriaDto d) {
        String nome = d.nome().trim();
        recusarCategoriaReservada(nome);
        if (categorias.existsById(nome)) throw new RegraDeNegocio("A categoria '" + nome + "' já existe.");
        Categoria c = new Categoria();
        c.nome = nome;
        c.ordem = d.ordem() > 0 ? d.ordem() : (int) categorias.count() + 1;
        c.grupo = d.grupo() != null ? d.grupo() : GrupoGasto.NECESSIDADE;
        categorias.save(c);
        return new CategoriaDto(c.nome, c.ordem, c.grupo);
    }

    /** Grupo 50/30/20 de cada categoria do seed (V10); as demais (criadas pelo usuário) são NECESSIDADE. */
    static GrupoGasto grupoPadrao(String nome) {
        return switch (nome) {
            case "Pessoal", "Lazer", "Outros" -> GrupoGasto.DESEJO;
            default -> GrupoGasto.NECESSIDADE;
        };
    }

    /** Volta o grupo de todas as categorias ao mapeamento padrão. */
    private void regruparCategoriasPadrao() {
        for (Categoria c : categorias.findAll()) {
            c.grupo = grupoPadrao(c.nome);
            categorias.save(c);
        }
    }

    public CategoriaDto atualizarGrupo(String nome, GrupoGasto grupo) {
        Categoria c = categorias.findById(nome)
                .orElseThrow(() -> new NaoEncontrado("Categoria '" + nome + "' não encontrada."));
        c.grupo = grupo;
        categorias.save(c);
        return new CategoriaDto(c.nome, c.ordem, c.grupo);
    }

    public void removerCategoria(String nome) {
        if (!categorias.existsById(nome)) throw new NaoEncontrado("Categoria '" + nome + "' não encontrada.");
        if (lancamentos.countByCategoriaAndDeletadoEmIsNull(nome) > 0) {
            throw new RegraDeNegocio("Há lançamentos usando a categoria '" + nome + "'.");
        }
        categorias.deleteById(nome);
    }

    // ---- internos -----------------------------------------------------------------------------

    /** A reserva tem aba própria (Metas); não existe mais como categoria de gasto. */
    private static void recusarCategoriaReservada(String nome) {
        if ("Reserva".equalsIgnoreCase(nome)) {
            throw new RegraDeNegocio("'Reserva' não é categoria de gasto: use a aba Metas.");
        }
    }

    private Lancamento entidade(Long id) {
        return lancamentos.findByIdAndDeletadoEmIsNull(id).orElseThrow(() -> new NaoEncontrado("Lançamento " + id + " não encontrado."));
    }

    private void aplicar(Lancamento l, LancamentoDto d) {
        String cat = d.categoria().trim();
        recusarCategoriaReservada(cat);
        if (!categorias.existsById(cat)) {
            Categoria c = new Categoria();
            c.nome = cat;
            c.ordem = (int) categorias.count() + 1;
            categorias.save(c);
        }
        l.descricao = d.descricao().trim();
        l.categoria = cat;
        l.tipo = d.tipo();
        l.valor = d.valor();
        l.mesInicial = d.mesInicial().atDay(1);
        l.parcelas = d.tipo() == TipoLancamento.PARCELADO ? Math.max(d.parcelas(), PARCELAS_MINIMAS) : 0;
    }

    private static LancamentoDto paraDto(Lancamento l) {
        return new LancamentoDto(l.id, l.descricao, l.categoria, l.tipo, l.valor, YearMonth.from(l.mesInicial), l.parcelas);
    }
}
