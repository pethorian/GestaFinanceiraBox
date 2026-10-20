package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.ParcelaDecimoDto;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.infra.persistencia.Entidades.EventoSalarial;
import br.com.gestafinanceira.infra.persistencia.EventoRepo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras comuns das parcelas de eventos salariais (13º e PLR) guardadas em {@code evento_salarial}:
 * meses únicos, soma dos percentuais ≤ 100 e acréscimo automático do que falta para 100%.
 */
final class ParcelasEvento {
    static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final EventoRepo eventos;
    private final String tipo;
    private final String rotulo;

    ParcelasEvento(EventoRepo eventos, String tipo, String rotulo) {
        this.eventos = eventos;
        this.tipo = tipo;
        this.rotulo = rotulo;
    }

    List<ParcelaDecimoDto> listar() {
        return eventos.findByTipoOrderByMesAsc(tipo).stream()
                .map(e -> new ParcelaDecimoDto(e.mes, e.percentual)).toList();
    }

    List<ParcelaDecimo> paraCalculo() {
        return eventos.findByTipoOrderByMesAsc(tipo).stream().map(e -> new ParcelaDecimo(e.mes, e.percentual)).toList();
    }

    void substituir(List<ParcelaDecimoDto> parcelas) {
        if (parcelas.stream().map(ParcelaDecimoDto::mes).distinct().count() != parcelas.size()) {
            throw new RegraDeNegocio("Há mais de uma parcela do " + rotulo + " no mesmo mês.");
        }
        BigDecimal total = soma(parcelas);
        if (total.compareTo(CEM) > 0) {
            throw new RegraDeNegocio("A soma dos percentuais do " + rotulo + " não pode passar de 100% (atual: "
                    + total + "%).");
        }
        eventos.deleteByTipo(tipo);
        eventos.flush();
        for (ParcelaDecimoDto p : parcelas) {
            EventoSalarial e = new EventoSalarial();
            e.tipo = tipo;
            e.mes = (short) p.mes();
            e.percentual = p.percentual();
            eventos.save(e);
        }
        eventos.flush();
    }

    /** Acrescenta uma parcela com o que falta para 100%, no último mês livre do ano (dezembro para trás). */
    void adicionar() {
        List<ParcelaDecimoDto> atuais = new ArrayList<>(listar());
        BigDecimal falta = CEM.subtract(soma(atuais));
        if (falta.signum() <= 0) throw new RegraDeNegocio("O " + rotulo + " já está 100% distribuído.");
        int mes = 12;
        while (mes > 1 && usado(atuais, mes)) mes--;
        if (usado(atuais, mes)) throw new RegraDeNegocio("Todos os meses já têm parcela do " + rotulo + ".");
        atuais.add(new ParcelaDecimoDto(mes, falta));
        substituir(atuais);
    }

    private static boolean usado(List<ParcelaDecimoDto> parcelas, int mes) {
        return parcelas.stream().anyMatch(p -> p.mes() == mes);
    }

    static BigDecimal soma(List<ParcelaDecimoDto> parcelas) {
        return parcelas.stream().map(ParcelaDecimoDto::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
