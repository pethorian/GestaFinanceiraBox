package br.com.gestafinanceira.domain.calculo.saude;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.adicionais.AdicionalMes;
import br.com.gestafinanceira.domain.calculo.adicionais.TotaisAdicionais;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Bloco "adicionais" da saúde financeira: como o 13º e o PLR do período (12 meses da projeção) são usados,
 * comparado ao 50/30/20. É <b>à parte</b> do método mensal (renda de mês comum): OBRIGATORIO → necessidades,
 * LAZER → desejos, RESERVA → poupança; o que não tem destino fica em {@code semDestino}. Percentuais (1 casa)
 * sobre o total líquido dos adicionais; status por faixa como em {@link Metodo503020}. Cálculo puro, HALF_UP.
 */
public final class SaudeAdicionais {
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private SaudeAdicionais() {}

    /** Valores (ou percentuais) por faixa, com o que ficou sem destino. */
    public record Divisao(BigDecimal necessidades, BigDecimal desejos, BigDecimal poupanca, BigDecimal semDestino) {
    }

    /** Comparação com o 50/30/20 aplicado ao total dos adicionais. */
    public record Comparacao(Faixas<BigDecimal> recomendado, Faixas<BigDecimal> diferenca,
                             Faixas<StatusFaixa> status) {
    }

    /**
     * {@code percentuais} e {@code comparacao} são {@code null} quando não há adicional no período (total ≤ 0).
     * {@code definido}: há ao menos um destino definido em alguma parcela do período.
     */
    public record Resultado(BigDecimal totalLiquidoPeriodo, Divisao divisao, Divisao percentuais,
                            Comparacao comparacao, boolean definido, List<String> diagnostico) {
    }

    /** @param parcelas todas as parcelas de adicionais do período (já divididas pelo plano de uso) */
    public static Resultado calcular(List<AdicionalMes> parcelas) {
        TotaisAdicionais t = TotaisAdicionais.somar(parcelas);
        BigDecimal total = t.liquido();
        Divisao divisao = new Divisao(t.obrigatorio(), t.lazer(), t.reserva(), t.semDestino());
        boolean definido = parcelas.stream().anyMatch(a -> !a.destinos().isEmpty());
        Set<EventoAdicional> presentes = new LinkedHashSet<>();
        Set<EventoAdicional> semPlano = new LinkedHashSet<>();
        parcelas.forEach(a -> presentes.add(a.evento()));
        for (EventoAdicional e : presentes) {
            if (parcelas.stream().filter(a -> a.evento() == e).allMatch(a -> a.destinos().isEmpty())) semPlano.add(e);
        }
        if (total.signum() <= 0) {
            List<String> d = new ArrayList<>();
            d.add(presentes.isEmpty()
                    ? "Não há 13º nem PLR na projeção dos próximos 12 meses."
                    : "Seus adicionais (" + nomes(presentes) + ") não têm valor líquido na projeção dos próximos 12 meses.");
            return new Resultado(total, divisao, null, null, definido, d);
        }
        Divisao pct = new Divisao(pct(divisao.necessidades(), total), pct(divisao.desejos(), total),
                pct(divisao.poupanca(), total), pct(divisao.semDestino(), total));
        Faixas<BigDecimal> rec = new Faixas<>(Dinheiro.percentual(total, Metodo503020.PCT_NECESSIDADES),
                Dinheiro.percentual(total, Metodo503020.PCT_DESEJOS), Dinheiro.percentual(total, Metodo503020.PCT_POUPANCA));
        Faixas<BigDecimal> dif = new Faixas<>(divisao.necessidades().subtract(rec.necessidades()),
                divisao.desejos().subtract(rec.desejos()), divisao.poupanca().subtract(rec.poupanca()));
        Faixas<StatusFaixa> status = new Faixas<>(
                dif.necessidades().signum() > 0 ? StatusFaixa.ACIMA : StatusFaixa.OK,
                dif.desejos().signum() > 0 ? StatusFaixa.ACIMA : StatusFaixa.OK,
                dif.poupanca().signum() < 0 ? StatusFaixa.ABAIXO : StatusFaixa.OK);
        Comparacao comp = new Comparacao(rec, dif, status);
        return new Resultado(total, divisao, pct, comp, definido,
                diagnostico(presentes, semPlano, definido, divisao, pct, comp));
    }

    private static List<String> diagnostico(Set<EventoAdicional> presentes, Set<EventoAdicional> semPlano,
                                            boolean definido, Divisao valores, Divisao pct, Comparacao comp) {
        List<String> d = new ArrayList<>();
        if (!definido) {
            d.add("Você ainda não definiu como usar seu " + nomes(semPlano) + ".");
            return d;
        }
        List<String> partes = new ArrayList<>();
        if (valores.desejos().signum() > 0) partes.add(p(pct.desejos()) + " para lazer");
        if (valores.necessidades().signum() > 0) partes.add(p(pct.necessidades()) + " para gastos obrigatórios");
        if (valores.poupanca().signum() > 0) partes.add(p(pct.poupanca()) + " para reservas");
        StringBuilder s = new StringBuilder("Dos seus adicionais nos próximos 12 meses (" + nomes(presentes) + "), ");
        if (partes.isEmpty()) {
            s.append("nenhum valor tem destino definido");
        } else {
            s.append(partes.get(0).replaceFirst(" para ", " vão para "));
            for (int i = 1; i < partes.size(); i++) s.append(i == partes.size() - 1 ? " e " : ", ").append(partes.get(i));
        }
        if (valores.semDestino().signum() > 0 && !partes.isEmpty()) {
            s.append("; ").append(p(pct.semDestino())).append(" ficam sem destino definido, no saldo disponível");
        }
        s.append('.');
        d.add(s.toString());
        if (comp.status().poupanca() == StatusFaixa.ABAIXO) {
            d.add("Pelo método 50/30/20, o ideal seria guardar pelo menos 20% deles (R$ "
                    + m(comp.recomendado().poupanca()) + "); hoje vão R$ " + m(valores.poupanca()) + " para reservas.");
        } else {
            d.add("Você guarda pelo menos 20% dos seus adicionais, como sugere o método 50/30/20.");
        }
        if (!semPlano.isEmpty()) d.add("Você ainda não definiu como usar seu " + nomes(semPlano) + ".");
        return d;
    }

    private static String nomes(Set<EventoAdicional> eventos) {
        List<String> n = eventos.stream().map(e -> e == EventoAdicional.DECIMO ? "13º" : "PLR").toList();
        return String.join(" e ", n);
    }

    private static BigDecimal pct(BigDecimal valor, BigDecimal total) {
        return valor.multiply(CEM).divide(total, 1, RoundingMode.HALF_UP);
    }

    private static String p(BigDecimal v) {
        return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(PT_BR)).format(v) + "%";
    }

    private static String m(BigDecimal v) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR)).format(v);
    }
}
