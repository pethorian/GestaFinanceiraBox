package br.com.gestafinanceira.domain.calculo.metas;

import java.math.BigDecimal;

/** Aporte de uma meta em um mês e o saldo guardado previsto ao fim desse mês. */
public record AporteMeta(Long id, String nome, BigDecimal aporte, BigDecimal guardado) {
}
