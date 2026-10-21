package br.com.gestafinanceira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gestafinanceira.domain.calculo.ParametrosMes;
import br.com.gestafinanceira.domain.calculo.adicionais.AdicionalMes;
import br.com.gestafinanceira.domain.calculo.adicionais.CalculadoraDestinos;
import br.com.gestafinanceira.domain.calculo.adicionais.DestinoValor;
import br.com.gestafinanceira.domain.calculo.adicionais.TotaisAdicionais;
import br.com.gestafinanceira.domain.calculo.projecao.DadosProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.domain.calculo.saude.SaudeAdicionais;
import br.com.gestafinanceira.domain.calculo.saude.StatusFaixa;
import br.com.gestafinanceira.domain.modelo.DestinoAdicional;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.domain.modelo.Ferias;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.ParcelaPlr;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import br.com.gestafinanceira.domain.modelo.Saude;
import br.com.gestafinanceira.domain.modelo.TipoDestino;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Plano de uso do 13º e do PLR: divisão, arredondamento, efeito na projeção e bloco da saúde financeira. */
class PlanoAdicionaisTest {
    private static final YearMonth OUT_26 = YearMonth.of(2026, 10);
    private static final List<ParcelaDecimo> DECIMO = List.of(
            new ParcelaDecimo(2, bd("50")), new ParcelaDecimo(11, bd("50")));

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static DestinoAdicional d(EventoAdicional e, String nome, TipoDestino t, String pct, Long metaId) {
        return new DestinoAdicional(null, e, nome, t, bd(pct), metaId);
    }

    private static DestinoAdicional decimo(String nome, TipoDestino t, String pct) {
        return d(EventoAdicional.DECIMO, nome, t, pct, null);
    }

    private static List<BigDecimal> valores(AdicionalMes a) {
        return a.destinos().stream().map(DestinoValor::valor).toList();
    }

    private static PerfilCalculo perfil() {
        return new PerfilCalculo(228, 0, 1, true, false, Saude.DEP, 2, bd("6"), true, true, bd("3200"),
                new Ferias(false, 7, false));
    }

    private static ParametrosMes pm(YearMonth m) {
        return new ParametrosMes(MotorCalculoTest.P, MotorCalculoTest.TECNICO, bd("9000"));
    }

    private static List<MesProjecao> projetar(List<ParcelaDecimo> decimo, List<ParcelaPlr> plr,
                                              List<MetaCalculo> metas, List<DestinoAdicional> destinos) {
        GastoCalculo aluguel = new GastoCalculo(TipoLancamento.RECORRENTE, bd("2500"), YearMonth.of(2026, 1), 0);
        return ProjetorMensal.projetar(new DadosProjecao(perfil(), List.of(aluguel), decimo, plr, metas, destinos),
                OUT_26, PlanoAdicionaisTest::pm);
    }

    // ---- divisão -------------------------------------------------------------------------------

    @Test
    void divideOLiquidoPelosPercentuaisEORestoFicaSemDestino() {
        AdicionalMes a = CalculadoraDestinos.dividir(EventoAdicional.DECIMO, bd("5000"), List.of(
                decimo("Viagem", TipoDestino.LAZER, "60"), decimo("Reserva", TipoDestino.RESERVA, "30")));
        assertThat(valores(a)).containsExactly(bd("3000.00"), bd("1500.00"));
        assertThat(a.semDestino()).isEqualByComparingTo("500.00");
        assertThat(a.liquido()).isEqualByComparingTo("5000.00");
        assertThat(a.total(TipoDestino.LAZER)).isEqualByComparingTo("3000.00");
        assertThat(a.destinos().get(0).nome()).isEqualTo("Viagem");
        assertThat(a.destinos().get(0).percentual()).isEqualByComparingTo("60");
    }

    @Test
    void residuoDeCentavosVaiParaOUltimoEFechaEm100() {
        AdicionalMes a = CalculadoraDestinos.dividir(EventoAdicional.DECIMO, bd("1000.01"), List.of(
                decimo("A", TipoDestino.LAZER, "33.33"), decimo("B", TipoDestino.OBRIGATORIO, "33.33"),
                decimo("C", TipoDestino.RESERVA, "33.34")));
        // 1000,01 × 33,33% = 333,303 → 333,30 (duas vezes); o último fecha: 1000,01 − 666,60 = 333,41
        assertThat(valores(a)).containsExactly(bd("333.30"), bd("333.30"), bd("333.41"));
        assertThat(a.semDestino()).isEqualByComparingTo("0");
    }

