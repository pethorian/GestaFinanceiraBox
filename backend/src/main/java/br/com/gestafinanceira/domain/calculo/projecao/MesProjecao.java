package br.com.gestafinanceira.domain.calculo.projecao;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.irpf.DetalheIrpf;
import br.com.gestafinanceira.domain.calculo.metas.AporteMeta;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Resultado de um mês da projeção. Descontos são valores positivos; líquido = receitas − descontos.
 * {@code plr} é o valor LÍQUIDO do PLR do mês (já entra em receitas); {@code plrBruto} e {@code plrDesconto}
 * (IR exclusivo na fonte) são informativos.
 *
 * <p>Metas: {@code aportes} é o total aportado nas metas no mês (saída da conta); {@code sobra} =
 * líquido − gastos − aportes; {@code acumulado} é o dinheiro disponível em conta (saldo inicial + sobras);
 * {@code guardado} é a soma dos saldos das metas ao fim do mês; {@code patrimonio} = acumulado + guardado.
 * {@code metas} detalha aporte e guardado de cada meta.
 */
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
        BigDecimal aportes,
        BigDecimal sobra,
        BigDecimal acumulado,
        BigDecimal guardado,
        BigDecimal patrimonio,
        String evento,
        BigDecimal baseInss,
        boolean tetoInssAplicado,
        BigDecimal plrBruto,
        BigDecimal plrDesconto,
        List<AporteMeta> metas) {

    /** Mesmo mês com o saldo disponível em conta informado (o patrimônio acompanha). */
    public MesProjecao comAcumulado(BigDecimal valor) {
        return new MesProjecao(indice, competencia, base, sobreaviso, creche, homeOffice, vrVa, decimo, ferias, plr, bonus,
                inss, irpf, funcef, saude, valeTransporte, sindicato, descontoDecimo, receitas, descontos,
                liquido, gastos, aportes, sobra, valor, guardado, Dinheiro.arredondar(valor.add(guardado)), evento,
                baseInss, tetoInssAplicado, plrBruto, plrDesconto, metas);
    }
}
