package br.com.gestafinanceira.domain.calculo.decimoterceiro;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.Parametros;
import br.com.gestafinanceira.domain.calculo.inss.CalculadoraInss;
import br.com.gestafinanceira.domain.calculo.irpf.CalculadoraIrpf;
import java.math.BigDecimal;
import java.math.MathContext;

public final class CalculadoraFerias {
    private static final BigDecimal TRES = BigDecimal.valueOf(3);

    private CalculadoraFerias() {}

    /**
     * Adicional de férias LIQUIDO (receita do mes das ferias, sem nova tributacao), como na planilha:
     * terco = base/3; b2 = base + base/3 (sem arredondar);
     * liquido = terco - (INSS(b2) - INSS(base)) - (IRPF(b2,0,0) - IRPF(base,0,0)).
     * O IRPF usa a deducao fixa (INSS/FUNCEF nao deduzidos). Abono: soma base/3 isento (premissa do prototipo, a confirmar).
     */
    public static BigDecimal valor(BigDecimal base, boolean abono, Parametros p) {
        BigDecimal terco = base.divide(TRES, MathContext.DECIMAL64);
        BigDecimal b2 = base.add(terco);
        BigDecimal dInss = CalculadoraInss.calcular(b2, p).subtract(CalculadoraInss.calcular(base, p));
        BigDecimal dIr = CalculadoraIrpf.calcular(b2, BigDecimal.ZERO, BigDecimal.ZERO, p).imposto()
                .subtract(CalculadoraIrpf.calcular(base, BigDecimal.ZERO, BigDecimal.ZERO, p).imposto());
        BigDecimal liquido = Dinheiro.arredondar(terco).subtract(dInss).subtract(dIr);
        if (abono) liquido = liquido.add(Dinheiro.arredondar(terco));
        return Dinheiro.arredondar(liquido);
    }
}
