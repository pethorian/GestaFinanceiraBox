package br.com.gestafinanceira.domain.calculo.adicionais;

import br.com.gestafinanceira.domain.modelo.TipoDestino;
import java.math.BigDecimal;

/** Valor de um destino em uma parcela do adicional. */
public record DestinoValor(Long id, String nome, TipoDestino tipo, Long metaId, BigDecimal percentual,
                           BigDecimal valor) {
}
