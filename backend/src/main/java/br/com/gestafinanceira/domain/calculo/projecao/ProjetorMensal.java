package br.com.gestafinanceira.domain.calculo.projecao;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.FonteParametros;
import br.com.gestafinanceira.domain.calculo.Parametros;
import br.com.gestafinanceira.domain.calculo.ParametrosMes;
import br.com.gestafinanceira.domain.calculo.decimoterceiro.Calculadora13;
import br.com.gestafinanceira.domain.calculo.decimoterceiro.CalculadoraFerias;
import br.com.gestafinanceira.domain.calculo.descontos.Funcef;
import br.com.gestafinanceira.domain.calculo.descontos.SaudeCaixa;
import br.com.gestafinanceira.domain.calculo.descontos.Sindicato;
import br.com.gestafinanceira.domain.calculo.descontos.ValeTransporte;
import br.com.gestafinanceira.domain.calculo.inss.CalculadoraInss;
import br.com.gestafinanceira.domain.calculo.irpf.CalculadoraIrpf;
import br.com.gestafinanceira.domain.calculo.irpf.DetalheIrpf;
import br.com.gestafinanceira.domain.calculo.remuneracao.Beneficios;
import br.com.gestafinanceira.domain.calculo.remuneracao.RemuneracaoBase;
import br.com.gestafinanceira.domain.calculo.remuneracao.Sobreaviso;
import br.com.gestafinanceira.domain.modelo.Bonus;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Orquestra o cálculo dos 12 meses a partir do mês inicial. */
public final class ProjetorMensal {
    public static final int MESES = 12;

    private ProjetorMensal() {}

    public static List<MesProjecao> projetar(DadosProjecao dados, YearMonth inicio, FonteParametros fonte) {
        List<MesProjecao> resultado = new ArrayList<>(MESES);
        BigDecimal acumulado = dados.perfil().saldoInicial();
        for (int i = 0; i < MESES; i++) {
            MesProjecao m = calcularMes(dados, inicio.plusMonths(i), i, false, fonte);
            acumulado = acumulado.add(m.sobra());
            resultado.add(m.comAcumulado(Dinheiro.arredondar(acumulado)));
        }
        return resultado;
    }

    /** Calcula um mês isolado (sem acumulado). {@code semEventos} ignora 13º e férias (mês comum). */
    public static MesProjecao calcularMes(DadosProjecao dados, YearMonth mes, int indice, boolean semEventos,
                                          FonteParametros fonte) {
        PerfilCalculo perfil = dados.perfil();
        ParametrosMes pm = fonte.para(mes);
        Parametros p = pm.parametros();

        BigDecimal base = RemuneracaoBase.calcular(pm.cargo(), pm.valorDelta());
        BigDecimal sobreaviso = Sobreaviso.calcular(base, perfil.diasSobreaviso(), pm.cargo().jornada(), p);
        BigDecimal creche = Beneficios.auxilioCreche(perfil.filhos(), p);
        BigDecimal homeOffice = Beneficios.homeOffice(perfil.homeOffice(), p);
        BigDecimal vrVa = Beneficios.vrVa(perfil.vrVa(), p);

        BigDecimal decimo = Dinheiro.ZERO;
        BigDecimal descontoDecimo = Dinheiro.ZERO;
        BigDecimal ferias = Dinheiro.ZERO;
        BigDecimal plr = Dinheiro.ZERO;
        BigDecimal bonus = Dinheiro.ZERO;
        if (!semEventos) {
            List<ParcelaDecimo> parcelas = dados.decimo();
            Optional<ParcelaDecimo> deste = parcelas.stream().filter(e -> e.mes() == mes.getMonthValue()).findFirst();
            if (deste.isPresent()) {
                decimo = Calculadora13.parcela(base, deste.get());
                int ultimoMes = parcelas.stream().map(ParcelaDecimo::mes).max(Comparator.naturalOrder()).orElse(0);
                if (mes.getMonthValue() == ultimoMes) {
                    descontoDecimo = Calculadora13.descontoNaUltimaParcela(base, parcelas, p,
                            perfil.funcefPct(), perfil.saude(), perfil.dependentes());
                }
            }
            if (perfil.ferias().ativo() && perfil.ferias().mes() == mes.getMonthValue()) {
                ferias = CalculadoraFerias.valor(base, perfil.ferias().abono(), p);
            }
            // PLR e Bônus Caixa entram como valores líquidos (sem INSS/IRPF adicionais).
            Optional<ParcelaDecimo> parcelaPlr = dados.plr().stream()
                    .filter(e -> e.mes() == mes.getMonthValue()).findFirst();
            if (parcelaPlr.isPresent()) {
                plr = Dinheiro.percentual(pm.cargo().plrAnual(), parcelaPlr.get().percentual());
            }
            Bonus b = perfil.bonus();
            if (b.ativo() && pm.cargo().jornada() == 8 && b.mes() == mes.getMonthValue()) {
                bonus = Dinheiro.arredondar(base.multiply(b.salarios()));
            }
        }

        BigDecimal bruta = base.add(sobreaviso);
        BigDecimal inss = CalculadoraInss.calcular(bruta, p);
        BigDecimal funcef = Funcef.calcular(base, perfil.funcefPct());
        DetalheIrpf irpf = CalculadoraIrpf.calcular(bruta, inss, funcef, p);
        BigDecimal saude = SaudeCaixa.calcular(perfil.saude(), perfil.dependentes(), base, p);
        BigDecimal vt = ValeTransporte.calcular(perfil.valeTransporte(), pm.valorDelta(), p);
        BigDecimal sindicato = Sindicato.calcular(perfil.sindicato(), bruta, p);

        BigDecimal receitas = base.add(sobreaviso).add(creche).add(homeOffice).add(vrVa).add(decimo).add(ferias)
                .add(plr).add(bonus);
        BigDecimal descontos = inss.add(irpf.imposto()).add(saude).add(funcef).add(vt).add(sindicato)
                .add(descontoDecimo);
        BigDecimal liquido = receitas.subtract(descontos);
        BigDecimal gastos = dados.gastos().stream().map(g -> g.valorEm(mes)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sobra = liquido.subtract(gastos);

        List<String> eventos = new ArrayList<>();
        if (decimo.signum() != 0) eventos.add("13º");
        if (ferias.signum() != 0) eventos.add("Férias");
        if (plr.signum() != 0) eventos.add("PLR");
        if (bonus.signum() != 0) eventos.add("Bônus");
        String evento = String.join(" + ", eventos);
        return new MesProjecao(indice, mes, base, sobreaviso, creche, homeOffice, vrVa, decimo, ferias, plr, bonus,
                inss, irpf, funcef, saude, vt, sindicato, descontoDecimo,
                Dinheiro.arredondar(receitas), Dinheiro.arredondar(descontos), Dinheiro.arredondar(liquido),
                Dinheiro.arredondar(gastos), Dinheiro.arredondar(sobra), Dinheiro.ZERO, evento,
                bruta, bruta.compareTo(p.inssTetoSalario()) > 0);
    }
}
