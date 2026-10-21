package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.DestaqueMesDto;
import br.com.gestafinanceira.api.dto.Dtos.ProjecaoDto;
import br.com.gestafinanceira.api.dto.Dtos.ResumoProjecaoDto;
import br.com.gestafinanceira.domain.calculo.FonteParametros;
import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.projecao.DadosProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.domain.modelo.Bonus;
import br.com.gestafinanceira.domain.modelo.Ferias;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import br.com.gestafinanceira.infra.persistencia.Entidades.Perfil;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta os dados persistidos e chama o motor de cálculo. A projeção nunca é persistida. */
@Service
@Transactional
public class ProjecaoService {
    private final PerfilService perfil;
    private final LancamentoService lancamentos;
    private final DecimoService decimo;
    private final PlrService plr;
    private final ParametrosService parametros;
    private final MetaService metas;
    private final Clock clock;

    public ProjecaoService(PerfilService perfil, LancamentoService lancamentos, DecimoService decimo,
                           PlrService plr, ParametrosService parametros, MetaService metas, Clock clock) {
        this.metas = metas;
        this.clock = clock;
        this.plr = plr;
        this.perfil = perfil;
        this.lancamentos = lancamentos;
        this.decimo = decimo;
        this.parametros = parametros;
    }

    /** Projeção de 12 meses; sem início informado, usa o mês atual. */
    public ProjecaoDto projetar(YearMonth inicio) {
        YearMonth ini = inicio != null ? inicio : YearMonth.now(clock);
        Perfil p = perfil.entidade();
        DadosProjecao dados = dados(p);
        List<MesProjecao> meses = ProjetorMensal.projetar(dados, ini, fonte(p));

        Function<Function<MesProjecao, BigDecimal>, BigDecimal> soma =
                f -> meses.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
        MesProjecao ultimo = meses.get(meses.size() - 1);
        BigDecimal guardadoInicial = Dinheiro.arredondar(dados.metas().stream().map(MetaCalculo::valorAtual)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        ResumoProjecaoDto resumo = new ResumoProjecaoDto(
                Dinheiro.arredondar(soma.apply(MesProjecao::receitas)),
                Dinheiro.arredondar(soma.apply(MesProjecao::descontos)),
                Dinheiro.arredondar(soma.apply(MesProjecao::liquido)),
                Dinheiro.arredondar(soma.apply(MesProjecao::gastos)),
                Dinheiro.arredondar(soma.apply(MesProjecao::sobra)),
                ultimo.acumulado(),
                Dinheiro.arredondar(ultimo.acumulado().subtract(p.saldoInicial)),
                media(soma.apply(MesProjecao::sobra)), media(soma.apply(MesProjecao::liquido)),
                media(soma.apply(MesProjecao::gastos)),
                destaque(meses.stream().max(Comparator.comparing(MesProjecao::sobra)).orElseThrow()),
                destaque(meses.stream().min(Comparator.comparing(MesProjecao::sobra)).orElseThrow()),
                meses.stream().filter(m -> m.sobra().signum() < 0).map(MesProjecao::competencia).toList(),
                meses.stream().map(m -> m.sobra().abs()).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO),
                Dinheiro.arredondar(soma.apply(MesProjecao::aportes)), guardadoInicial, ultimo.guardado(),
                ultimo.patrimonio());
        return new ProjecaoDto(ini, p.saldoInicial, meses, resumo);
    }

    /** Mês "comum" (sem 13º e férias) do mês inicial, usado como referência na tela de detalhe. */
    public MesProjecao mesComum(YearMonth mes) {
        Perfil p = perfil.entidade();
        YearMonth m = mes != null ? mes : YearMonth.now(clock);
        return ProjetorMensal.calcularMes(dados(p), m, 0, true, fonte(p));
    }

    private static BigDecimal media(BigDecimal total) {
        return Dinheiro.arredondar(total.divide(BigDecimal.valueOf(ProjetorMensal.MESES), 10, java.math.RoundingMode.HALF_UP));
    }

    private static DestaqueMesDto destaque(MesProjecao m) {
        return new DestaqueMesDto(m.competencia(), m.sobra());
    }

    private DadosProjecao dados(Perfil p) {
        PerfilCalculo pc = new PerfilCalculo(p.delta, p.diasSobreaviso, p.filhos, p.homeOffice, p.vrVa, p.saude,
                p.dependentes, p.funcefPct, p.valeTransporte, p.sindicato, p.saldoInicial,
                new Ferias(p.feriasAtivo, p.feriasMes, p.feriasAbono),
                new Bonus(p.bonusAtivo, p.bonusMes, p.bonusSalarios));
        return new DadosProjecao(pc, lancamentos.paraCalculo(), decimo.paraCalculo(), plr.paraCalculo(),
                metas.paraCalculo());
    }

    private FonteParametros fonte(Perfil p) {
        return mes -> parametros.paraMes(mes, p.cargoId, p.delta);
    }
}
