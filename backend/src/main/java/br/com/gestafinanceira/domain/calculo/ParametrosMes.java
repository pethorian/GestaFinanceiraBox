package br.com.gestafinanceira.domain.calculo;

import java.math.BigDecimal;

/** Tudo o que o cálculo de um mês precisa saber sobre as tabelas vigentes naquele mês. */
public record ParametrosMes(Parametros parametros, CargoVigente cargo, BigDecimal valorDelta) {
}
