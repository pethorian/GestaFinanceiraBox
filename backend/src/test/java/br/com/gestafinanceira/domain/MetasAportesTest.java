package br.com.gestafinanceira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gestafinanceira.domain.calculo.ParametrosMes;
import br.com.gestafinanceira.domain.calculo.metas.AporteMeta;
import br.com.gestafinanceira.domain.calculo.metas.CalculadoraAportes;
import br.com.gestafinanceira.domain.calculo.projecao.DadosProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.domain.modelo.Ferias;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import br.com.gestafinanceira.domain.modelo.Saude;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Aportes das metas: até o alvo, último aporte parcial, sem alvo, várias metas e efeito na sobra/patrimônio. */
class MetasAportesTest {
    private static final YearMonth OUT_26 = YearMonth.of(2026, 10);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static MetaCalculo meta(String alvo, String atual, String aporte) {
        return new MetaCalculo(1L, "Meta", bd(alvo), bd(atual), bd(aporte));
    }

    private static PerfilCalculo perfil() {
        return new PerfilCalculo(228, 0, 1, true, false, Saude.DEP, 2, bd("6"), true, true, bd("3200"),
                new Ferias(false, 7, false));
    }

    private static ParametrosMes pm(YearMonth m) {
        return new ParametrosMes(MotorCalculoTest.P, MotorCalculoTest.TECNICO, bd("9000"));
    }

    private static List<MesProjecao> projetar(List<MetaCalculo> metas) {
        GastoCalculo aluguel = new GastoCalculo(TipoLancamento.RECORRENTE, bd("2500"), YearMonth.of(2026, 1), 0);
        return ProjetorMensal.projetar(new DadosProjecao(perfil(), List.of(aluguel), List.of(), List.of(), metas),
                OUT_26, MetasAportesTest::pm);
    }

    // ---- regra do aporte -----------------------------------------------------------------------

    @Test
    void aportaOMensalEnquantoNaoAtingeOAlvo() {
        MetaCalculo m = meta("10000", "1000", "1500");
        assertThat(CalculadoraAportes.aporte(m, bd("1000"))).isEqualByComparingTo("1500.00");
        assertThat(CalculadoraAportes.aporte(m, bd("8500"))).isEqualByComparingTo("1500.00");
    }

    @Test
    void ultimoAporteEParcialEDepoisZera() {
        MetaCalculo m = meta("10000", "1000", "2000");
        // 1000 + 4 × 2000 = 9000 → o 5º aporte é só o que falta (1000); depois, zero
        assertThat(CalculadoraAportes.aporte(m, bd("9000"))).isEqualByComparingTo("1000.00");
        assertThat(CalculadoraAportes.aporte(m, bd("10000"))).isEqualByComparingTo("0");
        assertThat(CalculadoraAportes.aporte(m, bd("12000"))).isEqualByComparingTo("0");
        assertThat(CalculadoraAportes.guardadoApos(m, 12)).isEqualByComparingTo("10000.00");
    }

    @Test
    void semAlvoAportaSempreESemPrevisao() {
        MetaCalculo m = meta("0", "500", "300");
        assertThat(CalculadoraAportes.aporte(m, bd("999999"))).isEqualByComparingTo("300.00");
        assertThat(CalculadoraAportes.guardadoApos(m, 12)).isEqualByComparingTo("4100.00");
        assertThat(CalculadoraAportes.aportesParaAtingir(m)).isEmpty();
        assertThat(CalculadoraAportes.previsao(m, OUT_26)).isEmpty();
    }

    @Test
    void semAporteNuncaAportaESemPrevisao() {
        MetaCalculo m = meta("10000", "1000", "0");
        assertThat(CalculadoraAportes.aporte(m, bd("1000"))).isEqualByComparingTo("0");
        assertThat(CalculadoraAportes.previsao(m, OUT_26)).isEmpty();
    }

    @Test
    void metaJaAtingidaNaoAportaEPrevisaoEOMesAtual() {
        MetaCalculo m = meta("5000", "6000", "500");
        assertThat(CalculadoraAportes.aporte(m, bd("6000"))).isEqualByComparingTo("0");
        assertThat(CalculadoraAportes.aportesParaAtingir(m)).contains(0);
        assertThat(CalculadoraAportes.previsao(m, OUT_26)).contains(OUT_26);
    }

    @Test
    void previsaoEOMesDoUltimoAporte() {
        // falta 9000 com 1500/mês = 6 aportes (out..mar) → atinge em mar/27
        MetaCalculo exata = meta("10000", "1000", "1500");
        assertThat(CalculadoraAportes.aportesParaAtingir(exata)).contains(6);
        assertThat(CalculadoraAportes.previsao(exata, OUT_26)).contains(YearMonth.of(2027, 3));
        // falta 9000 com 2000/mês = 5 aportes (4 cheios + 1000) → fev/27
        assertThat(CalculadoraAportes.previsao(meta("10000", "1000", "2000"), OUT_26)).contains(YearMonth.of(2027, 2));
        // um aporte só (falta menor que o mensal) → o próprio mês
        assertThat(CalculadoraAportes.previsao(meta("1000", "900", "500"), OUT_26)).contains(OUT_26);
    }

