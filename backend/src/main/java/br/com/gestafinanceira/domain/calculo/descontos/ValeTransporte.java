package br.com.gestafinanceira.domain.calculo.descontos;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.Parametros;

import java.math.BigDecimal;

public final class ValeTransporte {
    private ValeTransporte() {}

    /** 4% (vt_percentual) do valor do delta escolhido no perfil, como no simulador. */
    public static BigDecimal calcular(boolean ativo, BigDecimal valorDelta, Parametros p) {
        return ativo ? Dinheiro.arredondar(valorDelta.multiply(p.vtPercentual())) : Dinheiro.ZERO;
    }
}
