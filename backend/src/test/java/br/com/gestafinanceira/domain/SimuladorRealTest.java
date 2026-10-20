package br.com.gestafinanceira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gestafinanceira.domain.calculo.CargoVigente;
import br.com.gestafinanceira.domain.calculo.Parametros;
import br.com.gestafinanceira.domain.calculo.ParametrosMes;
import br.com.gestafinanceira.domain.calculo.descontos.Sindicato;
import br.com.gestafinanceira.domain.calculo.descontos.ValeTransporte;
import br.com.gestafinanceira.domain.calculo.inss.CalculadoraInss;
import br.com.gestafinanceira.domain.calculo.irpf.CalculadoraIrpf;
import br.com.gestafinanceira.domain.calculo.projecao.DadosProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.domain.calculo.remuneracao.RemuneracaoBase;
import br.com.gestafinanceira.domain.calculo.remuneracao.Sobreaviso;
import br.com.gestafinanceira.domain.modelo.Bonus;
import br.com.gestafinanceira.domain.modelo.Ferias;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import br.com.gestafinanceira.domain.modelo.Saude;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reproduz os números reais do Simulador de Gastos / Tabela Salarial 2026 para cargos reais, além de VT, PLR e bônus. */
class SimuladorRealTest {
    private static final Parametros P = MotorCalculoTest.P;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    // cargo, jornada, piso, gratificação, PLR anual (valores da planilha)
    static final CargoVigente TECNICO = new CargoVigente("Técnico Bancário Novo", 6, true, bd("4351.71"), bd("0"), bd("13055.13"));
    static final CargoVigente JUNIOR = new CargoVigente("Assistente Júnior", 6, true, bd("8940.83"), bd("3000.49"), bd("23007.22"));
    static final CargoVigente SUPERVISOR = new CargoVigente("Supervisor de TI", 8, true, bd("18553.08"), bd("8047.68"), bd("30379.98"));

    private static PerfilCalculo perfil(int delta, int sobreaviso, String funcef, boolean vt, boolean vrVa, int filhos,
                                        boolean homeOffice, Bonus bonus) {
        return new PerfilCalculo(delta, sobreaviso, filhos, homeOffice, vrVa, Saude.NAO, 0, bd(funcef), vt, false,
                bd("0"), new Ferias(false, 7, false), bonus);
    }

    // ---- cálculo por cargo real ---------------------------------------------------------------

    @Test
    void assistenteJuniorBaseDoPisoComInssNoTetoEIrpfDaPlanilha() {
        // delta 201 (4351,71) + gratificação 3000,49 = 7352,20 < piso 8940,83 → vale o piso
        BigDecimal base = RemuneracaoBase.calcular(JUNIOR, bd("4351.71"));
        assertThat(base).isEqualByComparingTo("8940.83");
        BigDecimal inss = CalculadoraInss.calcular(base, P);
        assertThat(inss).isEqualByComparingTo("988.09");
        assertThat(CalculadoraIrpf.calcular(base, inss, bd("0"), P).imposto()).isEqualByComparingTo("1278.27");
        BigDecimal funcef12 = base.multiply(bd("0.12")).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(CalculadoraIrpf.calcular(base, inss, funcef12, P).imposto()).isEqualByComparingTo("983.23");
    }

    @Test
    void tecnicoBancarioBaseDe4351InssPorFaixaEIrpfZero() {
        BigDecimal base = RemuneracaoBase.calcular(TECNICO, bd("4351.71")); // delta 201 = piso
        assertThat(base).isEqualByComparingTo("4351.71");
        BigDecimal inss = CalculadoraInss.calcular(base, P);
        assertThat(inss).isEqualByComparingTo("410.81"); // 4351,71 × 12% − 111,40
        assertThat(CalculadoraIrpf.calcular(base, inss, bd("0"), P).imposto()).isEqualByComparingTo("0");
    }

    @Test
    void supervisorDeTiInssNoTetoEIrpfDaPlanilha() {
        // delta 0 + gratificação 8047,68 < piso 18553,08 → vale o piso
        BigDecimal base = RemuneracaoBase.calcular(SUPERVISOR, bd("0"));
        assertThat(base).isEqualByComparingTo("18553.08");
        BigDecimal inss = CalculadoraInss.calcular(base, P);
        assertThat(inss).isEqualByComparingTo("988.09");
        assertThat(CalculadoraIrpf.calcular(base, inss, bd("0"), P).imposto()).isEqualByComparingTo("3921.64");
        BigDecimal funcef12 = base.multiply(bd("0.12")).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(CalculadoraIrpf.calcular(base, inss, funcef12, P).imposto()).isEqualByComparingTo("3309.39");
    }

