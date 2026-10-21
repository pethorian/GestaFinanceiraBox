package br.com.gestafinanceira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gestafinanceira.domain.calculo.ParametrosMes;
import br.com.gestafinanceira.domain.calculo.descontos.Funcef;
import br.com.gestafinanceira.domain.calculo.projecao.DadosProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.domain.modelo.Bonus;
import br.com.gestafinanceira.domain.modelo.Ferias;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import br.com.gestafinanceira.domain.modelo.Saude;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Previdência FUNCEF na projeção: saldo informado + contribuições do participante (mensais e do 13º), sem rendimento. */
class PrevidenciaFuncefTest {
    private static final YearMonth OUT_26 = YearMonth.of(2026, 10);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static ParametrosMes pm(YearMonth m) {
        return new ParametrosMes(MotorCalculoTest.P, MotorCalculoTest.TECNICO, bd("9000"));
    }

    private static PerfilCalculo perfil(String pct, String saldoFuncef) {
        return new PerfilCalculo(228, 0, 1, true, false, Saude.NAO, 0, bd(pct), false, false, bd("1000"),
                new Ferias(false, 7, false), Bonus.INATIVO, bd(saldoFuncef));
    }

    private static List<MesProjecao> projetar(PerfilCalculo perfil, List<ParcelaDecimo> decimo) {
        return ProjetorMensal.projetar(new DadosProjecao(perfil, List.of(), decimo, List.of(), List.of()), OUT_26,
                PrevidenciaFuncefTest::pm);
    }

    @Test
    void previdenciaSomaSaldoEContribuicoesMesAMes() {
        List<MesProjecao> meses = projetar(perfil("6", "10000"), List.of());
        BigDecimal esperado = bd("10000");
        for (MesProjecao m : meses) {
            assertThat(m.funcef()).isGreaterThan(BigDecimal.ZERO);
            assertThat(m.funcefDecimo()).isEqualByComparingTo("0");
            esperado = esperado.add(m.funcef());
            assertThat(m.previdencia()).isEqualByComparingTo(esperado);
            assertThat(m.patrimonioTotal()).isEqualByComparingTo(m.patrimonio().add(m.previdencia()));
            // patrimonio (acumulado + guardado) não inclui a previdência
            assertThat(m.patrimonio()).isEqualByComparingTo(m.acumulado().add(m.guardado()));
        }
    }

    @Test
    void contribuicaoSobreO13EntraNaPrevidenciaNoMesDaUltimaParcela() {
        List<ParcelaDecimo> decimo = List.of(new ParcelaDecimo(11, bd("50")), new ParcelaDecimo(12, bd("50")));
        List<MesProjecao> meses = projetar(perfil("6", "0"), decimo);
        MesProjecao dez = meses.stream().filter(m -> m.competencia().equals(YearMonth.of(2026, 12))).findFirst()
                .orElseThrow();
        MesProjecao nov = meses.stream().filter(m -> m.competencia().equals(YearMonth.of(2026, 11))).findFirst()
                .orElseThrow();
        BigDecimal esperado = Funcef.calcular(dez.base(), bd("6"));
        assertThat(dez.funcefDecimo()).isEqualByComparingTo(esperado);
        assertThat(nov.funcefDecimo()).isEqualByComparingTo("0");
        // saldo evolui: dezembro = novembro + funcef do mês + funcef do 13º
        assertThat(dez.previdencia()).isEqualByComparingTo(nov.previdencia().add(dez.funcef()).add(esperado));
    }

    @Test
    void semFuncefESemSaldoPrevidenciaZeraEPatrimonioTotalIgualPatrimonio() {
        List<MesProjecao> meses = projetar(perfil("0", "0"), List.of());
        for (MesProjecao m : meses) {
            assertThat(m.funcef()).isEqualByComparingTo("0");
            assertThat(m.previdencia()).isEqualByComparingTo("0");
            assertThat(m.patrimonioTotal()).isEqualByComparingTo(m.patrimonio());
        }
    }

    @Test
    void saldoSemContribuicaoPermaneceConstante() {
        List<MesProjecao> meses = projetar(perfil("0", "2500.50"), List.of());
        for (MesProjecao m : meses) {
            assertThat(m.previdencia()).isEqualByComparingTo("2500.50");
        }
    }

    @Test
    void mesIsoladoUsaSaldoMaisContribuicaoDoMes() {
        MesProjecao m = ProjetorMensal.calcularMes(
                new DadosProjecao(perfil("6", "1000"), List.of(), List.of(), List.of(), List.of()), OUT_26, 0, true,
                PrevidenciaFuncefTest::pm);
        assertThat(m.previdencia()).isEqualByComparingTo(bd("1000").add(m.funcef()));
    }
}
