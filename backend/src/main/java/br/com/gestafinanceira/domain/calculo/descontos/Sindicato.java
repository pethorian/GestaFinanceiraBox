package br.com.gestafinanceira.domain.calculo.descontos;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.Parametros;
import java.math.BigDecimal;

public final class Sindicato {
    private Sindicato() {}

    /** MIN(1% × (base + sobreaviso), teto). */
    public static BigDecimal calcular(boolean ativo, BigDecimal baseMaisSobreaviso, Parametros p) {
        if (!ativo) return Dinheiro.ZERO;
        return Dinheiro.arredondar(Dinheiro.min(baseMaisSobreaviso.multiply(p.sindicatoPercentual()), p.sindicatoTeto()));
    }
}
