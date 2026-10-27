package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.CartaoDto;
import br.com.gestafinanceira.api.dto.Dtos.CartaoEntradaDto;
import br.com.gestafinanceira.api.dto.Dtos.CompraDto;
import br.com.gestafinanceira.api.dto.Dtos.CompraEntradaDto;
import br.com.gestafinanceira.application.Erros.DadosInvalidos;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.cartao.CalculadoraFatura;
import br.com.gestafinanceira.domain.calculo.cartao.CompraCartaoCalculo;
import br.com.gestafinanceira.domain.calculo.cartao.Fatura;
import br.com.gestafinanceira.domain.calculo.cartao.TipoCompra;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.infra.persistencia.CartaoRepo;
import br.com.gestafinanceira.infra.persistencia.CompraCartaoRepo;
import br.com.gestafinanceira.infra.persistencia.EntidadesCartao.Cartao;
import br.com.gestafinanceira.infra.persistencia.EntidadesCartao.CompraCartao;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cartões de crédito e suas compras. A fatura de cada mês vira gasto fixo: {@link #paraCalculo()} alimenta a
 * projeção e {@link #gastosPorCategoria()} o resumo por categoria. Não depende de {@link LancamentoService}.
 */
@Service
@Transactional
public class CartaoService {
    static final int PARCELAS_MAXIMAS = 72;
    static final int PARCELAS_MINIMAS = 2;
    static final int MESES_FATURAS = 12;

    /** Gasto do motor com a categoria da compra de origem. */
    public record GastoCategoria(String categoria, GastoCalculo gasto) {
    }

    private final CartaoRepo cartoes;
    private final CompraCartaoRepo compras;
    private final CategoriaGasto categorias;
    private final Clock clock;

    public CartaoService(CartaoRepo cartoes, CompraCartaoRepo compras, CategoriaGasto categorias, Clock clock) {
        this.cartoes = cartoes;
        this.compras = compras;
        this.categorias = categorias;
        this.clock = clock;
    }

    // ---- cartões ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CartaoDto> listar() {
        Map<Long, List<CompraCartao>> porCartao = compras.findAtivasDeCartoesAtivos().stream()
                .collect(Collectors.groupingBy(c -> c.cartaoId));
        return cartoes.findAllByDeletadoEmIsNullOrderByIdAsc().stream()
                .map(c -> paraDto(c, porCartao.getOrDefault(c.id, List.of()))).toList();
    }

    public CartaoDto criar(CartaoEntradaDto d) {
        Cartao c = new Cartao();
        aplicar(c, d);
        return paraDto(cartoes.save(c), List.of());
    }

    public CartaoDto atualizar(Long id, CartaoEntradaDto d) {
        Cartao c = cartao(id);
        aplicar(c, d);
        cartoes.save(c);
        return paraDto(c, compras.findAllByCartaoIdAndDeletadoEmIsNullOrderByDataCompraAscIdAsc(id));
    }

    /** Exclusão lógica do cartão e de todas as suas compras. */
    public void remover(Long id) {
        Cartao c = cartao(id);
        LocalDateTime agora = LocalDateTime.now(clock);
        compras.excluirDoCartao(id, agora);
        c = cartao(id);
        c.deletadoEm = agora;
        cartoes.save(c);
    }

    /** "Restaurar padrão": exclusão lógica de todos os cartões e compras. */
    public void removerTodos() {
        LocalDateTime agora = LocalDateTime.now(clock);
        compras.excluirTodasAtivas(agora);
        cartoes.excluirTodosAtivos(agora);
    }

    // ---- compras ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CompraDto> listarCompras(Long cartaoId) {
        Cartao c = cartao(cartaoId);
        return compras.findAllByCartaoIdAndDeletadoEmIsNullOrderByDataCompraAscIdAsc(cartaoId).stream()
                .map(x -> paraDto(c, x)).toList();
    }

    public CompraDto criarCompra(Long cartaoId, CompraEntradaDto d) {
        Cartao c = cartao(cartaoId);
        CompraCartao x = new CompraCartao();
        x.cartaoId = cartaoId;
        aplicar(x, d);
        return paraDto(c, compras.save(x));
    }

    public CompraDto atualizarCompra(Long compraId, CompraEntradaDto d) {
        CompraCartao x = compra(compraId);
        Cartao c = cartao(x.cartaoId);
        aplicar(x, d);
        return paraDto(c, compras.save(x));
    }

    public void removerCompra(Long compraId) {
        CompraCartao x = compra(compraId);
        x.deletadoEm = LocalDateTime.now(clock);
        compras.save(x);
    }

    // ---- faturas ------------------------------------------------------------------------------

    /** Faturas que vencem nos 12 meses a partir de {@code inicio} (padrão: a primeira ainda por pagar). */
    @Transactional(readOnly = true)
    public List<Fatura> faturas(Long cartaoId, YearMonth inicio) {
        Cartao c = cartao(cartaoId);
        YearMonth ini = inicio != null ? inicio : faturaAPagar(c);
        List<CompraCartaoCalculo> calc = compras.findAllByCartaoIdAndDeletadoEmIsNullOrderByDataCompraAscIdAsc(cartaoId)
                .stream().map(CartaoService::paraCalculo).toList();
        List<Fatura> r = new ArrayList<>();
        for (int i = 0; i < MESES_FATURAS; i++) {
            r.add(CalculadoraFatura.fatura(calc, c.diaFechamento, c.diaVencimento, ini.plusMonths(i)));
        }
        return r;
    }

    // ---- integração com a projeção ------------------------------------------------------------

    /** Gastos do motor de todas as compras ativas de cartões ativos, com a categoria de cada uma. */
    @Transactional(readOnly = true)
    public List<GastoCategoria> gastosPorCategoria() {
        Map<Long, Cartao> ativos = cartoes.findAllByDeletadoEmIsNullOrderByIdAsc().stream()
                .collect(Collectors.toMap(c -> c.id, Function.identity()));
        List<GastoCategoria> r = new ArrayList<>();
        for (CompraCartao x : compras.findAtivasDeCartoesAtivos()) {
            Cartao c = ativos.get(x.cartaoId);
            if (c == null) continue;
            CompraCartaoCalculo calc = paraCalculo(x);
            YearMonth primeira = CalculadoraFatura.primeiraFatura(x.dataCompra, c.diaFechamento, c.diaVencimento);
            for (GastoCalculo g : CalculadoraFatura.paraGastos(calc, primeira)) r.add(new GastoCategoria(x.categoria, g));
        }
        return r;
    }

    /** Idem, sem a categoria (projeção). */
    @Transactional(readOnly = true)
    public List<GastoCalculo> paraCalculo() {
        return gastosPorCategoria().stream().map(GastoCategoria::gasto).toList();
    }

    @Transactional(readOnly = true)
    public boolean categoriaEmUso(String categoria) {
        return compras.contarAtivasPorCategoria(categoria) > 0;
    }

    // ---- internos -----------------------------------------------------------------------------

    private Cartao cartao(Long id) {
        return cartoes.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new NaoEncontrado("Cartão " + id + " não encontrado."));
    }

    private CompraCartao compra(Long id) {
        CompraCartao x = compras.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new NaoEncontrado("Compra " + id + " não encontrada."));
        if (cartoes.findByIdAndDeletadoEmIsNull(x.cartaoId).isEmpty()) {
            throw new NaoEncontrado("Compra " + id + " não encontrada.");
        }
        return x;
    }

    private static void aplicar(Cartao c, CartaoEntradaDto d) {
        c.nome = d.nome();
        c.diaFechamento = d.diaFechamento().shortValue();
        c.diaVencimento = d.diaVencimento().shortValue();
        c.limite = d.limite() != null ? Dinheiro.arredondar(d.limite()) : null;
    }

    private void aplicar(CompraCartao x, CompraEntradaDto d) {
        int parcelas = 0;
        if (d.tipo() == TipoCompra.PARCELADA) {
            Integer p = d.parcelas();
            if (p == null || p < PARCELAS_MINIMAS || p > PARCELAS_MAXIMAS) {
                throw new DadosInvalidos(List.of("parcelas: informe de " + PARCELAS_MINIMAS + " a "
                        + PARCELAS_MAXIMAS + " parcelas para compra parcelada"));
            }
            parcelas = p;
        }
        x.categoria = categorias.garantir(d.categoria());
        x.descricao = d.descricao();
        x.tipo = d.tipo();
        x.dataCompra = d.dataCompra();
        x.valor = Dinheiro.arredondar(d.valor());
        x.parcelas = parcelas;
    }

    private static CompraCartaoCalculo paraCalculo(CompraCartao x) {
        return new CompraCartaoCalculo(x.id, x.descricao, x.categoria, x.tipo, x.dataCompra, x.valor, x.parcelas);
    }

    private CartaoDto paraDto(Cartao c, List<CompraCartao> doCartao) {
        LocalDate hoje = LocalDate.now(clock);
        YearMonth aberta = CalculadoraFatura.faturaAberta(hoje, c.diaFechamento, c.diaVencimento);
        YearMonth aPagar = faturaAPagar(c);
        List<CompraCartaoCalculo> calc = doCartao.stream().map(CartaoService::paraCalculo).toList();
        BigDecimal fatura = CalculadoraFatura.fatura(calc, c.diaFechamento, c.diaVencimento, aberta).total();
        BigDecimal proxima = CalculadoraFatura.fatura(calc, c.diaFechamento, c.diaVencimento, aberta.plusMonths(1)).total();
        BigDecimal comprometido = Dinheiro.ZERO;
        for (CompraCartaoCalculo x : calc) {
            YearMonth primeira = CalculadoraFatura.primeiraFatura(x.dataCompra(), c.diaFechamento, c.diaVencimento);
            comprometido = comprometido.add(CalculadoraFatura.comprometido(x, primeira, aPagar));
        }
        comprometido = Dinheiro.arredondar(comprometido);
        BigDecimal disponivel = c.limite != null ? Dinheiro.arredondar(c.limite.subtract(comprometido)) : null;
        return new CartaoDto(c.id, c.nome, c.diaFechamento, c.diaVencimento, c.limite, fatura, proxima, comprometido,
                disponivel);
    }

    private CompraDto paraDto(Cartao c, CompraCartao x) {
        CompraCartaoCalculo calc = paraCalculo(x);
        YearMonth primeira = CalculadoraFatura.primeiraFatura(x.dataCompra, c.diaFechamento, c.diaVencimento);
        return new CompraDto(x.id, x.cartaoId, x.descricao, x.categoria, x.tipo, x.dataCompra, x.valor, x.parcelas,
                CalculadoraFatura.valorParcela(calc), primeira, CalculadoraFatura.ultimaFatura(calc, primeira),
                CalculadoraFatura.parcelasRestantes(calc, primeira, faturaAPagar(c)));
    }

    /** Mês de vencimento da primeira fatura ainda por pagar (vence hoje ou depois). */
    private YearMonth faturaAPagar(Cartao c) {
        return CalculadoraFatura.faturaAPagar(LocalDate.now(clock), c.diaVencimento);
    }
}
