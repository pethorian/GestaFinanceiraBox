package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.MetaDto;
import br.com.gestafinanceira.api.dto.Dtos.MetaEntradaDto;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.domain.calculo.metas.CalculadoraAportes;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import br.com.gestafinanceira.infra.persistencia.Entidades.Meta;
import br.com.gestafinanceira.infra.persistencia.MetaRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MetaService {
    private final MetaRepo metas;
    private final Clock clock;

    public MetaService(MetaRepo metas, Clock clock) {
        this.metas = metas;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MetaDto> listar() {
        return metas.findAllByOrderByIdAsc().stream().map(this::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public MetaDto buscar(Long id) {
        return paraDto(entidade(id));
    }

    /** Metas na ordem de cadastro, no formato do motor de cálculo (aportes da projeção). */
    @Transactional(readOnly = true)
    public List<MetaCalculo> paraCalculo() {
        return metas.findAllByOrderByIdAsc().stream().map(MetaService::paraCalculo).toList();
    }

    public MetaDto criar(MetaEntradaDto d) {
        Meta m = new Meta();
        aplicar(m, d);
        return paraDto(metas.save(m));
    }

    public MetaDto atualizar(Long id, MetaEntradaDto d) {
        Meta m = entidade(id);
        aplicar(m, d);
        return paraDto(metas.save(m));
    }

    public void remover(Long id) {
        metas.delete(entidade(id));
    }

    /** Nova meta com os valores padrão definidos pelo servidor. */
    public MetaDto criarNova() {
        return criar(new MetaEntradaDto("Nova meta", BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(500)));
    }

    public void restaurarPadrao() {
        metas.deleteAllInBatch();
        criar(new MetaEntradaDto("Reserva de emergência", BigDecimal.valueOf(30000), BigDecimal.valueOf(3200), BigDecimal.valueOf(900)));
        criar(new MetaEntradaDto("Entrada do apartamento", BigDecimal.valueOf(80000), BigDecimal.valueOf(12000), BigDecimal.valueOf(1500)));
        criar(new MetaEntradaDto("Troca de carro", BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.valueOf(600)));
    }

    private Meta entidade(Long id) {
        return metas.findById(id).orElseThrow(() -> new NaoEncontrado("Meta " + id + " não encontrada."));
    }

    private static void aplicar(Meta m, MetaEntradaDto d) {
        m.nome = d.nome().trim();
        m.valorAlvo = d.valorAlvo();
        m.valorAtual = d.valorAtual();
        m.aporteMensal = d.aporteMensal();
        m.banco = d.banco(); // já aparado; vazio vira null no DTO
    }

    private static MetaCalculo paraCalculo(Meta m) {
        return new MetaCalculo(m.id, m.nome, m.valorAlvo, m.valorAtual, m.aporteMensal);
    }

    /**
     * Quanto falta, quantos aportes faltam e em que mês a meta é atingida, com o primeiro aporte no mês atual
     * (mesma regra da projeção: {@link CalculadoraAportes}), e o saldo previsto ao fim dos 12 meses.
     */
    private MetaDto paraDto(Meta m) {
        MetaCalculo mc = paraCalculo(m);
        YearMonth hoje = YearMonth.now(clock);
        BigDecimal falta = m.valorAlvo.subtract(m.valorAtual).max(BigDecimal.ZERO);
        Integer aportes = CalculadoraAportes.aportesParaAtingir(mc).orElse(null);
        YearMonth previsao = CalculadoraAportes.previsao(mc, hoje).orElse(null);
        BigDecimal pct = m.valorAlvo.signum() > 0
                ? m.valorAtual.multiply(BigDecimal.valueOf(100)).divide(m.valorAlvo, 0, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        return new MetaDto(m.id, m.nome, m.valorAlvo, m.valorAtual, m.aporteMensal, m.banco, falta, pct, aportes,
                previsao, CalculadoraAportes.guardadoApos(mc, ProjetorMensal.MESES),
                hoje.plusMonths(ProjetorMensal.MESES - 1L));
    }
}
