package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.PlrControleDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrGastoDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrGastoEntradaDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrRecebimentoDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrRecebimentoEntradaDto;
import br.com.gestafinanceira.application.Erros.DadosInvalidos;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.domain.calculo.plr.ControlePlr;
import br.com.gestafinanceira.domain.calculo.plr.ControlePlr.DestinoEntrada;
import br.com.gestafinanceira.domain.calculo.plr.ControlePlr.GastoEntrada;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.infra.persistencia.DestinoAdicionalRepo;
import br.com.gestafinanceira.infra.persistencia.Entidades.DestinoAdicionalEntidade;
import br.com.gestafinanceira.infra.persistencia.Entidades.PlrGasto;
import br.com.gestafinanceira.infra.persistencia.Entidades.PlrRecebimento;
import br.com.gestafinanceira.infra.persistencia.PlrGastoRepo;
import br.com.gestafinanceira.infra.persistencia.PlrRecebimentoRepo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controle do PLR recebido: recebimentos e gastos reais, divididos pelos destinos do plano de uso do PLR
 * ("envelopes"). Não altera a projeção. A conta é feita no domínio ({@link ControlePlr}).
 */
@Service
@Transactional
public class PlrControleService {
    static final int MAX_DESC_RECEBIMENTO = 120;
    static final int MAX_DESC_GASTO = 160;

    private final PlrRecebimentoRepo recebimentos;
    private final PlrGastoRepo gastos;
    private final DestinoAdicionalRepo destinos;
    private final Clock clock;

    public PlrControleService(PlrRecebimentoRepo recebimentos, PlrGastoRepo gastos, DestinoAdicionalRepo destinos,
                              Clock clock) {
        this.recebimentos = recebimentos;
        this.gastos = gastos;
        this.destinos = destinos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PlrControleDto buscar() {
        List<DestinoAdicionalEntidade> dest = destinos.findByEventoOrderByOrdemAscIdAsc(EventoAdicional.PLR);
        Map<Long, String> nomes = dest.stream().collect(Collectors.toMap(d -> d.id, d -> d.nome));
        List<PlrRecebimento> recs = recebimentos.findAllByDeletadoEmIsNullOrderByDataDescIdDesc();
        List<PlrGasto> gts = gastos.findAllByDeletadoEmIsNullOrderByDataDescIdDesc();

        ControlePlr.Resumo r = ControlePlr.calcular(new ControlePlr.Entrada(
                recs.stream().map(x -> x.valor).toList(),
                dest.stream().map(d -> new DestinoEntrada(d.id, d.nome, d.tipo, d.percentual)).toList(),
                gts.stream().map(g -> new GastoEntrada(g.destinoId, g.valor)).toList()));
        return new PlrControleDto(r.recebido(), r.gasto(), r.saldo(), r.destinos(), r.semDestino(),
                recs.stream().map(PlrControleService::paraDto).toList(),
                gts.stream().map(g -> paraDto(g, nomes)).toList());
    }

    public PlrRecebimentoDto criarRecebimento(PlrRecebimentoEntradaDto d) {
        PlrRecebimento e = new PlrRecebimento();
        aplicar(e, d);
        return paraDto(recebimentos.save(e));
    }

    public PlrRecebimentoDto atualizarRecebimento(Long id, PlrRecebimentoEntradaDto d) {
        PlrRecebimento e = recebimento(id);
        aplicar(e, d);
        return paraDto(recebimentos.save(e));
    }

    public void removerRecebimento(Long id) {
        PlrRecebimento e = recebimento(id);
        e.deletadoEm = LocalDateTime.now(clock);
        recebimentos.save(e);
    }

    public PlrGastoDto criarGasto(PlrGastoEntradaDto d) {
        PlrGasto e = new PlrGasto();
        aplicar(e, d);
        return paraDto(gastos.save(e), nomesDestinos());
    }

    public PlrGastoDto atualizarGasto(Long id, PlrGastoEntradaDto d) {
        PlrGasto e = gasto(id);
        aplicar(e, d);
        return paraDto(gastos.save(e), nomesDestinos());
    }

    public void removerGasto(Long id) {
        PlrGasto e = gasto(id);
        e.deletadoEm = LocalDateTime.now(clock);
        gastos.save(e);
    }

    /** Restaurar o padrão: exclusão lógica de todos os recebimentos e gastos do PLR. */
    public void restaurarPadrao() {
        LocalDateTime agora = LocalDateTime.now(clock);
        gastos.excluirTodosAtivos(agora);
        recebimentos.excluirTodosAtivos(agora);
    }

    private PlrRecebimento recebimento(Long id) {
        return recebimentos.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new NaoEncontrado("Recebimento de PLR " + id + " não encontrado."));
    }

