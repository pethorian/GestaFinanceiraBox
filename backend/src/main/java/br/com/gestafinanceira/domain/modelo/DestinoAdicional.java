package br.com.gestafinanceira.domain.modelo;

import java.math.BigDecimal;

/**
 * Fatia do plano de uso de um adicional: {@code percentual} (0 &lt; p ≤ 100) do líquido de cada parcela do
 * {@code evento}. {@code metaId} (só com {@code RESERVA}) liga a fatia a uma meta; nulo = reserva dos adicionais.
 */
public record DestinoAdicional(Long id, EventoAdicional evento, String nome, TipoDestino tipo, BigDecimal percentual,
                               Long metaId) {
}
