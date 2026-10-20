package br.com.gestafinanceira.domain.calculo.remuneracao;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.Parametros;
import java.math.BigDecimal;

public final class Beneficios {
    private Beneficios() {}

    public static BigDecimal auxilioCreche(int filhos, Parametros p) {
        return Dinheiro.arredondar(p.pai().multiply(BigDecimal.valueOf(filhos)));
    }

    public static BigDecimal homeOffice(boolean ativo, Parametros p) {
        return ativo ? Dinheiro.arredondar(p.homeOffice()) : Dinheiro.ZERO;
    }

    public static BigDecimal vrVa(boolean ativo, Parametros p) {
        return ativo ? Dinheiro.arredondar(p.vrVa()) : Dinheiro.ZERO;
    }
}