    // ---- projeção ------------------------------------------------------------------------------

    @Test
    void previsaoCoincideComOMesEmQueAProjecaoAtingeOAlvo() {
        MetaCalculo m = meta("10000", "1000", "2000");
        List<MesProjecao> meses = projetar(List.of(m));
        YearMonth previsao = CalculadoraAportes.previsao(m, OUT_26).orElseThrow();
        MesProjecao atinge = meses.stream().filter(x -> x.guardado().compareTo(bd("10000")) >= 0).findFirst().orElseThrow();
        assertThat(atinge.competencia()).isEqualTo(previsao);
        assertThat(atinge.aportes()).isEqualByComparingTo("1000.00");
        // guardado ao fim da projeção = mesmo passo usado em guardadoApos
        assertThat(meses.get(11).guardado()).isEqualByComparingTo(CalculadoraAportes.guardadoApos(m, 12));
    }

    @Test
    void sobraDescontaAportesEAcumuladoSoOSaldoDisponivel() {
        List<MesProjecao> sem = projetar(List.of());
        List<MesProjecao> com = projetar(List.of(meta("10000", "1000", "2000")));
        // out..jan: 2000; fev: 1000 (último, parcial); mar em diante: 0
        String[] esperados = {"2000", "2000", "2000", "2000", "1000", "0", "0", "0", "0", "0", "0", "0"};
        for (int i = 0; i < 12; i++) {
            MesProjecao c = com.get(i);
            assertThat(c.aportes()).as("aporte do mês %d", i).isEqualByComparingTo(esperados[i]);
            assertThat(c.liquido()).isEqualByComparingTo(sem.get(i).liquido());
            assertThat(c.sobra()).isEqualByComparingTo(c.liquido().subtract(c.gastos()).subtract(c.aportes()));
            assertThat(c.sobra()).isEqualByComparingTo(sem.get(i).sobra().subtract(c.aportes()));
            assertThat(c.patrimonio()).isEqualByComparingTo(c.acumulado().add(c.guardado()));
        }
        // o dinheiro aportado continua da pessoa: patrimônio final = sem metas + o que já estava guardado
        assertThat(com.get(11).acumulado()).isEqualByComparingTo(sem.get(11).acumulado().subtract(bd("9000")));
        assertThat(com.get(11).guardado()).isEqualByComparingTo("10000.00");
        assertThat(com.get(11).patrimonio()).isEqualByComparingTo(sem.get(11).acumulado().add(bd("1000")));
    }

    @Test
    void variasMetasSomamAportesEGuardado() {
        MetaCalculo reserva = new MetaCalculo(1L, "Reserva", bd("30000"), bd("3200"), bd("900"));
        MetaCalculo carro = new MetaCalculo(2L, "Carro", bd("1000"), bd("0"), bd("600"));
        MetaCalculo livre = new MetaCalculo(3L, "Livre", bd("0"), bd("100"), bd("50"));
        List<MesProjecao> meses = projetar(List.of(reserva, carro, livre));

        MesProjecao out = meses.get(0);
        assertThat(out.aportes()).isEqualByComparingTo("1550.00"); // 900 + 600 + 50
        assertThat(out.guardado()).isEqualByComparingTo("4850.00"); // 4100 + 600 + 150
        assertThat(out.metas()).extracting(AporteMeta::nome).containsExactly("Reserva", "Carro", "Livre");
        MesProjecao nov = meses.get(1);
        assertThat(nov.aportes()).isEqualByComparingTo("1350.00"); // 900 + 400 (último do carro) + 50
        assertThat(nov.metas().get(1).guardado()).isEqualByComparingTo("1000.00");
        MesProjecao dez = meses.get(2);
        assertThat(dez.aportes()).isEqualByComparingTo("950.00"); // carro já atingido
        // 12 meses: reserva 3200 + 12 × 900 = 14000; carro 1000; livre 100 + 12 × 50 = 700
        assertThat(meses.get(11).guardado()).isEqualByComparingTo("15700.00");
        assertThat(meses.get(11).metas().get(0).guardado()).isEqualByComparingTo("14000.00");
    }

    @Test
    void mesIsoladoAportaAPartirDoValorAtual() {
        DadosProjecao dados = new DadosProjecao(perfil(), List.of(), List.of(), List.of(),
                List.of(meta("10000", "9500", "1500")));
        MesProjecao m = ProjetorMensal.calcularMes(dados, OUT_26, 0, true, MetasAportesTest::pm);
        assertThat(m.aportes()).isEqualByComparingTo("500.00");
        assertThat(m.guardado()).isEqualByComparingTo("10000.00");
        assertThat(m.sobra()).isEqualByComparingTo(m.liquido().subtract(bd("500")));
    }

    @Test
    void semMetasNadaMuda() {
        MesProjecao m = projetar(List.of()).get(0);
        assertThat(m.aportes()).isEqualByComparingTo("0");
        assertThat(m.guardado()).isEqualByComparingTo("0");
        assertThat(m.patrimonio()).isEqualByComparingTo(m.acumulado());
        assertThat(m.metas()).isEmpty();
    }
}
