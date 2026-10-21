package br.com.gestafinanceira.domain.calculo.metas;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import java.math.BigDecimal;

/**
 * Aporte mensal de uma meta em um mês e o saldo guardado previsto ao fim desse mês. {@code aporteAdicional} é a
 * reserva vinda do plano de uso do 13º/PLR no mês (já incluída em {@code guardado}; não faz parte de {@code aporte}).
 */
public record AporteMeta(Long id, String nome, BigDecimal aporte, BigDecimal guardado, BigDecimal aporteAdicional) {

    public AporteMeta(Long id, String nome, BigDecimal aporte, BigDecimal guardado) {
        this(id, nome, aporte, guardado, Dinheiro.ZERO);
    }
}
