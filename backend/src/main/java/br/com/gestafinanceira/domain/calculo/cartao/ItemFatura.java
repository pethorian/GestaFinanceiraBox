package br.com.gestafinanceira.domain.calculo.cartao;

import java.math.BigDecimal;

/** Linha da fatura: {@code parcela} é o rótulo "3/10" (nulo em compra à vista e assinatura). */
public record ItemFatura(Long compraId, String descricao, String categoria, String parcela, BigDecimal valor) {
}
