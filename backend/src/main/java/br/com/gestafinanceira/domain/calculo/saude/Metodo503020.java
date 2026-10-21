package br.com.gestafinanceira.domain.calculo.saude;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Método 50/30/20: até 50% da renda em necessidades, até 30% em desejos e pelo menos 20% em poupança.
 * Cálculo puro: BigDecimal, HALF_UP, 2 casas (percentuais com 1 casa).
 *
 * <p>Status: necessidades e desejos ficam {@code ACIMA} se passam do recomendado, senão {@code OK};
 * a poupança fica {@code ABAIXO} se é menor que o recomendado, senão {@code OK}.
 */
public final class Metodo503020 {
    public static final BigDecimal PCT_NECESSIDADES = BigDecimal.valueOf(50);
    public static final BigDecimal PCT_DESEJOS = BigDecimal.valueOf(30);
    public static final BigDecimal PCT_POUPANCA = BigDecimal.valueOf(20);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Metodo503020() {}

    /**
     * @param recomendado valores recomendados (50/30/20 da renda)
     * @param atual valores atuais (necessidades, desejos, poupança)
     * @param percentuais % da renda com 1 casa; {@code null} se a renda é ≤ 0
     * @param diferenca atual − recomendado por faixa
     */
    public record Resultado(
            BigDecimal rendaMensal, Faixas<BigDecimal> recomendado, Faixas<BigDecimal> atual,
            Faixas<BigDecimal> percentuais, Faixas<BigDecimal> diferenca, Faixas<StatusFaixa> status,
            BigDecimal sobraLivre, BigDecimal aporteSugerido, List<String> diagnostico) {
    }

    public static Resultado calcular(BigDecimal renda, BigDecimal necessidades, BigDecimal desejos,
                                     BigDecimal poupanca) {
        BigDecimal r = Dinheiro.arredondar(renda);
        boolean temRenda = r.signum() > 0;
        Faixas<BigDecimal> atual = new Faixas<>(Dinheiro.arredondar(necessidades), Dinheiro.arredondar(desejos),
                Dinheiro.arredondar(poupanca));
        Faixas<BigDecimal> rec = temRenda
                ? new Faixas<>(Dinheiro.percentual(r, PCT_NECESSIDADES), Dinheiro.percentual(r, PCT_DESEJOS),
                        Dinheiro.percentual(r, PCT_POUPANCA))
                : new Faixas<>(Dinheiro.ZERO, Dinheiro.ZERO, Dinheiro.ZERO);
        Faixas<BigDecimal> dif = new Faixas<>(atual.necessidades().subtract(rec.necessidades()),
                atual.desejos().subtract(rec.desejos()), atual.poupanca().subtract(rec.poupanca()));
        Faixas<BigDecimal> pct = temRenda
                ? new Faixas<>(pct(atual.necessidades(), r), pct(atual.desejos(), r), pct(atual.poupanca(), r))
                : null;
        Faixas<StatusFaixa> status = new Faixas<>(
                dif.necessidades().signum() > 0 ? StatusFaixa.ACIMA : StatusFaixa.OK,
                dif.desejos().signum() > 0 ? StatusFaixa.ACIMA : StatusFaixa.OK,
                dif.poupanca().signum() < 0 ? StatusFaixa.ABAIXO : StatusFaixa.OK);
        BigDecimal sobra = r.subtract(atual.necessidades()).subtract(atual.desejos()).subtract(atual.poupanca());
        BigDecimal aporte = temRenda ? Dinheiro.max(Dinheiro.ZERO, rec.poupanca().subtract(atual.poupanca()))
                : Dinheiro.ZERO;
        return new Resultado(r, rec, atual, pct, dif, status, sobra, aporte,
                diagnostico(temRenda, atual, pct, status, sobra, aporte));
    }

    private static BigDecimal pct(BigDecimal valor, BigDecimal renda) {
        return valor.multiply(CEM).divide(renda, 1, RoundingMode.HALF_UP);
    }

    private static List<String> diagnostico(boolean temRenda, Faixas<BigDecimal> atual, Faixas<BigDecimal> pct,
                                            Faixas<StatusFaixa> status, BigDecimal sobra, BigDecimal aporte) {
        List<String> d = new ArrayList<>();
        if (!temRenda) {
            d.add("Não há renda mensal de referência para calcular o método 50/30/20; confira seu perfil.");
            return d;
        }
        d.add(status.necessidades() == StatusFaixa.ACIMA
                ? "Seus gastos obrigatórios estão em " + p(pct.necessidades()) + " da renda (meta: até 50%)."
                : "Seus gastos obrigatórios estão em " + p(pct.necessidades()) + " da renda, dentro da meta de até 50%.");
        d.add(status.desejos() == StatusFaixa.ACIMA
                ? "Seus gastos com desejos estão em " + p(pct.desejos()) + " da renda (meta: até 30%)."
                : "Seus gastos com desejos estão em " + p(pct.desejos()) + " da renda, dentro da meta de até 30%.");
        d.add(status.poupanca() == StatusFaixa.ABAIXO
                ? "Você guarda " + p(pct.poupanca()) + " da renda; para chegar a 20%, aporte mais R$ " + m(aporte)
                        + " por mês em uma reserva (aba Metas e reserva)."
                : "Você guarda " + p(pct.poupanca()) + " da renda, dentro da meta de pelo menos 20%.");
        if (sobra.signum() < 0) {
            d.add("Seus gastos e aportes passam da renda em R$ " + m(sobra.negate()) + " por mês.");
        } else if (sobra.signum() > 0) {
            d.add("Sobram R$ " + m(sobra) + " por mês sem destino; considere direcioná-los à poupança.");
        }
        return d;
    }

    private static String p(BigDecimal v) {
        DecimalFormat f = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(PT_BR));
        return f.format(v) + "%";
    }

    private static String m(BigDecimal v) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        return f.format(v);
    }
}
