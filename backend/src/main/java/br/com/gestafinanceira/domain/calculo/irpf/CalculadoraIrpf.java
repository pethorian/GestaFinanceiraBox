package br.com.gestafinanceira.domain.calculo.irpf;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.FaixaProgressiva;
import br.com.gestafinanceira.domain.calculo.Parametros;
import java.math.BigDecimal;

public final class CalculadoraIrpf {
    private CalculadoraIrpf() {}

    public static DetalheIrpf calcular(BigDecimal salario, BigDecimal inss, BigDecimal funcef, Parametros p) {
        BigDecimal deducao = Dinheiro.max(inss.add(funcef), p.irDeducaoMinima());
        BigDecimal base = Dinheiro.max(salario.subtract(deducao), BigDecimal.ZERO);
        FaixaProgressiva f = FaixaProgressiva.localizar(p.faixasIrpf(), base);
        BigDecimal bruto = Dinheiro.arredondar(
                Dinheiro.max(base.multiply(f.aliquota()).subtract(f.deducao()), BigDecimal.ZERO));

        BigDecimal reducao = Dinheiro.ZERO;
        BigDecimal imposto = bruto;
        if (salario.compareTo(p.irLimiteIsencao()) <= 0) {
            reducao = bruto;
            imposto = Dinheiro.ZERO;
        } else if (salario.compareTo(p.irLimiteReducao()) <= 0) {
            reducao = Dinheiro.arredondar(
                    Dinheiro.max(BigDecimal.ZERO, p.irReducaoA().subtract(p.irReducaoB().multiply(salario))));
            imposto = Dinheiro.arredondar(Dinheiro.max(BigDecimal.ZERO, bruto.subtract(reducao)));
        }
        return new DetalheIrpf(Dinheiro.arredondar(inss.add(funcef)), Dinheiro.arredondar(p.irDeducaoMinima()),
                Dinheiro.arredondar(deducao), Dinheiro.arredondar(base), f.aliquota(), bruto, reducao, imposto);
    }
}
