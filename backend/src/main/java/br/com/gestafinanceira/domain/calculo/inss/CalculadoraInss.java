package br.com.gestafinanceira.domain.calculo.inss;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.FaixaProgressiva;
import br.com.gestafinanceira.domain.calculo.Parametros;
import java.math.BigDecimal;

public final class CalculadoraInss {
    private CalculadoraInss() {}

    /** salário × alíquota − parcela a deduzir da faixa; acima do teto salarial aplica o teto de contribuição. */
    public static BigDecimal calcular(BigDecimal salario, Parametros p) {
        if (salario.compareTo(p.inssTetoSalario()) > 0) return Dinheiro.arredondar(p.inssTeto());
        FaixaProgressiva f = FaixaProgressiva.localizar(p.faixasInss(), salario);
        BigDecimal v = salario.multiply(f.aliquota()).subtract(f.deducao());
        return Dinheiro.arredondar(Dinheiro.max(v, BigDecimal.ZERO));
    }
}
