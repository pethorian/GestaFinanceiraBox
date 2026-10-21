package br.com.gestafinanceira.domain.calculo;

import java.math.BigDecimal;
import java.util.List;

/** Parâmetros legais e de benefícios vigentes em um determinado mês. Percentuais em fração (0,04 = 4%). */
public record Parametros(
        BigDecimal pai,
        BigDecimal homeOffice,
        BigDecimal vrVa,
        BigDecimal vtPercentual,
        BigDecimal sindicatoPercentual,
        BigDecimal sindicatoTeto,
        BigDecimal saudePercentual,
        BigDecimal saudeTetoPercentual,
        BigDecimal planoPorDependente,
        BigDecimal inssTeto,
        BigDecimal inssTetoSalario,
        BigDecimal irDeducaoMinima,
        BigDecimal irLimiteIsencao,
        BigDecimal irLimiteReducao,
        BigDecimal irReducaoA,
        BigDecimal irReducaoB,
        int divisorJornada6h,
        int divisorJornada8h,
        BigDecimal bonusCaixaMaxSalarios,
        List<FaixaProgressiva> faixasInss,
        List<FaixaProgressiva> faixasIrpf,
        List<FaixaProgressiva> faixasPlr) {

    /** Sem tabela do PLR (PLR sem imposto). */
    public Parametros(BigDecimal pai, BigDecimal homeOffice, BigDecimal vrVa, BigDecimal vtPercentual,
                      BigDecimal sindicatoPercentual, BigDecimal sindicatoTeto, BigDecimal saudePercentual,
                      BigDecimal saudeTetoPercentual, BigDecimal planoPorDependente, BigDecimal inssTeto,
                      BigDecimal inssTetoSalario, BigDecimal irDeducaoMinima, BigDecimal irLimiteIsencao,
                      BigDecimal irLimiteReducao, BigDecimal irReducaoA, BigDecimal irReducaoB, int divisorJornada6h,
                      int divisorJornada8h, BigDecimal bonusCaixaMaxSalarios, List<FaixaProgressiva> faixasInss,
                      List<FaixaProgressiva> faixasIrpf) {
        this(pai, homeOffice, vrVa, vtPercentual, sindicatoPercentual, sindicatoTeto, saudePercentual,
                saudeTetoPercentual, planoPorDependente, inssTeto, inssTetoSalario, irDeducaoMinima, irLimiteIsencao,
                irLimiteReducao, irReducaoA, irReducaoB, divisorJornada6h, divisorJornada8h, bonusCaixaMaxSalarios,
                faixasInss, faixasIrpf, List.of());
    }

    public Parametros comFaixasPlr(List<FaixaProgressiva> faixas) {
        return new Parametros(pai, homeOffice, vrVa, vtPercentual, sindicatoPercentual, sindicatoTeto, saudePercentual,
                saudeTetoPercentual, planoPorDependente, inssTeto, inssTetoSalario, irDeducaoMinima, irLimiteIsencao,
                irLimiteReducao, irReducaoA, irReducaoB, divisorJornada6h, divisorJornada8h, bonusCaixaMaxSalarios,
                faixasInss, faixasIrpf, faixas);
    }
}
