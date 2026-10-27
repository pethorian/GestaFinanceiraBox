package br.com.gestafinanceira.domain.calculo.cartao;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de fatura do cartão (cálculo puro).
 *
 * <p>Fechamento: compra com dia &gt;= dia de fechamento cai na fatura que fecha no mês seguinte; senão, na que fecha
 * no mês da compra. Dia de fechamento/vencimento maior que o último dia do mês vale o último dia. Vencimento:
 * sempre no mês seguinte ao do fechamento (fecha 13/10, vence 23/11). Parcelas:
 * {@code total / n} HALF_UP com 2 casas, e a diferença de arredondamento vai na primeira parcela.
 */
public final class CalculadoraFatura {
    private CalculadoraFatura() {}

    /** Mês em que fecha a fatura que recebe a compra. */
    public static YearMonth mesFechamento(LocalDate dataCompra, int diaFechamento) {
        YearMonth mesCompra = YearMonth.from(dataCompra);
        int fechamento = Math.min(diaFechamento, mesCompra.lengthOfMonth());
        return dataCompra.getDayOfMonth() >= fechamento ? mesCompra.plusMonths(1) : mesCompra;
    }

    /** Mês de vencimento da primeira fatura da compra: sempre o mês seguinte ao do fechamento. */
    public static YearMonth primeiraFatura(LocalDate dataCompra, int diaFechamento, int diaVencimento) {
        return mesFechamento(dataCompra, diaFechamento).plusMonths(1);
    }

    /** Fatura aberta: a que recebe as compras de hoje (ainda não fechou). */
    public static YearMonth faturaAberta(LocalDate hoje, int diaFechamento, int diaVencimento) {
        return primeiraFatura(hoje, diaFechamento, diaVencimento);
    }

    /**
     * Primeira fatura ainda por pagar (vence hoje ou depois), mesmo que já esteja fechada. Passado o dia de
     * vencimento do mês, a fatura daquele mês já foi paga.
     */
    public static YearMonth faturaAPagar(LocalDate hoje, int diaVencimento) {
        YearMonth mes = YearMonth.from(hoje);
        int vencimento = Math.min(diaVencimento, mes.lengthOfMonth());
        return hoje.getDayOfMonth() > vencimento ? mes.plusMonths(1) : mes;
    }

    /** Quantidade de faturas da compra; 0 = sem fim (assinatura). */
    public static int totalParcelas(CompraCartaoCalculo c) {
        return switch (c.tipo()) {
            case A_VISTA -> 1;
            case PARCELADA -> c.parcelas();
            case ASSINATURA -> 0;
        };
    }

    /** Valor de cada parcela (sem a diferença de arredondamento); em assinatura, o valor mensal. */
    public static BigDecimal valorParcela(CompraCartaoCalculo c) {
        int n = totalParcelas(c);
        if (n <= 1) return Dinheiro.arredondar(c.valor());
        return Dinheiro.arredondar(c.valor().divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP));
    }

    /** Valor da parcela {@code k} (1..n): a primeira absorve a diferença de arredondamento. */
    public static BigDecimal valorDaParcela(CompraCartaoCalculo c, int k) {
        int n = totalParcelas(c);
        BigDecimal base = valorParcela(c);
        if (n <= 1 || k != 1) return base;
        return base.add(diferenca(c));
    }

    /** Total da compra menos a soma de n parcelas iguais (pode ser negativa). */
    private static BigDecimal diferenca(CompraCartaoCalculo c) {
        int n = totalParcelas(c);
        return Dinheiro.arredondar(c.valor()).subtract(valorParcela(c).multiply(BigDecimal.valueOf(n)));
    }

    /** Última fatura da compra; nulo em assinatura. */
    public static YearMonth ultimaFatura(CompraCartaoCalculo c, YearMonth primeira) {
        int n = totalParcelas(c);
        return n == 0 ? null : primeira.plusMonths(n - 1L);
    }

    /** Parcelas com vencimento a partir de {@code mesAtual} (inclusive); nulo em assinatura. */
    public static Integer parcelasRestantes(CompraCartaoCalculo c, YearMonth primeira, YearMonth mesAtual) {
        int n = totalParcelas(c);
        if (n == 0) return null;
        long decorridas = Math.max(0, ChronoUnit.MONTHS.between(primeira, mesAtual));
        return (int) Math.max(0, n - decorridas);
    }

    /** Soma das parcelas ainda por vencer (vencimento &gt;= {@code mesAtual}); assinatura não conta. */
    public static BigDecimal comprometido(CompraCartaoCalculo c, YearMonth primeira, YearMonth mesAtual) {
        int n = totalParcelas(c);
        BigDecimal soma = Dinheiro.ZERO;
        for (int k = 1; k <= n; k++) {
            if (!primeira.plusMonths(k - 1L).isBefore(mesAtual)) soma = soma.add(valorDaParcela(c, k));
        }
        return soma;
    }

    /** Converte a compra em gastos do motor (projeção e resumo por categoria). */
    public static List<GastoCalculo> paraGastos(CompraCartaoCalculo c, YearMonth primeira) {
        List<GastoCalculo> r = new ArrayList<>();
        switch (c.tipo()) {
            case A_VISTA -> r.add(new GastoCalculo(TipoLancamento.PONTUAL, Dinheiro.arredondar(c.valor()), primeira, 0));
            case ASSINATURA -> r.add(new GastoCalculo(TipoLancamento.RECORRENTE, Dinheiro.arredondar(c.valor()), primeira, 0));
            case PARCELADA -> {
                int n = c.parcelas();
                BigDecimal base = valorParcela(c);
                if (diferenca(c).signum() == 0) {
                    r.add(new GastoCalculo(TipoLancamento.PARCELADO, base, primeira, n));
                } else {
                    r.add(new GastoCalculo(TipoLancamento.PONTUAL, valorDaParcela(c, 1), primeira, 0));
                    r.add(new GastoCalculo(TipoLancamento.PARCELADO, base, primeira.plusMonths(1), n - 1));
                }
            }
        }
        return r;
    }

    /** Fatura que vence em {@code mes}: itens e total. */
    public static Fatura fatura(List<CompraCartaoCalculo> compras, int diaFechamento, int diaVencimento, YearMonth mes) {
        List<ItemFatura> itens = new ArrayList<>();
        BigDecimal total = Dinheiro.ZERO;
        for (CompraCartaoCalculo c : compras) {
            YearMonth primeira = primeiraFatura(c.dataCompra(), diaFechamento, diaVencimento);
            long d = ChronoUnit.MONTHS.between(primeira, mes);
            int n = totalParcelas(c);
            if (d < 0 || (n > 0 && d >= n)) continue;
            int k = (int) d + 1;
            BigDecimal valor = c.tipo() == TipoCompra.ASSINATURA ? Dinheiro.arredondar(c.valor()) : valorDaParcela(c, k);
            String rotulo = c.tipo() == TipoCompra.PARCELADA ? k + "/" + n : null;
            itens.add(new ItemFatura(c.id(), c.descricao(), c.categoria(), rotulo, valor));
            total = total.add(valor);
        }
        return new Fatura(mes, Dinheiro.arredondar(total), itens);
    }
}