    @Test
    void baseUsaMaxEntrePisoEDeltaMaisGratificacaoParaTodosOsCargos() {
        // delta alto vence o piso: 12704,78 + 8047,68 = 20752,46 > 18553,08
        assertThat(RemuneracaoBase.calcular(SUPERVISOR, bd("12704.78"))).isEqualByComparingTo("20752.46");
        assertThat(RemuneracaoBase.calcular(JUNIOR, bd("8035.13"))).isEqualByComparingTo("11035.62");
    }

    @Test
    void sobreavisoUsaDivisorDaJornadaDoCargo() {
        // 6h: ÷180; 8h: ÷220
        assertThat(Sobreaviso.calcular(bd("8940.83"), 2, JUNIOR.jornada(), P)).isEqualByComparingTo("794.74");
        assertThat(Sobreaviso.calcular(bd("18553.08"), 2, SUPERVISOR.jornada(), P)).isEqualByComparingTo("1349.31");
    }

    @Test
    void inssESindicatoIncidemSobreBasePlusSobreavisoEVrVaNaoEntram() {
        ParametrosMes pm = new ParametrosMes(P, TECNICO, bd("4351.71"));
        MesProjecao sem = mes(pm, perfil(201, 3, "0", false, false, 0, false, Bonus.INATIVO), List.of(), List.of());
        MesProjecao com = mes(pm, perfil(201, 3, "0", false, true, 2, true, Bonus.INATIVO), List.of(), List.of());
        // base 4351,71 + sobreaviso 4351,71×3×8/180 = 580,23 → 4931,94 (faixa de 14%: 690,47 − 198,49)
        assertThat(sem.sobreaviso()).isEqualByComparingTo("580.23");
        assertThat(sem.inss()).isEqualByComparingTo("491.98");
        assertThat(sem.baseInss()).isEqualByComparingTo("4931.94");
        assertThat(Sindicato.calcular(true, sem.baseInss(), P)).isEqualByComparingTo("49.32"); // 1% do bruto
        // VR/VA, creche e home office aumentam receitas mas não o INSS nem o IRPF
        assertThat(com.receitas()).isGreaterThan(sem.receitas());
        assertThat(com.inss()).isEqualByComparingTo(sem.inss());
        assertThat(com.irpf().imposto()).isEqualByComparingTo(sem.irpf().imposto());
    }

    // ---- VT -----------------------------------------------------------------------------------

    @Test
    void valeTransporteEQuatroPorCentoDoValorDoDelta() {
        assertThat(ValeTransporte.calcular(true, bd("8035.13"), P)).isEqualByComparingTo("321.41");
        assertThat(ValeTransporte.calcular(false, bd("8035.13"), P)).isEqualByComparingTo("0");
        ParametrosMes pm = new ParametrosMes(P, TECNICO, bd("4351.71"));
        MesProjecao m = mes(pm, perfil(201, 0, "0", true, false, 0, false, Bonus.INATIVO), List.of(), List.of());
        assertThat(m.valeTransporte()).isEqualByComparingTo("174.07"); // 4% × 4351,71
    }

    // ---- PLR ----------------------------------------------------------------------------------

