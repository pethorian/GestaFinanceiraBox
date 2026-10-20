package br.com.gestafinanceira.domain.calculo.projecao;

import br.com.gestafinanceira.domain.calculo.irpf.DetalheIrpf;
import java.math.BigDecimal;
import java.time.YearMonth;

/** Resultado de um mês da projeção. Descontos são valores positivos; líquido = receitas − descontos. */
public record MesProjecao(
        int indice,
        YearMonth competencia,
        BigDecimal base,
        BigDecimal sobreaviso,
        BigDecimal creche,
        BigDecimal homeOffice,
        BigDecimal vrVa,
        BigDecimal decimo,
        BigDecimal ferias,
        BigDecimal plr,
        BigDecimal bonus,
        BigDecimal inss,
        DetalheIrpf irpf,
        BigDecimal funcef,
        BigDecimal saude,
        BigDecimal valeTransporte,
        BigDecimal sindicato,
        BigDecimal descontoDecimo,
        BigDecimal receitas,
        BigDecimal descontos,
        BigDecimal liquido,
        BigDecimal gastos,
        BigDecimal sobra,
        BigDecimal acumulado,
        String evento,
        BigDecimal baseInss,
        boolean tetoInssAplicado) {

    public MesProjecao comAcumulado(BigDecimal valor) {
        return new MesProjecao(indice, competencia, base, sobreaviso, creche, homeOffice, vrVa, decimo, ferias, plr, bonus,
                inss, irpf, funcef, saude, valeTransporte, sindicato, descontoDecimo, receitas, descontos,
                liquido, gastos, sobra, valor, evento, baseInss, tetoInssAplicado);
    }
}
