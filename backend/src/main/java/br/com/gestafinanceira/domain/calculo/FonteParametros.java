package br.com.gestafinanceira.domain.calculo;

import java.time.YearMonth;

/** Porta do domínio: devolve os parâmetros vigentes em cada mês (implementada pela camada de aplicação). */
@FunctionalInterface
public interface FonteParametros {
    ParametrosMes para(YearMonth mes);
}
