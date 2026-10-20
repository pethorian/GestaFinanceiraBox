package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.CategoriaDto;
import br.com.gestafinanceira.api.dto.Dtos.CategoriaResumoDto;
import br.com.gestafinanceira.api.dto.Dtos.ResumoGastosDto;
import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.api.dto.Dtos.LancamentoDto;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import br.com.gestafinanceira.infra.persistencia.Entidades.Categoria;
import br.com.gestafinanceira.infra.persistencia.Entidades.Lancamento;
import br.com.gestafinanceira.infra.persistencia.CategoriaRepo;
import br.com.gestafinanceira.infra.persistencia.LancamentoRepo;
import java.math.BigDecimal;
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

    public LancamentoService(LancamentoRepo lancamentos, CategoriaRepo categorias) {
        this.lancamentos = lancamentos;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<LancamentoDto> listar() {
        return lancamentos.findAllByOrderByIdAsc().stream().map(LancamentoService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public LancamentoDto buscar(Long id) {
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

    public void remover(Long id) {
        lancamentos.delete(entidade(id));
    }

    /** Nova linha de gasto com os valores padrão definidos pelo servidor (mês atual, primeira categoria). */
    public LancamentoDto criarNovo() {
        String categoria = categorias.findAllByOrderByOrdemAscNomeAsc().stream().findFirst()
                .map(c -> c.nome).orElse("Outros");
        return criar(new LancamentoDto(null, "Novo gasto", categoria, TipoLancamento.RECORRENTE,
                BigDecimal.ZERO, YearMonth.now(), 0));
    }

    @Transactional(readOnly = true)
    public List<GastoCalculo> paraCalculo() {
        return lancamentos.findAllByOrderByIdAsc().stream()
                .map(l -> new GastoCalculo(l.tipo, l.valor, YearMonth.from(l.mesInicial), l.parcelas))
                .toList();
    }

    public void removerTodos() {
        lancamentos.deleteAllInBatch();
    }

    /** Lançamentos padrão, com início relativo ao mês atual (equivale a dadosIniciais() do frontend). */
    public void restaurarPadrao() {
        removerTodos();
        YearMonth m = YearMonth.now();
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
    @Transactional(readOnly = true)
    public ResumoGastosDto resumoPorCategoria(YearMonth inicio) {
        YearMonth ini = inicio != null ? inicio : YearMonth.now();
        Map<String, BigDecimal> totais = new LinkedHashMap<>();
        for (Lancamento l : lancamentos.findAllByOrderByIdAsc()) {
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
                .map(c -> new CategoriaDto(c.nome, c.ordem)).toList();
    }

    public CategoriaDto criarCategoria(CategoriaDto d) {
        String nome = d.nome().trim();
        if (categorias.existsById(nome)) throw new RegraDeNegocio("A categoria '" + nome + "' já existe.");
        Categoria c = new Categoria();
        c.nome = nome;
        c.ordem = d.ordem() > 0 ? d.ordem() : (int) categorias.count() + 1;
        categorias.save(c);
        return new CategoriaDto(c.nome, c.ordem);
    }

    public void removerCategoria(String nome) {
        if (!categorias.existsById(nome)) throw new NaoEncontrado("Categoria '" + nome + "' não encontrada.");
        if (lancamentos.countByCategoria(nome) > 0) {
            throw new RegraDeNegocio("Há lançamentos usando a categoria '" + nome + "'.");
        }
        categorias.deleteById(nome);
    }

    // ---- internos -----------------------------------------------------------------------------

    private Lancamento entidade(Long id) {
        return lancamentos.findById(id).orElseThrow(() -> new NaoEncontrado("Lançamento " + id + " não encontrado."));
    }

    private void aplicar(Lancamento l, LancamentoDto d) {
        String cat = d.categoria().trim();
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
