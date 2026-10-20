package br.com.gestafinanceira.domain.calculo;

import java.math.BigDecimal;

public record CargoVigente(String nome, int jornada, boolean usaDelta, BigDecimal piso, BigDecimal gratificacao,
                           BigDecimal plrAnual) {

    public CargoVigente(String nome, int jornada, boolean usaDelta, BigDecimal piso, BigDecimal gratificacao) {
        this(nome, jornada, usaDelta, piso, gratificacao, BigDecimal.ZERO);
    }
}
