package br.com.gestafinanceira.domain.calculo.cartao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Compra no cartão no formato do motor. {@code valor} é o total da compra (assinatura: o valor mensal);
 * {@code parcelas} só vale em PARCELADA (>= 2).
 */
public record CompraCartaoCalculo(Long id, String descricao, String categoria, TipoCompra tipo,
                                  LocalDate dataCompra, BigDecimal valor, int parcelas) {
}
