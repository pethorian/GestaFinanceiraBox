package br.com.gestafinanceira.domain.calculo.irpf;

import java.math.BigDecimal;

/** Memória de cálculo do IRPF. */
public record DetalheIrpf(
        BigDecimal deducaoLegal,
        BigDecimal deducaoMinima,
        BigDecimal deducao,
        BigDecimal baseCalculo,
        BigDecimal aliquota,
        BigDecimal bruto,
        BigDecimal reducao,
        BigDecimal imposto) {
}