    @Test
    void residuoFechaNoTotalArredondadoQuandoSomaMenorQue100() {
        AdicionalMes a = CalculadoraDestinos.dividir(EventoAdicional.PLR, bd("100.01"), List.of(
                d(EventoAdicional.PLR, "A", TipoDestino.LAZER, "33.33", null),
                d(EventoAdicional.PLR, "B", TipoDestino.LAZER, "33.33", null)));
        // alvo = 100,01 × 66,66% = 66,666… → 66,67; A = 33,33; B = 66,67 − 33,33 = 33,34
        assertThat(valores(a)).containsExactly(bd("33.33"), bd("33.34"));
        assertThat(a.semDestino()).isEqualByComparingTo("33.34");
    }

    @Test
    void semDestinosTudoFicaSemDestinoEOutrosEventosSaoIgnorados() {
        AdicionalMes a = CalculadoraDestinos.dividir(EventoAdicional.PLR, bd("2500.50"),
                List.of(decimo("Viagem", TipoDestino.LAZER, "100")));
        assertThat(a.evento()).isEqualTo(EventoAdicional.PLR);
        assertThat(a.destinos()).isEmpty();
        assertThat(a.semDestino()).isEqualByComparingTo("2500.50");
    }

    @Test
    void liquidoZeroOuNegativoNaoEDividido() {
        AdicionalMes a = CalculadoraDestinos.dividir(EventoAdicional.DECIMO, bd("-10"),
                List.of(decimo("Viagem", TipoDestino.LAZER, "100")));
        assertThat(valores(a)).containsExactly(bd("0.00"));
        assertThat(a.semDestino()).isEqualByComparingTo("-10");
    }

    @Test
    void poucosCentavosNuncaGeramDestinoNegativo() {
        List<DestinoAdicional> dez = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> decimo("D" + i, TipoDestino.LAZER, "10")).toList();
        AdicionalMes a = CalculadoraDestinos.dividir(EventoAdicional.DECIMO, bd("0.05"), dez);
        assertThat(valores(a)).allSatisfy(v -> assertThat(v.signum()).isGreaterThanOrEqualTo(0));
        assertThat(valores(a).stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("0.05");
        assertThat(a.semDestino()).isEqualByComparingTo("0");
    }

    // ---- projeção ------------------------------------------------------------------------------

