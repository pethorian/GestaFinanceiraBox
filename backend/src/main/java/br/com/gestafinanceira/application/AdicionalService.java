package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.DestinoDto;
import br.com.gestafinanceira.api.dto.Dtos.DestinoEntradaDto;
import br.com.gestafinanceira.api.dto.Dtos.PlanoAdicionalDto;
import br.com.gestafinanceira.api.dto.Dtos.PlanosAdicionaisDto;
import br.com.gestafinanceira.application.Erros.DadosInvalidos;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.domain.modelo.TipoDestino;
import br.com.gestafinanceira.infra.persistencia.DestinoAdicionalRepo;
import br.com.gestafinanceira.infra.persistencia.Entidades.DestinoAdicionalEntidade;
import br.com.gestafinanceira.infra.persistencia.Entidades.Meta;
import br.com.gestafinanceira.infra.persistencia.Entidades.Perfil;
import br.com.gestafinanceira.infra.persistencia.MetaRepo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plano de uso do 13º e do PLR: lista de destinos por evento (nome, tipo, percentual do líquido de cada parcela e
 * meta opcional para RESERVA). O PUT substitui a lista inteira; lista vazia = sem destinos (padrão).
 * A divisão em si é calculada no domínio ({@code CalculadoraDestinos}/{@code PlanoAdicionais}).
 */
@Service
@Transactional
public class AdicionalService {
    static final int MAX_DESTINOS = 20;
    static final int MAX_NOME = 60;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final DestinoAdicionalRepo destinos;
    private final MetaRepo metas;
    private final PerfilService perfil;
    private final ProjecaoService projecao;

    public AdicionalService(DestinoAdicionalRepo destinos, MetaRepo metas, PerfilService perfil,
                            ProjecaoService projecao) {
        this.destinos = destinos;
        this.metas = metas;
        this.perfil = perfil;
        this.projecao = projecao;
    }

    /** Converte o segmento da URL ({@code decimo} | {@code plr}, sem diferenciar maiúsculas); outro valor = 404. */
    public static EventoAdicional evento(String caminho) {
        return switch (caminho == null ? "" : caminho.toLowerCase(Locale.ROOT)) {
            case "decimo" -> EventoAdicional.DECIMO;
            case "plr" -> EventoAdicional.PLR;
            default -> throw new NaoEncontrado("Adicional '" + caminho + "' não existe (use decimo ou plr).");
        };
    }

    public PlanoAdicionalDto buscar(EventoAdicional evento) {
        Map<Long, String> nomesMetas = metas.findAll().stream()
                .collect(Collectors.toMap(m -> m.id, m -> m.nome, (a, b) -> a));
        List<DestinoDto> lista = destinos.findByEventoOrderByOrdemAscIdAsc(evento).stream().map(e -> {
            // meta apagada: o banco anula meta_id (ON DELETE SET NULL); aqui também cobre entidade ainda em memória
            Long metaId = e.metaId != null && nomesMetas.containsKey(e.metaId) ? e.metaId : null;
            return new DestinoDto(e.id, e.nome, e.tipo, e.percentual, metaId, metaId != null ? nomesMetas.get(metaId) : null);
        }).toList();
        BigDecimal total = lista.stream().map(DestinoDto::percentual).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
        Perfil p = perfil.entidade();
        boolean ativo = evento == EventoAdicional.DECIMO ? p.decimoAtivo : p.plrAtivo;
        return new PlanoAdicionalDto(evento, ativo, lista, total, CEM.subtract(total).setScale(2),
                projecao.previa(evento));
    }

    public PlanosAdicionaisDto buscarTodos() {
        return new PlanosAdicionaisDto(buscar(EventoAdicional.DECIMO), buscar(EventoAdicional.PLR));
    }

    /** Substitui a lista inteira de destinos do evento, na ordem recebida. */
    public PlanoAdicionalDto substituir(EventoAdicional evento, List<DestinoEntradaDto> novos) {
        List<DestinoEntradaDto> lista = validar(novos);
        destinos.apagarDoEvento(evento);
        for (int i = 0; i < lista.size(); i++) {
            DestinoEntradaDto d = lista.get(i);
            DestinoAdicionalEntidade e = new DestinoAdicionalEntidade();
            e.evento = evento;
            e.nome = d.nome();
            e.tipo = d.tipo();
            e.percentual = d.percentual();
            e.metaId = d.metaId();
            e.ordem = i;
            destinos.save(e);
        }
        destinos.flush();
        return buscar(evento);
    }

    /** Padrão: nenhum destino (todo o adicional fica no saldo disponível). */
    public void restaurarPadrao() {
        destinos.deleteAllInBatch();
    }

    /** 400 para formato (nome, tipo, percentual, quantidade); 422 para regra (soma, meta inexistente ou fora de RESERVA). */
    private List<DestinoEntradaDto> validar(List<DestinoEntradaDto> novos) {
        if (novos == null) throw new DadosInvalidos(List.of("destinos: lista obrigatória (pode ser vazia)"));
        List<String> erros = new ArrayList<>();
        if (novos.size() > MAX_DESTINOS) erros.add("destinos: no máximo " + MAX_DESTINOS + " itens");
        List<DestinoEntradaDto> r = new ArrayList<>(novos.size());
        for (int i = 0; i < novos.size(); i++) {
            DestinoEntradaDto d = novos.get(i);
            String campo = "destinos[" + i + "].";
            if (d == null) {
                erros.add("destinos[" + i + "]: item obrigatório");
                continue;
            }
            String nome = d.nome() == null ? "" : d.nome().strip();
            if (nome.isEmpty()) erros.add(campo + "nome: obrigatório");
            else if (nome.length() > MAX_NOME) erros.add(campo + "nome: até " + MAX_NOME + " caracteres");
            if (d.tipo() == null) erros.add(campo + "tipo: obrigatório (LAZER, OBRIGATORIO ou RESERVA)");
            BigDecimal pct = d.percentual();
            if (pct == null) erros.add(campo + "percentual: obrigatório");
            else if (pct.signum() <= 0 || pct.compareTo(CEM) > 0) erros.add(campo + "percentual: deve ser maior que 0 e até 100");
            else if (pct.stripTrailingZeros().scale() > 2) erros.add(campo + "percentual: no máximo 2 casas decimais");
            r.add(new DestinoEntradaDto(nome, d.tipo(), pct, d.metaId()));
        }
        if (!erros.isEmpty()) throw new DadosInvalidos(erros);

        BigDecimal total = r.stream().map(DestinoEntradaDto::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(CEM) > 0) {
            throw new RegraDeNegocio("A soma dos percentuais dos destinos não pode passar de 100% (atual: "
                    + total.stripTrailingZeros().toPlainString() + "%).");
        }
        Map<Long, Meta> existentes = metas.findAll().stream().collect(Collectors.toMap(m -> m.id, Function.identity()));
        for (DestinoEntradaDto d : r) {
            if (d.metaId() == null) continue;
            if (d.tipo() != TipoDestino.RESERVA) {
                throw new RegraDeNegocio("O destino '" + d.nome() + "' só pode ser ligado a uma meta se for do tipo RESERVA.");
            }
            if (!existentes.containsKey(d.metaId())) {
                throw new RegraDeNegocio("Meta " + d.metaId() + " não encontrada para o destino '" + d.nome() + "'.");
            }
        }
        return r;
    }
}
