package br.com.gestafinanceira.domain.calculo.descontos;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.Parametros;
import br.com.gestafinanceira.domain.modelo.Saude;
import java.math.BigDecimal;

public final class SaudeCaixa {
    private SaudeCaixa() {}

    public static BigDecimal calcular(Saude plano, int dependentes, BigDecimal base, Parametros p) {
        BigDecimal titular = base.multiply(p.saudePercentual());
        return switch (plano) {
            case NAO -> Dinheiro.ZERO;
            case SOZINHO -> Dinheiro.arredondar(titular);
            case DEP -> Dinheiro.arredondar(Dinheiro.min(
                    p.planoPorDependente().multiply(BigDecimal.valueOf(dependentes)).add(titular),
                    base.multiply(p.saudeTetoPercentual())));
        };
    }
}
