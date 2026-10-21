package br.com.gestafinanceira.domain.calculo.plr;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.FaixaProgressiva;
import br.com.gestafinanceira.domain.modelo.ParcelaPlr;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PLR (Lei 10.101/2000): sem INSS; IR em tributação exclusiva na fonte por tabela própria, aplicada sobre o total
 * do PLR do ciclo (soma das parcelas), descontando o que já foi retido nas parcelas anteriores
 * (Lei 12.832/2013, que alterou a Lei 10.101). Suposição de modelagem: a antecipação (parcelas que não são o saldo)
 * é paga sem retenção e a parcela de saldo carrega todo o imposto do ciclo. O saldo é a parcela de menor mês
 * (paga no início do ano seguinte à antecipação). O redutor da Lei 15.270/2025 não se aplica ao PLR.
 */
public final class CalculadoraPlr {
    private CalculadoraPlr() {}

    /** Resultado de uma parcela; {@code liquido = bruto - desconto}. */
    public record ParcelaCalculada(int mes, BigDecimal bruto, BigDecimal desconto, BigDecimal liquido) {}

    /** Calcula todas as parcelas, na mesma ordem recebida. */
    public static List<ParcelaCalculada> calcular(List<ParcelaPlr> parcelas, BigDecimal estimativaAnual,
                                                  List<FaixaProgressiva> faixas) {
        if (parcelas.isEmpty()) return List.of();
        List<BigDecimal> brutos = parcelas.stream().map(p -> bruto(p, estimativaAnual)).toList();
        BigDecimal total = brutos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal imposto = imposto(total, faixas);
        int mesSaldo = parcelas.stream().map(ParcelaPlr::mes).min(Comparator.naturalOrder()).orElseThrow();
        List<ParcelaCalculada> r = new ArrayList<>();
        for (int i = 0; i < parcelas.size(); i++) {
            BigDecimal bruto = brutos.get(i);
            boolean saldo = parcelas.get(i).mes() == mesSaldo;
            BigDecimal desconto = saldo ? Dinheiro.min(imposto, bruto) : Dinheiro.ZERO;
            r.add(new ParcelaCalculada(parcelas.get(i).mes(), bruto, desconto,
                    Dinheiro.arredondar(bruto.subtract(desconto))));
        }
        return r;
    }

    /** Valor informado (bruto) ou estimativa anual × percentual. */
    public static BigDecimal bruto(ParcelaPlr p, BigDecimal estimativaAnual) {
        return p.valor() != null ? Dinheiro.arredondar(p.valor()) : Dinheiro.percentual(estimativaAnual, p.percentual());
    }

    /** IR exclusivo sobre o total: total × alíquota − dedução da faixa (piso zero). Sem tabela, sem imposto. */
    public static BigDecimal imposto(BigDecimal total, List<FaixaProgressiva> faixas) {
        if (faixas == null || faixas.isEmpty() || total.signum() <= 0) return Dinheiro.ZERO;
        FaixaProgressiva f = FaixaProgressiva.localizar(faixas, total);
        return Dinheiro.arredondar(Dinheiro.max(total.multiply(f.aliquota()).subtract(f.deducao()), BigDecimal.ZERO));
    }
}
