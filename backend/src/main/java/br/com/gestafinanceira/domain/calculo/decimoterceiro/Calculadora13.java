package br.com.gestafinanceira.domain.calculo.decimoterceiro;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.Parametros;
import br.com.gestafinanceira.domain.calculo.inss.CalculadoraInss;
import br.com.gestafinanceira.domain.calculo.irpf.CalculadoraIrpf;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.calculo.descontos.Funcef;
import br.com.gestafinanceira.domain.calculo.descontos.SaudeCaixa;
import br.com.gestafinanceira.domain.modelo.Saude;
import java.math.BigDecimal;
import java.util.List;

public final class Calculadora13 {
    private Calculadora13() {}

    public static BigDecimal parcela(BigDecimal base, ParcelaDecimo parcela) {
        return Dinheiro.percentual(base, parcela.percentual());
    }

    public static BigDecimal totalPago(BigDecimal base, List<ParcelaDecimo> parcelas) {
        BigDecimal pct = parcelas.stream().map(ParcelaDecimo::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.percentual(base, pct);
    }

    /**
     * Desconto da ultima parcela do ano: INSS(total) + IR(total) + FUNCEF + Saude Caixa, com total = soma das parcelas.
     * PREMISSA REPLICADA DA PLANILHA (a confirmar): se ha FUNCEF, o IR usa a deducao fixa de 607,20 (INSS e FUNCEF
     * nao deduzidos), o que aumenta o IR; sem FUNCEF, o INSS e deduzido. A saude nao e arredondada na planilha.
     */
    public static BigDecimal descontoNaUltimaParcela(BigDecimal base, List<ParcelaDecimo> parcelas, Parametros p,
                                                     BigDecimal funcefPct, Saude saude, int dependentes) {
        BigDecimal total = totalPago(base, parcelas);
        BigDecimal inss = CalculadoraInss.calcular(total, p);
        BigDecimal funcef = Funcef.calcular(total, funcefPct);
        BigDecimal ir = funcef.signum() == 0
                ? CalculadoraIrpf.calcular(total, inss, BigDecimal.ZERO, p).imposto()
                : CalculadoraIrpf.calcular(total, BigDecimal.ZERO, BigDecimal.ZERO, p).imposto();
        BigDecimal saudeTotal = SaudeCaixa.calcular(saude, dependentes, total, p);
        return Dinheiro.arredondar(inss.add(ir).add(funcef).add(saudeTotal));
    }

    /** Sem FUNCEF e sem saude. */
    public static BigDecimal descontoNaUltimaParcela(BigDecimal base, List<ParcelaDecimo> parcelas, Parametros p) {
        return descontoNaUltimaParcela(base, parcelas, p, BigDecimal.ZERO, Saude.NAO, 0);
    }
}