    @Test
    void decimoComDestinosViraGastoEReservaNoMesDaParcela() {
        List<DestinoAdicional> plano = List.of(decimo("Viagem", TipoDestino.LAZER, "50"),
                decimo("IPVA", TipoDestino.OBRIGATORIO, "20"), decimo("Reserva", TipoDestino.RESERVA, "20"));
        List<MesProjecao> sem = projetar(DECIMO, List.of(), List.of(), List.of());
        List<MesProjecao> com = projetar(DECIMO, List.of(), List.of(), plano);

        MesProjecao out = com.get(0);
        assertThat(out.adicionais()).isEmpty();
        assertThat(out.gastosAdicionais()).isEqualByComparingTo("0");
        assertThat(out.sobra()).isEqualByComparingTo(sem.get(0).sobra());

        MesProjecao nov = com.get(1); // parcela de novembro, com o desconto do 13º
        assertThat(nov.adicionais()).hasSize(1);
        AdicionalMes a = nov.adicionais().get(0);
        assertThat(a.evento()).isEqualTo(EventoAdicional.DECIMO);
        assertThat(a.liquido()).isEqualByComparingTo(nov.decimo().subtract(nov.descontoDecimo()));
        BigDecimal viagem = a.destinos().get(0).valor();
        BigDecimal ipva = a.destinos().get(1).valor();
        BigDecimal reserva = a.destinos().get(2).valor();
        assertThat(viagem).isEqualByComparingTo(a.liquido().multiply(bd("0.5")).setScale(2, java.math.RoundingMode.HALF_UP));
        assertThat(nov.gastosAdicionais()).isEqualByComparingTo(viagem.add(ipva));
        assertThat(nov.reservasAdicionais()).isEqualByComparingTo(reserva);
        assertThat(a.semDestino()).isEqualByComparingTo(a.liquido().subtract(viagem).subtract(ipva).subtract(reserva));
        assertThat(nov.sobra()).isEqualByComparingTo(nov.liquido().subtract(nov.gastos()).subtract(nov.aportes())
                .subtract(nov.gastosAdicionais()).subtract(nov.reservasAdicionais()));
        // reserva sem meta: guardado da reserva dos adicionais, que segue nos meses seguintes
        assertThat(nov.guardadoReservaAdicionais()).isEqualByComparingTo(reserva);
        assertThat(nov.guardado()).isEqualByComparingTo(reserva);
        assertThat(com.get(2).guardadoReservaAdicionais()).isEqualByComparingTo(reserva);

        // fevereiro (sem desconto): segunda parcela também dividida; a reserva acumula
        MesProjecao fev = com.get(4);
        assertThat(fev.adicionais().get(0).liquido()).isEqualByComparingTo(fev.decimo());
        BigDecimal reservaTotal = reserva.add(fev.reservasAdicionais());
        assertThat(com.get(11).guardadoReservaAdicionais()).isEqualByComparingTo(reservaTotal);

        // o patrimônio só perde o que foi gasto (a reserva continua sendo da pessoa)
        BigDecimal gastosAd = com.stream().map(MesProjecao::gastosAdicionais).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sem.get(11).patrimonio().subtract(com.get(11).patrimonio())).isEqualByComparingTo(gastosAd);
        // FUNCEF: patrimonioTotal = patrimonio + previdencia (a reserva dos adicionais já está em guardado/patrimonio)
        assertThat(com.get(11).patrimonioTotal())
                .isEqualByComparingTo(com.get(11).patrimonio().add(com.get(11).previdencia()));
        assertThat(sem.get(11).patrimonioTotal().subtract(com.get(11).patrimonioTotal())).isEqualByComparingTo(gastosAd);
        assertThat(sem.get(11).acumulado().subtract(com.get(11).acumulado()))
                .isEqualByComparingTo(gastosAd.add(reservaTotal));
    }

    @Test
    void eventoInativoNaoGeraNada() {
        List<DestinoAdicional> plano = List.of(decimo("Viagem", TipoDestino.LAZER, "100"),
                d(EventoAdicional.PLR, "Viagem", TipoDestino.LAZER, "100", null));
        List<MesProjecao> sem = projetar(List.of(), List.of(), List.of(), List.of());
        List<MesProjecao> com = projetar(List.of(), List.of(), List.of(), plano);
        for (int i = 0; i < 12; i++) {
            assertThat(com.get(i).adicionais()).isEmpty();
            assertThat(com.get(i).gastosAdicionais()).isEqualByComparingTo("0");
            assertThat(com.get(i).reservasAdicionais()).isEqualByComparingTo("0");
            assertThat(com.get(i).sobra()).isEqualByComparingTo(sem.get(i).sobra());
        }
    }

    @Test
    void semDestinosAProjecaoNaoMuda() {
        List<MesProjecao> m = projetar(DECIMO, List.of(), List.of(), List.of());
        MesProjecao nov = m.get(1);
        assertThat(nov.adicionais()).hasSize(1);
        assertThat(nov.adicionais().get(0).semDestino()).isEqualByComparingTo(nov.adicionais().get(0).liquido());
        assertThat(nov.sobra()).isEqualByComparingTo(nov.liquido().subtract(nov.gastos()));
        assertThat(nov.guardado()).isEqualByComparingTo("0");
    }

    @Test
    void reservaLigadaAMetaAntecipaOAlvoEOAporteMensalPara() {
        // sem extra: 1000 + 6 × 1500 → atinge em mar/27. PLR de 3.000 (isento) em novembro, 100% para a meta.
        MetaCalculo viagem = new MetaCalculo(7L, "Viagem", bd("10000"), bd("1000"), bd("1500"));
        List<ParcelaPlr> plr = List.of(new ParcelaPlr(11, bd("50"), bd("3000")));
        List<DestinoAdicional> plano = List.of(d(EventoAdicional.PLR, "Viagem", TipoDestino.RESERVA, "100", 7L));
        List<MesProjecao> m = projetar(List.of(), plr, List.of(viagem), plano);

        MesProjecao nov = m.get(1);
        assertThat(nov.plr()).isEqualByComparingTo("3000.00");
        assertThat(nov.reservasAdicionais()).isEqualByComparingTo("3000.00");
        assertThat(nov.metas().get(0).aporteAdicional()).isEqualByComparingTo("3000.00");
        assertThat(nov.metas().get(0).aporte()).isEqualByComparingTo("1500.00"); // o extra entra antes do mensal
        assertThat(nov.metas().get(0).guardado()).isEqualByComparingTo("7000.00"); // 2500 + 3000 + 1500
        assertThat(nov.guardadoReservaAdicionais()).isEqualByComparingTo("0");
        assertThat(nov.aportes()).isEqualByComparingTo("1500.00");
        assertThat(m.get(2).metas().get(0).guardado()).isEqualByComparingTo("8500.00");
        assertThat(m.get(3).metas().get(0).guardado()).isEqualByComparingTo("10000.00"); // jan/27: atingida
        assertThat(m.get(4).aportes()).isEqualByComparingTo("0"); // fev/27 em diante: sem aporte mensal
        assertThat(m.get(11).metas().get(0).guardado()).isEqualByComparingTo("10000.00");
        assertThat(m.get(0).metas().get(0).aporteAdicional()).isEqualByComparingTo("0");
    }

    @Test
    void extraQueCobreOAlvoZeraOAporteDoMes() {
        MetaCalculo viagem = new MetaCalculo(7L, "Viagem", bd("4000"), bd("1000"), bd("1500"));
        List<ParcelaPlr> plr = List.of(new ParcelaPlr(10, bd("50"), bd("5000")));
        List<DestinoAdicional> plano = List.of(d(EventoAdicional.PLR, "Viagem", TipoDestino.RESERVA, "100", 7L));
        MesProjecao out = projetar(List.of(), plr, List.of(viagem), plano).get(0);
        assertThat(out.metas().get(0).aporte()).isEqualByComparingTo("0");
        assertThat(out.metas().get(0).guardado()).isEqualByComparingTo("6000.00"); // o extra não é limitado ao alvo
    }

    @Test
    void reservaComMetaInexistenteVaiParaAReservaDosAdicionais() {
        List<ParcelaPlr> plr = List.of(new ParcelaPlr(10, bd("50"), bd("2000")));
        List<DestinoAdicional> plano = List.of(d(EventoAdicional.PLR, "X", TipoDestino.RESERVA, "50", 99L));
        MesProjecao out = projetar(List.of(), plr, List.of(), plano).get(0);
        assertThat(out.reservasAdicionais()).isEqualByComparingTo("1000.00");
        assertThat(out.guardadoReservaAdicionais()).isEqualByComparingTo("1000.00");
        assertThat(out.guardado()).isEqualByComparingTo("1000.00");
    }

    @Test
    void mesComumNaoTemAdicionais() {
        MesProjecao m = ProjetorMensal.calcularMes(new DadosProjecao(perfil(), List.of(), DECIMO, List.of(), List.of(),
                List.of(decimo("Viagem", TipoDestino.LAZER, "100"))), YearMonth.of(2026, 11), 0, true,
                PlanoAdicionaisTest::pm);
        assertThat(m.adicionais()).isEmpty();
        assertThat(m.gastosAdicionais()).isEqualByComparingTo("0");
    }

    @Test
    void totaisPorTipoEPorEvento() {
        List<AdicionalMes> parcelas = List.of(
                CalculadoraDestinos.dividir(EventoAdicional.DECIMO, bd("4000"), List.of(
                        decimo("Viagem", TipoDestino.LAZER, "60"), decimo("Reserva", TipoDestino.RESERVA, "20"))),
                CalculadoraDestinos.dividir(EventoAdicional.PLR, bd("6000"), List.of()));
        TotaisAdicionais t = TotaisAdicionais.somar(parcelas);
        assertThat(t.liquido()).isEqualByComparingTo("10000.00");
        assertThat(t.lazer()).isEqualByComparingTo("2400.00");
        assertThat(t.obrigatorio()).isEqualByComparingTo("0");
        assertThat(t.reserva()).isEqualByComparingTo("800.00");
        assertThat(t.semDestino()).isEqualByComparingTo("6800.00");
        assertThat(TotaisAdicionais.somar(parcelas, EventoAdicional.PLR).semDestino()).isEqualByComparingTo("6000.00");
    }

    // ---- saúde financeira ----------------------------------------------------------------------

    @Test
    void saudeSemAdicionaisNoPeriodo() {
        SaudeAdicionais.Resultado r = SaudeAdicionais.calcular(List.of());
        assertThat(r.totalLiquidoPeriodo()).isEqualByComparingTo("0");
        assertThat(r.percentuais()).isNull();
        assertThat(r.comparacao()).isNull();
        assertThat(r.diagnostico()).containsExactly("Não há 13º nem PLR na projeção dos próximos 12 meses.");
    }

    @Test
    void saudeSemDestinosDefinidos() {
        SaudeAdicionais.Resultado r = SaudeAdicionais.calcular(List.of(
                CalculadoraDestinos.dividir(EventoAdicional.DECIMO, bd("4000"), List.of()),
                CalculadoraDestinos.dividir(EventoAdicional.PLR, bd("6000"), List.of())));
        assertThat(r.definido()).isFalse();
        assertThat(r.divisao().semDestino()).isEqualByComparingTo("10000.00");
        assertThat(r.percentuais().semDestino()).isEqualByComparingTo("100.0");
        assertThat(r.diagnostico()).containsExactly("Você ainda não definiu como usar seu 13º e PLR.");
    }

    @Test
    void saudeComparaOsAdicionaisCom503020() {
        SaudeAdicionais.Resultado r = SaudeAdicionais.calcular(List.of(
                CalculadoraDestinos.dividir(EventoAdicional.DECIMO, bd("4000"), List.of(
                        decimo("Viagem", TipoDestino.LAZER, "60"), decimo("Reserva", TipoDestino.RESERVA, "20"))),
                CalculadoraDestinos.dividir(EventoAdicional.PLR, bd("6000"), List.of())));
        assertThat(r.totalLiquidoPeriodo()).isEqualByComparingTo("10000.00");
        assertThat(r.divisao().desejos()).isEqualByComparingTo("2400.00");
        assertThat(r.divisao().poupanca()).isEqualByComparingTo("800.00");
        assertThat(r.percentuais().desejos()).isEqualByComparingTo("24.0");
        assertThat(r.percentuais().poupanca()).isEqualByComparingTo("8.0");
        assertThat(r.percentuais().semDestino()).isEqualByComparingTo("68.0");
        assertThat(r.comparacao().recomendado().poupanca()).isEqualByComparingTo("2000.00");
        assertThat(r.comparacao().status().necessidades()).isEqualTo(StatusFaixa.OK);
        assertThat(r.comparacao().status().desejos()).isEqualTo(StatusFaixa.OK);
        assertThat(r.comparacao().status().poupanca()).isEqualTo(StatusFaixa.ABAIXO);
        assertThat(r.diagnostico()).containsExactly(
                "Dos seus adicionais nos próximos 12 meses (13º e PLR), 24% vão para lazer e 8% para reservas; "
                        + "68% ficam sem destino definido, no saldo disponível.",
                "Pelo método 50/30/20, o ideal seria guardar pelo menos 20% deles (R$ 2.000,00); "
                        + "hoje vão R$ 800,00 para reservas.",
                "Você ainda não definiu como usar seu PLR.");
    }

    @Test
    void saudeComLazerAcimaEPoupancaOk() {
        SaudeAdicionais.Resultado r = SaudeAdicionais.calcular(List.of(
                CalculadoraDestinos.dividir(EventoAdicional.DECIMO, bd("10000"), List.of(
                        decimo("Viagem", TipoDestino.LAZER, "60"), decimo("Reserva", TipoDestino.RESERVA, "40")))));
        assertThat(r.comparacao().status().desejos()).isEqualTo(StatusFaixa.ACIMA);
        assertThat(r.comparacao().status().poupanca()).isEqualTo(StatusFaixa.OK);
        assertThat(r.comparacao().diferenca().desejos()).isEqualByComparingTo("3000.00");
        assertThat(r.diagnostico()).containsExactly(
                "Dos seus adicionais nos próximos 12 meses (13º), 60% vão para lazer e 40% para reservas.",
                "Você guarda pelo menos 20% dos seus adicionais, como sugere o método 50/30/20.");
    }
}
