package br.com.gestafinanceira.domain.calculo.remuneracao;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.Parametros;
import java.math.BigDecimal;
import java.math.MathContext;

public final class Sobreaviso {
    private Sobreaviso() {}

    /** base × dias × 8 ÷ divisor (180 para jornada de 6h, 220 para 8h). */
    public static BigDecimal calcular(BigDecimal base, int dias, int jornada, Parametros p) {
        int divisor = jornada == 6 ? p.divisorJornada6h() : p.divisorJornada8h();
        BigDecimal v = base.multiply(BigDecimal.valueOf(dias)).multiply(BigDecimal.valueOf(8))
                .divide(BigDecimal.valueOf(divisor), MathContext.DECIMAL64);
        return Dinheiro.arredondar(v);
    }
}
