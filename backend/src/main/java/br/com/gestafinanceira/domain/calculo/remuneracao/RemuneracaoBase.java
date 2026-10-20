package br.com.gestafinanceira.domain.calculo.remuneracao;

import br.com.gestafinanceira.domain.calculo.CargoVigente;
import br.com.gestafinanceira.domain.calculo.Dinheiro;
import java.math.BigDecimal;

public final class RemuneracaoBase {
    private RemuneracaoBase() {}

    /** MAX(piso, delta + gratificação) para cargos com delta; piso + gratificação nos demais. */
    public static BigDecimal calcular(CargoVigente cargo, BigDecimal valorDelta) {
        if (cargo.usaDelta()) {
            return Dinheiro.arredondar(Dinheiro.max(cargo.piso(), valorDelta.add(cargo.gratificacao())));
        }
        return Dinheiro.arredondar(cargo.piso().add(cargo.gratificacao()));
    }
}
