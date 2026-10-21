package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.DestaqueMesDto;
import br.com.gestafinanceira.api.dto.Dtos.PreviaAdicionalDto;
import br.com.gestafinanceira.api.dto.Dtos.ProjecaoDto;
import br.com.gestafinanceira.api.dto.Dtos.ResumoAdicionaisDto;
import br.com.gestafinanceira.api.dto.Dtos.ResumoProjecaoDto;
import br.com.gestafinanceira.domain.calculo.FonteParametros;
import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.adicionais.AdicionalMes;
import br.com.gestafinanceira.domain.calculo.adicionais.TotaisAdicionais;
import br.com.gestafinanceira.domain.calculo.projecao.DadosProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.domain.modelo.Bonus;
import br.com.gestafinanceira.domain.modelo.DestinoAdicional;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.domain.modelo.Ferias;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import br.com.gestafinanceira.infra.persistencia.DestinoAdicionalRepo;
import br.com.gestafinanceira.infra.persistencia.Entidades.Perfil;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
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
    private final DestinoAdicionalRepo destinos;

    public ProjecaoService(PerfilService perfil, LancamentoService lancamentos, DecimoService decimo,
                           PlrService plr, ParametrosService parametros, MetaService metas, Clock clock,
                           DestinoAdicionalRepo destinos) {
        this.metas = metas;
        this.clock = clock;
        this.plr = plr;
        this.perfil = perfil;
        this.lancamentos = lancamentos;
        this.decimo = decimo;
        this.parametros = parametros;
        this.destinos = destinos;
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
        List<AdicionalMes> adicionais = meses.stream().flatMap(m -> m.adicionais().stream()).toList();
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
                ultimo.patrimonio(), Dinheiro.arredondar(p.saldoFuncef), ultimo.previdencia(),
                ultimo.patrimonioTotal(),
                Dinheiro.arredondar(soma.apply(MesProjecao::gastosAdicionais)),
                Dinheiro.arredondar(soma.apply(MesProjecao::reservasAdicionais)),
                new ResumoAdicionaisDto(TotaisAdicionais.somar(adicionais, EventoAdicional.DECIMO),
                        TotaisAdicionais.somar(adicionais, EventoAdicional.PLR), TotaisAdicionais.somar(adicionais)));
        return new ProjecaoDto(ini, p.saldoInicial, meses, resumo);
    }

    /** Mês "comum" (sem 13º e férias) do mês inicial, usado como referência na tela de detalhe. */
    public MesProjecao mesComum(YearMonth mes) {
        Perfil p = perfil.entidade();
        YearMonth m = mes != null ? mes : YearMonth.now(clock);
        return ProjetorMensal.calcularMes(dados(p), m, 0, true, fonte(p));
    }

    /**
     * Prévia do plano de uso de um adicional: as parcelas do evento nos 12 meses a partir do mês atual, divididas
     * pelos destinos gravados. O evento é considerado ativo na prévia mesmo que esteja desligado.
     */
    public List<PreviaAdicionalDto> previa(EventoAdicional evento) {
        Perfil p = perfil.entidade();
        DadosProjecao d = dados(p);
        DadosProjecao forcado = evento == EventoAdicional.DECIMO
                ? new DadosProjecao(d.perfil(), d.gastos(), DecimoService.PARCELAS, d.plr(), d.metas(), d.destinos())
                : new DadosProjecao(d.perfil(), d.gastos(), d.decimo(), plr.parcelasCadastradas(), d.metas(),
                        d.destinos());
        List<PreviaAdicionalDto> r = new ArrayList<>();
        for (MesProjecao m : ProjetorMensal.projetar(forcado, YearMonth.now(clock), fonte(p))) {
            for (AdicionalMes a : m.adicionais()) {
                if (a.evento() == evento) {
                    r.add(new PreviaAdicionalDto(m.competencia(), a.liquido(), a.destinos(), a.semDestino()));
                }
            }
        }
        return r;
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
                new Bonus(p.bonusAtivo, p.bonusMes, p.bonusSalarios), p.saldoFuncef);
        return new DadosProjecao(pc, lancamentos.paraCalculo(), decimo.paraCalculo(), plr.paraCalculo(),
                metas.paraCalculo(), destinosParaCalculo());
    }

    /** Plano de uso do 13º/PLR no formato do motor (meta inexistente vira reserva geral no próprio motor). */
    private List<DestinoAdicional> destinosParaCalculo() {
        return destinos.findAllByOrderByEventoAscOrdemAscIdAsc().stream()
                .map(e -> new DestinoAdicional(e.id, e.evento, e.nome, e.tipo, e.percentual, e.metaId)).toList();
    }

    private FonteParametros fonte(Perfil p) {
        return mes -> parametros.paraMes(mes, p.cargoId, p.delta);
    }
}
