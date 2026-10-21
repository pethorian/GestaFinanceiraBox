package br.com.gestafinanceira.domain.modelo;

import java.math.BigDecimal;

/** Parcela do PLR: mês 1..12, percentual do PLR anual do cargo e, opcionalmente, valor bruto que substitui o calculado. */
public record ParcelaPlr(int mes, BigDecimal percentual, BigDecimal valor) {

    public ParcelaPlr(int mes, BigDecimal percentual) {
        this(mes, percentual, null);
    }
}
