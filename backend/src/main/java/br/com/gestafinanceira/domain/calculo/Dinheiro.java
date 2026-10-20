package br.com.gestafinanceira.domain.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento monetário igual ao ROUND do Excel: HALF_UP com 2 casas. */
public final class Dinheiro {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private Dinheiro() {}

    public static BigDecimal arredondar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    /** Percentual no formato 0..100 (ex.: 6 = 6%) aplicado sobre um valor. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal pct) {
        return arredondar(valor.multiply(pct).divide(CEM));
    }
}
