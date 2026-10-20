package br.com.gestafinanceira.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.gestafinanceira.domain.calculo.decimoterceiro.Calculadora13;
import br.com.gestafinanceira.domain.calculo.decimoterceiro.CalculadoraFerias;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.Saude;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valores dourados da planilha (Tabela Salarial 2026) para 13o e ferias liquidos. Tolerancia de 0,01. */
class DecimoFeriasPlanilhaTest {
    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    // cargo, base, ferias liquidas
    static final String[][] FERIAS = {
        {"TÉCNICO BANCÁRIO NOVO", "4351.71", "933.71"},
        {"ASSISTENTE JÚNIOR", "8940.83", "2160.71"},
        {"ASSISTENTE PLENO", "11875.43", "2869.9"},
        {"ASSISTENTE SÊNIOR", "13682.63", "3306.64"},
        {"ASSISTENTE MASTER TI", "15759.66", "3808.59"},
        {"CONSULTOR DE TI", "18166.12", "4390.14"},
        {"SUPERVISOR DE TI", "18553.08", "4483.66"},
        {"COORDENADOR DE PROJETOS TI", "21381.01", "5167.08"},
        {"COORDENADOR DE TI", "24638.78", "5954.37"},
        {"GERENTE EXECUTIVO OU GERENTE DE CENTRALIZADORA", "32730.39", "7909.85"},
        {"GERENTE NACIONAL", "43463.77", "10503.74"},
        {"SUPERINTENDENTE NACIONAL", "57723.71", "13949.9"}
    };

    // cargo, base, 13o liquido: sem saude/sem FUNCEF, com saude/sem FUNCEF, sem saude/com FUNCEF 12%, com saude/com FUNCEF
    static final String[][] DECIMO = {
        {"TÉCNICO BANCÁRIO NOVO", "4351.71", "3940.9", "3788.59015", "3418.6948", "3266.38495"},
        {"ASSISTENTE JÚNIOR", "8940.83", "6674.47", "6361.54095", "5496.8204", "5183.89135"},
        {"SUPERVISOR DE TI", "18553.08", "13643.35", "12993.9922", "11312.2304", "10662.8726"},
        {"SUPERINTENDENTE NACIONAL", "57723.71", "42042.05", "40021.72015", "35010.4648", "32990.13495"}
    };

    @Test
    void feriasLiquidasBatemComAPlanilhaNosDozeCargos() {
        for (String[] c : FERIAS) {
            assertThat(CalculadoraFerias.valor(bd(c[1]), false, MotorCalculoTest.P).doubleValue())
                    .as(c[0]).isCloseTo(Double.parseDouble(c[2]), within(0.01));
        }
    }

    @Test
    void feriasComAbonoSomamUmTercoIsento() {
        BigDecimal sem = CalculadoraFerias.valor(bd("4351.71"), false, MotorCalculoTest.P);
        BigDecimal com = CalculadoraFerias.valor(bd("4351.71"), true, MotorCalculoTest.P);
        assertThat(com.subtract(sem)).isEqualByComparingTo("1450.57");
    }

    @Test
    void decimoLiquidoBateComAPlanilhaNosQuatroCenarios() {
        List<ParcelaDecimo> cem = List.of(new ParcelaDecimo(12, bd("100")));
        for (String[] c : DECIMO) {
            BigDecimal base = bd(c[1]);
            double[] esperado = {Double.parseDouble(c[2]), Double.parseDouble(c[3]), Double.parseDouble(c[4]),
                    Double.parseDouble(c[5])};
            String[] funcef = {"0", "0", "12", "12"};
            Saude[] saude = {Saude.NAO, Saude.SOZINHO, Saude.NAO, Saude.SOZINHO};
            for (int k = 0; k < 4; k++) {
                BigDecimal desc = Calculadora13.descontoNaUltimaParcela(base, cem, MotorCalculoTest.P, bd(funcef[k]),
                        saude[k], 0);
                assertThat(base.subtract(desc).doubleValue()).as(c[0] + " cenario " + (k + 1))
                        .isCloseTo(esperado[k], within(0.011));
            }
        }
    }
}