    @Test
    void plrEntraComoReceitaLiquidaNoMesDaParcelaSemAlterarOInss() {
        ParametrosMes pm = new ParametrosMes(P, TECNICO, bd("4351.71"));
        PerfilCalculo pf = perfil(201, 0, "0", false, false, 0, false, Bonus.INATIVO);
        List<ParcelaDecimo> plr = List.of(new ParcelaDecimo(3, bd("40")), new ParcelaDecimo(9, bd("60")));
        MesProjecao comum = mes(pm, pf, List.of(), List.of());
        MesProjecao marco = ProjetorMensal.calcularMes(new DadosProjecao(pf, List.of(), List.of(), plr),
                YearMonth.of(2027, 3), 0, false, f -> pm);
        MesProjecao junho = ProjetorMensal.calcularMes(new DadosProjecao(pf, List.of(), List.of(), plr),
                YearMonth.of(2027, 6), 0, false, f -> pm);
        assertThat(marco.plr()).isEqualByComparingTo("5222.05"); // 13055,13 × 40%
        assertThat(marco.evento()).isEqualTo("PLR");
        assertThat(marco.receitas()).isEqualByComparingTo(comum.receitas().add(bd("5222.05")));
        assertThat(marco.inss()).isEqualByComparingTo(comum.inss());
        assertThat(marco.irpf().imposto()).isEqualByComparingTo(comum.irpf().imposto());
        assertThat(marco.descontos()).isEqualByComparingTo(comum.descontos());
        assertThat(junho.plr()).isEqualByComparingTo("0");
        assertThat(junho.evento()).isEmpty();
        // mês comum ignora eventos
        assertThat(ProjetorMensal.calcularMes(new DadosProjecao(pf, List.of(), List.of(), plr),
                YearMonth.of(2027, 3), 0, true, f -> pm).plr()).isEqualByComparingTo("0");
    }

    // ---- bônus Caixa --------------------------------------------------------------------------

    @Test
    void bonusEBaseVezesSalariosLiquidoNoMesConfigurado() {
        ParametrosMes pm = new ParametrosMes(P, SUPERVISOR, bd("0"));
        PerfilCalculo pf = perfil(201, 0, "0", false, false, 0, false, new Bonus(true, 12, bd("2.5")));
        MesProjecao dez = ProjetorMensal.calcularMes(new DadosProjecao(pf, List.of(), List.of()),
                YearMonth.of(2026, 12), 0, false, f -> pm);
        MesProjecao nov = ProjetorMensal.calcularMes(new DadosProjecao(pf, List.of(), List.of()),
                YearMonth.of(2026, 11), 0, false, f -> pm);
        assertThat(dez.bonus()).isEqualByComparingTo("46382.70"); // 18553,08 × 2,5
        assertThat(dez.evento()).isEqualTo("Bônus");
        assertThat(dez.receitas()).isEqualByComparingTo(nov.receitas().add(bd("46382.70")));
        assertThat(dez.inss()).isEqualByComparingTo(nov.inss());
        assertThat(dez.irpf().imposto()).isEqualByComparingTo(nov.irpf().imposto());
        assertThat(nov.bonus()).isEqualByComparingTo("0");
    }

    @Test
    void motorIgnoraBonusEmCargoDeSeisHoras() {
        ParametrosMes pm = new ParametrosMes(P, JUNIOR, bd("201"));
        PerfilCalculo pf = perfil(201, 0, "0", false, false, 0, false, new Bonus(true, 12, bd("3")));
        MesProjecao dez = ProjetorMensal.calcularMes(new DadosProjecao(pf, List.of(), List.of()),
                YearMonth.of(2026, 12), 0, false, f -> pm);
        assertThat(dez.bonus()).isEqualByComparingTo("0");
    }

    @Test
    void eventosDoMesSaoUnidosPorMaisEInssNaoMuda() {
        ParametrosMes pm = new ParametrosMes(P, SUPERVISOR, bd("0"));
        PerfilCalculo pf = new PerfilCalculo(201, 0, 1, true, true, Saude.NAO, 0, bd("5"), true, true, bd("0"),
                new Ferias(true, 12, false), new Bonus(true, 12, bd("1")));
        DadosProjecao dados = new DadosProjecao(pf, List.of(), List.of(new ParcelaDecimo(12, bd("100"))),
                List.of(new ParcelaDecimo(12, bd("100"))));
        MesProjecao dez = ProjetorMensal.calcularMes(dados, YearMonth.of(2026, 12), 0, false, f -> pm);
        assertThat(dez.evento()).isEqualTo("13º + Férias + PLR + Bônus");
        MesProjecao comum = ProjetorMensal.calcularMes(dados, YearMonth.of(2026, 12), 0, true, f -> pm);
        assertThat(dez.inss()).isEqualByComparingTo(comum.inss());
        assertThat(dez.baseInss()).isEqualByComparingTo(comum.base().add(comum.sobreaviso()));
    }

    private static MesProjecao mes(ParametrosMes pm, PerfilCalculo pf, List<ParcelaDecimo> decimo, List<ParcelaDecimo> plr) {
        return ProjetorMensal.calcularMes(new DadosProjecao(pf, List.of(), decimo, plr), YearMonth.of(2027, 1), 0,
                false, f -> pm);
    }
}
