package br.com.gestafinanceira.domain.calculo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Faixa de tabela progressiva (INSS ou IRPF). {@code ate == null} significa sem limite superior.
 * Imposto da faixa = valor × alíquota − dedução.
 */
public record FaixaProgressiva(BigDecimal ate, BigDecimal aliquota, BigDecimal deducao) {

    /** Primeira faixa cujo limite comporta o valor; se nenhuma, a última. */
    public static FaixaProgressiva localizar(List<FaixaProgressiva> faixas, BigDecimal valor) {
        for (FaixaProgressiva f : faixas) {
            if (f.ate() == null || valor.compareTo(f.ate()) <= 0) return f;
        }
        return faixas.get(faixas.size() - 1);
    }
}
