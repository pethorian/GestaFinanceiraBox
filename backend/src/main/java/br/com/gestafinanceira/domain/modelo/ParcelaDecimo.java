package br.com.gestafinanceira.domain.modelo;

import java.math.BigDecimal;

/** Parcela do 13º: mês 1..12 e percentual 0..100 do salário base. */
public record ParcelaDecimo(int mes, BigDecimal percentual) {
}
