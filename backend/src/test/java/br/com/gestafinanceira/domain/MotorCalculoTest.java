package br.com.gestafinanceira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gestafinanceira.domain.calculo.CargoVigente;
import br.com.gestafinanceira.domain.calculo.FaixaProgressiva;
import br.com.gestafinanceira.domain.calculo.Parametros;
import br.com.gestafinanceira.domain.calculo.ParametrosMes;
import br.com.gestafinanceira.domain.calculo.decimoterceiro.Calculadora13;
import br.com.gestafinanceira.domain.calculo.descontos.SaudeCaixa;
import br.com.gestafinanceira.domain.calculo.descontos.Sindicato;
import br.com.gestafinanceira.domain.calculo.inss.CalculadoraInss;
import br.com.gestafinanceira.domain.calculo.irpf.CalculadoraIrpf;
import br.com.gestafinanceira.domain.calculo.irpf.DetalheIrpf;
import br.com.gestafinanceira.domain.calculo.projecao.DadosProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.domain.calculo.remuneracao.RemuneracaoBase;
import br.com.gestafinanceira.domain.calculo.remuneracao.Sobreaviso;
import br.com.gestafinanceira.domain.modelo.Ferias;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import br.com.gestafinanceira.domain.modelo.Saude;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class MotorCalculoTest {
    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    static final Parametros P = new Parametros(
            bd("729.21"), bd("104.48"), bd("2194.16"), bd("0.04"), bd("0.01"), bd("149.21"),
            bd("0.035"), bd("0.07"), bd("502.08"), bd("988.09"), bd("8475.55"), bd("607.20"),
            bd("5000"), bd("7350"), bd("978.62"), bd("0.133145"), 180, 220, bd("3"),
            List.of(new FaixaProgressiva(bd("1621.00"), bd("0.075"), bd("0")),
                    new FaixaProgressiva(bd("2902.84"), bd("0.09"), bd("24.32")),
                    new FaixaProgressiva(bd("4354.27"), bd("0.12"), bd("111.40")),
                    new FaixaProgressiva(bd("8475.55"), bd("0.14"), bd("198.49"))),
            List.of(new FaixaProgressiva(bd("2428.80"), bd("0"), bd("0")),
                    new FaixaProgressiva(bd("2826.65"), bd("0.075"), bd("182.16")),
                    new FaixaProgressiva(bd("3751.05"), bd("0.15"), bd("394.16")),
                    new FaixaProgressiva(bd("4664.68"), bd("0.225"), bd("675.49")),
                    new FaixaProgressiva(null, bd("0.275"), bd("908.73"))));

    static final CargoVigente TECNICO = new CargoVigente("Técnico Bancário Novo", 6, true, bd("0"), bd("0"));
    static final CargoVigente ASSISTENTE = new CargoVigente("Assistente Júnior", 8, false, bd("6200"), bd("1900"));

    // ---- INSS ---------------------------------------------------------------------------------

    @Test
    void inssAplicaTetoAcimaDoLimiteSalarial() {
        assertThat(CalculadoraInss.calcular(bd("12000"), P)).isEqualByComparingTo("988.09");
    }

    @Test
    void inssUsaAliquotaEParcelaADeduzirDaFaixa() {
        // 3000 × 12% − 111,40 = 248,60
        assertThat(CalculadoraInss.calcular(bd("3000"), P)).isEqualByComparingTo("248.60");
        // primeira faixa: 1500 × 7,5% = 112,50
        assertThat(CalculadoraInss.calcular(bd("1500"), P)).isEqualByComparingTo("112.50");
    }

    // ---- IRPF ---------------------------------------------------------------------------------

    @Test
    void irpfZeraAteCincoMil() {
        DetalheIrpf r = CalculadoraIrpf.calcular(bd("4500"), bd("400"), bd("200"), P);
        assertThat(r.imposto()).isEqualByComparingTo("0");
        assertThat(r.reducao()).isEqualByComparingTo(r.bruto());
    }

    @Test
    void irpfUsaDeducaoMinimaQuandoLegalEMenor() {
        assertThat(CalculadoraIrpf.calcular(bd("8000"), bd("100"), bd("100"), P).deducao()).isEqualByComparingTo("607.20");
    }

    @Test
    void irpfAplicaReducaoEntreCincoMilESeteMilETrezentosECinquenta() {
        DetalheIrpf r = CalculadoraIrpf.calcular(bd("6000"), bd("600"), bd("300"), P);
        // 978,62 − 0,133145 × 6000 = 179,75
        assertThat(r.reducao()).isEqualByComparingTo("179.75");
        // dedução 900 → base 5100 → 27,5% × 5100 − 908,73 = 493,77 → imposto 493,77 − 179,75
        assertThat(r.bruto()).isEqualByComparingTo("493.77");
        assertThat(r.imposto()).isEqualByComparingTo("314.02");
    }

    @Test
    void irpfNaoReduzAcimaDeSeteMilETrezentosECinquenta() {
        DetalheIrpf r = CalculadoraIrpf.calcular(bd("9000"), bd("988.09"), bd("500"), P);
        assertThat(r.reducao()).isEqualByComparingTo("0");
        assertThat(r.imposto()).isEqualByComparingTo(r.bruto());
    }

    // ---- remuneração e descontos --------------------------------------------------------------

    @Test
    void remuneracaoBaseUsaMaiorEntrePisoEDeltaMaisGratificacao() {
        assertThat(RemuneracaoBase.calcular(TECNICO, bd("6000"))).isEqualByComparingTo("6000");
        CargoVigente comPiso = new CargoVigente("X", 6, true, bd("7000"), bd("500"));
        assertThat(RemuneracaoBase.calcular(comPiso, bd("6000"))).isEqualByComparingTo("7000");
        assertThat(RemuneracaoBase.calcular(ASSISTENTE, bd("0"))).isEqualByComparingTo("8100");
    }

    @Test
    void sobreavisoUsaDivisorPorJornada() {
        assertThat(Sobreaviso.calcular(bd("9000"), 3, 6, P)).isEqualByComparingTo("1200.00"); // 9000×3×8/180
        assertThat(Sobreaviso.calcular(bd("8800"), 2, 8, P)).isEqualByComparingTo("640.00"); // 8800×2×8/220
    }

    @Test
    void saudeCaixaRespeitaTetoDeSetePorCento() {
        assertThat(SaudeCaixa.calcular(Saude.SOZINHO, 0, bd("6000"), P)).isEqualByComparingTo("210.00");
        assertThat(SaudeCaixa.calcular(Saude.NAO, 3, bd("6000"), P)).isEqualByComparingTo("0");
        // 2 × 502,08 + 210 = 1214,16 → limitado a 7% × 6000 = 420
        assertThat(SaudeCaixa.calcular(Saude.DEP, 2, bd("6000"), P)).isEqualByComparingTo("420.00");
    }

    @Test
    void sindicatoRespeitaOTeto() {
        assertThat(Sindicato.calcular(true, bd("20000"), P)).isEqualByComparingTo("149.21");
        assertThat(Sindicato.calcular(true, bd("10000"), P)).isEqualByComparingTo("100.00");
        assertThat(Sindicato.calcular(false, bd("10000"), P)).isEqualByComparingTo("0");
    }

    // ---- gastos -------------------------------------------------------------------------------

    @Test
    void gastosRecorrentePontualEParcelado() {
        YearMonth ini = YearMonth.of(2026, 11);
        GastoCalculo rec = new GastoCalculo(TipoLancamento.RECORRENTE, bd("100"), ini, 0);
        GastoCalculo pont = new GastoCalculo(TipoLancamento.PONTUAL, bd("100"), ini, 0);
        GastoCalculo parc = new GastoCalculo(TipoLancamento.PARCELADO, bd("100"), ini, 3);
        assertThat(rec.valorEm(ini.minusMonths(1))).isEqualByComparingTo("0");
        assertThat(rec.valorEm(ini.plusMonths(8))).isEqualByComparingTo("100");
        assertThat(pont.valorEm(ini)).isEqualByComparingTo("100");
        assertThat(pont.valorEm(ini.plusMonths(1))).isEqualByComparingTo("0");
        assertThat(parc.valorEm(ini.plusMonths(2))).isEqualByComparingTo("100");
        assertThat(parc.valorEm(ini.plusMonths(3))).isEqualByComparingTo("0");
    }

    // ---- 13º ----------------------------------------------------------------------------------

    @Test
    void decimoDescontaInssEIrExclusivoNaUltimaParcela() {
        List<ParcelaDecimo> parcelas = List.of(new ParcelaDecimo(2, bd("50")), new ParcelaDecimo(11, bd("50")));
        assertThat(Calculadora13.parcela(bd("6000"), parcelas.get(0))).isEqualByComparingTo("3000.00");
        BigDecimal desconto = Calculadora13.descontoNaUltimaParcela(bd("6000"), parcelas, P);
        // INSS(6000) = 6000×14% − 198,49 = 641,51; dedução 641,51 → base 5358,49 → 27,5% − 908,73 = 564,... (bruto)
        BigDecimal inss = CalculadoraInss.calcular(bd("6000"), P);
        BigDecimal ir = CalculadoraIrpf.calcular(bd("6000"), inss, BigDecimal.ZERO, P).imposto();
        assertThat(inss).isEqualByComparingTo("641.51");
        assertThat(desconto).isEqualByComparingTo(inss.add(ir));
    }

    // ---- projeção -----------------------------------------------------------------------------

    private static PerfilCalculo perfil(Ferias ferias) {
        return new PerfilCalculo(228, 0, 1, true, false, Saude.DEP, 2, bd("6"), true, true, bd("3200"), ferias);
    }

    private static ParametrosMes pm(CargoVigente c, String delta) {
        return new ParametrosMes(P, c, bd(delta));
    }

    @Test
    void projecaoGeraDozeMesesEAcumulaASobraAPartirDoSaldo() {
        DadosProjecao dados = new DadosProjecao(perfil(new Ferias(false, 7, false)), List.of(), List.of());
        List<MesProjecao> meses = ProjetorMensal.projetar(dados, YearMonth.of(2026, 10), m -> pm(TECNICO, "9000"));
        assertThat(meses).hasSize(12);
        assertThat(meses.get(0).competencia()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(meses.get(11).competencia()).isEqualTo(YearMonth.of(2027, 9));
        assertThat(meses.get(0).acumulado())
                .isEqualByComparingTo(bd("3200").add(meses.get(0).sobra()));
        BigDecimal soma = meses.stream().map(MesProjecao::sobra).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(meses.get(11).acumulado()).isEqualByComparingTo(bd("3200").add(soma));
    }

    @Test
    void liquidoEReceitasMenosDescontosESobraELiquidoMenosGastos() {
        GastoCalculo g = new GastoCalculo(TipoLancamento.RECORRENTE, bd("2500"), YearMonth.of(2026, 1), 0);
        DadosProjecao dados = new DadosProjecao(perfil(new Ferias(false, 7, false)), List.of(g), List.of());
        MesProjecao m = ProjetorMensal.calcularMes(dados, YearMonth.of(2026, 10), 0, false, x -> pm(TECNICO, "9000"));
        assertThat(m.liquido()).isEqualByComparingTo(m.receitas().subtract(m.descontos()));
        assertThat(m.sobra()).isEqualByComparingTo(m.liquido().subtract(bd("2500")));
        // receitas: base 9000 + creche 729,21 + home office 104,48
        assertThat(m.receitas()).isEqualByComparingTo("9833.69");
        // INSS no teto, FUNCEF 6% × 9000 = 540, VT 4% × delta (9000) = 360, sindicato 1% × 9000 = 90
        assertThat(m.inss()).isEqualByComparingTo("988.09");
        assertThat(m.funcef()).isEqualByComparingTo("540.00");
        assertThat(m.valeTransporte()).isEqualByComparingTo("360.00");
        assertThat(m.sindicato()).isEqualByComparingTo("90.00");
    }

    @Test
    void eventosDeDecimoEFeriasAparecemNosMesesConfigurados() {
        List<ParcelaDecimo> decimo = List.of(new ParcelaDecimo(2, bd("50")), new ParcelaDecimo(11, bd("50")));
        DadosProjecao dados = new DadosProjecao(perfil(new Ferias(true, 7, false)), List.of(), decimo);
        List<MesProjecao> meses = ProjetorMensal.projetar(dados, YearMonth.of(2026, 10), m -> pm(ASSISTENTE, "0"));
        MesProjecao nov = meses.get(1);
        MesProjecao fev = meses.get(4);
        MesProjecao jul = meses.get(9);
        assertThat(nov.evento()).isEqualTo("13º");
        assertThat(nov.decimo()).isEqualByComparingTo("4050.00");
        assertThat(nov.descontoDecimo()).isGreaterThan(BigDecimal.ZERO); // última parcela do ano
        assertThat(fev.evento()).isEqualTo("13º");
        assertThat(fev.descontoDecimo()).isEqualByComparingTo("0");
        assertThat(jul.evento()).isEqualTo("Férias");
        assertThat(jul.ferias()).isEqualByComparingTo("1904.92"); // 8100: 2700 - dINSS - dIRPF (líquido, planilha)
        assertThat(meses.get(0).evento()).isEmpty();
    }

    @Test
    void decimoInativoNaoGeraValorNemDescontoEmNenhumMes() {
        // 13º inativo: o serviço entrega lista vazia ao motor
        DadosProjecao dados = new DadosProjecao(perfil(new Ferias(false, 7, false)), List.of(), List.of());
        List<MesProjecao> meses = ProjetorMensal.projetar(dados, YearMonth.of(2026, 10), m -> pm(ASSISTENTE, "0"));
        for (MesProjecao m : meses) {
            assertThat(m.decimo()).isEqualByComparingTo("0");
            assertThat(m.descontoDecimo()).isEqualByComparingTo("0");
            assertThat(m.evento()).doesNotContain("13º");
        }
    }
}
