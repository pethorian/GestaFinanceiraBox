package br.com.gestafinanceira.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.com.gestafinanceira.domain.calculo.cartao.CalculadoraFatura;
import br.com.gestafinanceira.domain.calculo.cartao.CompraCartaoCalculo;
import br.com.gestafinanceira.domain.calculo.cartao.Fatura;
import br.com.gestafinanceira.domain.calculo.cartao.TipoCompra;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraFaturaTest {
    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static CompraCartaoCalculo compra(TipoCompra tipo, String valor, int parcelas, String data) {
        return new CompraCartaoCalculo(1L, "Compra", "Outros", tipo, LocalDate.parse(data), d(valor), parcelas);
    }

    @Test
    void compraAntesDoFechamentoCaiNaFaturaDoMes() {
        // fecha dia 10, vence dia 20 (mesmo mês do fechamento)
        assertEquals(YearMonth.of(2026, 3), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 3, 9), 10, 20));
    }

    @Test
    void compraNoDiaDoFechamentoJaCaiNaProximaFatura() {
        assertEquals(YearMonth.of(2026, 4), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 3, 10), 10, 20));
    }

    @Test
    void compraDepoisDoFechamentoCaiNaProximaFatura() {
        assertEquals(YearMonth.of(2026, 4), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 3, 25), 10, 20));
    }

    @Test
    void vencimentoAntesDoFechamentoVaiParaOMesSeguinteAoFechamento() {
        // fecha dia 25, vence dia 5: compra dia 3/3 fecha em 25/3 e vence em 5/4
        assertEquals(YearMonth.of(2026, 4), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 3, 3), 25, 5));
        // compra dia 26/3 fecha em 25/4 e vence em 5/5
        assertEquals(YearMonth.of(2026, 5), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 3, 26), 25, 5));
    }

    @Test
    void viradaDeAno() {
        // fecha dia 10 vence dia 20: compra em 15/12 fecha em jan e vence em jan
        assertEquals(YearMonth.of(2027, 1), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 12, 15), 10, 20));
        // fecha dia 25 vence dia 5: compra em 26/12 fecha em jan e vence em fev
        assertEquals(YearMonth.of(2027, 2), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 12, 26), 25, 5));
        // fecha dia 25 vence dia 5: compra em 3/12 fecha em dez e vence em jan
        assertEquals(YearMonth.of(2027, 1), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 12, 3), 25, 5));
    }

    @Test
    void fechamentoDia31EmFevereiroValeUltimoDiaDoMes() {
        // fev/2026 tem 28 dias: fechamento efetivo = 28
        assertEquals(YearMonth.of(2026, 2), CalculadoraFatura.mesFechamento(LocalDate.of(2026, 2, 27), 31));
        assertEquals(YearMonth.of(2026, 3), CalculadoraFatura.mesFechamento(LocalDate.of(2026, 2, 28), 31));
        // em março (31 dias) o dia 30 ainda fecha em março
        assertEquals(YearMonth.of(2026, 3), CalculadoraFatura.mesFechamento(LocalDate.of(2026, 3, 30), 31));
        assertEquals(YearMonth.of(2026, 4), CalculadoraFatura.mesFechamento(LocalDate.of(2026, 3, 31), 31));
        // fechamento 31 e vencimento 10: vence no mês seguinte ao fechamento
        assertEquals(YearMonth.of(2026, 3), CalculadoraFatura.primeiraFatura(LocalDate.of(2026, 2, 10), 31, 10));
    }

    @Test
    void parcelasComRestoColocamADiferencaNaPrimeira() {
        CompraCartaoCalculo c = compra(TipoCompra.PARCELADA, "100.00", 3, "2026-03-01");
        assertEquals(d("33.34"), CalculadoraFatura.valorDaParcela(c, 1));
        assertEquals(d("33.33"), CalculadoraFatura.valorDaParcela(c, 2));
        assertEquals(d("33.33"), CalculadoraFatura.valorDaParcela(c, 3));
        assertEquals(d("33.33"), CalculadoraFatura.valorParcela(c));
    }

    @Test
    void parcelasComArredondamentoParaCimaTiramDaPrimeira() {
        // 10,00 em 6x: 1,67 cada arredondado (soma 10,02) -> primeira 1,65
        CompraCartaoCalculo c = compra(TipoCompra.PARCELADA, "10.00", 6, "2026-03-01");
        assertEquals(d("1.65"), CalculadoraFatura.valorDaParcela(c, 1));
        assertEquals(d("1.67"), CalculadoraFatura.valorDaParcela(c, 2));
        BigDecimal soma = BigDecimal.ZERO;
        for (int k = 1; k <= 6; k++) soma = soma.add(CalculadoraFatura.valorDaParcela(c, k));
        assertEquals(d("10.00"), soma);
    }

    @Test
    void parceladaComRestoViraPontualMaisParcelado() {
        CompraCartaoCalculo c = compra(TipoCompra.PARCELADA, "100.00", 3, "2026-03-01");
        YearMonth primeira = YearMonth.of(2026, 3);
        List<GastoCalculo> g = CalculadoraFatura.paraGastos(c, primeira);
        assertEquals(2, g.size());
        assertEquals(new GastoCalculo(TipoLancamento.PONTUAL, d("33.34"), primeira, 0), g.get(0));
        assertEquals(new GastoCalculo(TipoLancamento.PARCELADO, d("33.33"), primeira.plusMonths(1), 2), g.get(1));
        BigDecimal soma = BigDecimal.ZERO;
        for (int i = 0; i < 6; i++) {
            for (GastoCalculo x : g) soma = soma.add(x.valorEm(primeira.plusMonths(i)));
        }
        assertEquals(0, d("100.00").compareTo(soma));
    }

    @Test
    void parceladaExataViraUmParcelado() {
        CompraCartaoCalculo c = compra(TipoCompra.PARCELADA, "120.00", 4, "2026-03-01");
        List<GastoCalculo> g = CalculadoraFatura.paraGastos(c, YearMonth.of(2026, 3));
        assertEquals(List.of(new GastoCalculo(TipoLancamento.PARCELADO, d("30.00"), YearMonth.of(2026, 3), 4)), g);
    }

    @Test
    void aVistaViraPontualEAssinaturaViraRecorrente() {
        YearMonth m = YearMonth.of(2026, 5);
        assertEquals(List.of(new GastoCalculo(TipoLancamento.PONTUAL, d("59.90"), m, 0)),
                CalculadoraFatura.paraGastos(compra(TipoCompra.A_VISTA, "59.90", 0, "2026-04-01"), m));
        assertEquals(List.of(new GastoCalculo(TipoLancamento.RECORRENTE, d("39.90"), m, 0)),
                CalculadoraFatura.paraGastos(compra(TipoCompra.ASSINATURA, "39.90", 0, "2026-04-01"), m));
    }

    @Test
    void assinaturaNaoTemUltimaFaturaNemParcelasRestantes() {
        CompraCartaoCalculo c = compra(TipoCompra.ASSINATURA, "39.90", 0, "2026-04-01");
        assertNull(CalculadoraFatura.ultimaFatura(c, YearMonth.of(2026, 5)));
        assertNull(CalculadoraFatura.parcelasRestantes(c, YearMonth.of(2026, 5), YearMonth.of(2026, 6)));
        assertEquals(0, CalculadoraFatura.comprometido(c, YearMonth.of(2026, 5), YearMonth.of(2026, 6)).signum());
    }

    @Test
    void ultimaFaturaEParcelasRestantes() {
        CompraCartaoCalculo c = compra(TipoCompra.PARCELADA, "600.00", 6, "2026-03-01");
        YearMonth primeira = YearMonth.of(2026, 3);
        assertEquals(YearMonth.of(2026, 8), CalculadoraFatura.ultimaFatura(c, primeira));
        assertEquals(6, CalculadoraFatura.parcelasRestantes(c, primeira, YearMonth.of(2026, 1)));
        assertEquals(4, CalculadoraFatura.parcelasRestantes(c, primeira, YearMonth.of(2026, 5)));
        assertEquals(0, CalculadoraFatura.parcelasRestantes(c, primeira, YearMonth.of(2027, 1)));
        // comprometido a partir de maio: 4 parcelas de 100
        assertEquals(d("400.00"), CalculadoraFatura.comprometido(c, primeira, YearMonth.of(2026, 5)));
    }

    @Test
    void faturaDeUmMesComItensRotulosETotal() {
        // fecha dia 10, vence dia 20
        List<CompraCartaoCalculo> compras = List.of(
                new CompraCartaoCalculo(1L, "Notebook", "Pessoal", TipoCompra.PARCELADA, LocalDate.of(2026, 1, 5), d("1000.00"), 10),
                new CompraCartaoCalculo(2L, "Streaming", "Lazer", TipoCompra.ASSINATURA, LocalDate.of(2026, 2, 1), d("39.90"), 0),
                new CompraCartaoCalculo(3L, "Jantar", "Alimentação", TipoCompra.A_VISTA, LocalDate.of(2026, 3, 9), d("80.10"), 0),
                new CompraCartaoCalculo(4L, "Fora do mês", "Outros", TipoCompra.A_VISTA, LocalDate.of(2026, 3, 20), d("50.00"), 0));
        Fatura f = CalculadoraFatura.fatura(compras, 10, 20, YearMonth.of(2026, 3));
        assertEquals(YearMonth.of(2026, 3), f.mes());
        // notebook: 1ª fatura jan -> março é a 3/10; streaming: 1ª fatura fev -> recorrente; jantar: março
        assertEquals(3, f.itens().size());
        assertEquals("3/10", f.itens().get(0).parcela());
        assertEquals(d("100.00"), f.itens().get(0).valor());
        assertNull(f.itens().get(1).parcela());
        assertEquals(d("39.90"), f.itens().get(1).valor());
        assertEquals(d("80.10"), f.itens().get(2).valor());
        assertEquals(d("220.00"), f.total());
        // antes da primeira fatura e depois da última não há itens
        assertEquals(0, CalculadoraFatura.fatura(compras, 10, 20, YearMonth.of(2025, 12)).itens().size());
        assertEquals(d("0.00"), CalculadoraFatura.fatura(compras, 10, 20, YearMonth.of(2025, 12)).total());
    }
}
