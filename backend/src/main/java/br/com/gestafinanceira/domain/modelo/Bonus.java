package br.com.gestafinanceira.domain.modelo;

import java.math.BigDecimal;

/** Bônus Caixa: base × salarios, pago (líquido) no mês informado; só existe para cargos de jornada de 8h. */
public record Bonus(boolean ativo, int mes, BigDecimal salarios) {
    public static final Bonus INATIVO = new Bonus(false, 12, BigDecimal.ZERO);
}
