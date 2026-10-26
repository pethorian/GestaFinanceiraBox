package br.com.gestafinanceira.domain.calculo.cartao;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Fatura que vence em {@code mes}. */
public record Fatura(YearMonth mes, BigDecimal total, List<ItemFatura> itens) {
}