    private PlrGasto gasto(Long id) {
        return gastos.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new NaoEncontrado("Gasto do PLR " + id + " não encontrado."));
    }

    private Map<Long, String> nomesDestinos() {
        return destinos.findByEventoOrderByOrdemAscIdAsc(EventoAdicional.PLR).stream()
                .collect(Collectors.toMap(d -> d.id, d -> d.nome, (a, b) -> a));
    }

    private void aplicar(PlrRecebimento e, PlrRecebimentoEntradaDto d) {
        if (d == null) throw new DadosInvalidos(List.of("corpo: obrigatório"));
        List<String> erros = new ArrayList<>();
        String desc = descricao(d.descricao(), MAX_DESC_RECEBIMENTO, erros);
        if (d.data() == null) erros.add("data: obrigatória");
        BigDecimal valor = valor(d.valor(), erros);
        if (!erros.isEmpty()) throw new DadosInvalidos(erros);
        e.data = d.data();
        e.descricao = desc;
        e.valor = valor;
    }

    private void aplicar(PlrGasto e, PlrGastoEntradaDto d) {
        if (d == null) throw new DadosInvalidos(List.of("corpo: obrigatório"));
        List<String> erros = new ArrayList<>();
        String desc = descricao(d.descricao(), MAX_DESC_GASTO, erros);
        if (d.data() == null) erros.add("data: obrigatória");
        BigDecimal valor = valor(d.valor(), erros);
        if (!erros.isEmpty()) throw new DadosInvalidos(erros);
        if (d.destinoId() != null) {
            boolean doPlr = destinos.findById(d.destinoId()).map(x -> x.evento == EventoAdicional.PLR).orElse(false);
            if (!doPlr) throw new RegraDeNegocio("Destino " + d.destinoId() + " não pertence ao plano de uso do PLR.");
        }
        e.data = d.data();
        e.descricao = desc;
        e.valor = valor;
        e.destinoId = d.destinoId();
    }

    private static String descricao(String bruta, int max, List<String> erros) {
        String s = bruta == null ? "" : bruta.strip();
        if (s.isEmpty()) erros.add("descricao: obrigatória");
        else if (s.length() > max) erros.add("descricao: até " + max + " caracteres");
        return s;
    }

    private static BigDecimal valor(BigDecimal v, List<String> erros) {
        if (v == null) {
            erros.add("valor: obrigatório");
            return null;
        }
        if (v.signum() <= 0) erros.add("valor: deve ser maior que 0");
        else if (v.stripTrailingZeros().scale() > 2) erros.add("valor: no máximo 2 casas decimais");
        return v.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static PlrRecebimentoDto paraDto(PlrRecebimento e) {
        return new PlrRecebimentoDto(e.id, e.data, e.descricao, e.valor);
    }

    private static PlrGastoDto paraDto(PlrGasto e, Map<Long, String> nomes) {
        return new PlrGastoDto(e.id, e.data, e.descricao, e.valor, e.destinoId,
                e.destinoId == null ? null : nomes.get(e.destinoId));
    }
}
