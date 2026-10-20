package br.com.gestafinanceira.domain.modelo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public record GastoCalculo(TipoLancamento tipo, BigDecimal valor, YearMonth mesInicial, int parcelas) {

    /** Valor devido no mês informado. */
    public BigDecimal valorEm(YearMonth mes) {
        long d = ChronoUnit.MONTHS.between(mesInicial, mes);
        boolean devido = switch (tipo) {
            case RECORRENTE -> d >= 0;
            case PONTUAL -> d == 0;
            case PARCELADO -> d >= 0 && d < parcelas;
        };
        return devido ? valor : BigDecimal.ZERO;
    }
}
