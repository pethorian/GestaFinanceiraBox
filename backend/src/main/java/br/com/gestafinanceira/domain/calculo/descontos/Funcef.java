package br.com.gestafinanceira.domain.calculo.descontos;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import java.math.BigDecimal;

public final class Funcef {
    private Funcef() {}

    /** Percentual livre (0..100) sobre a remuneração base. */
    public static BigDecimal calcular(BigDecimal base, BigDecimal percentual) {
        return Dinheiro.percentual(base, percentual);
    }
}
