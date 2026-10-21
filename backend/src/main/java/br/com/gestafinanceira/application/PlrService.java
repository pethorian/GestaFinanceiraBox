package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.ParcelaPlrDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrDto;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.domain.calculo.plr.CalculadoraPlr;
import br.com.gestafinanceira.domain.calculo.plr.CalculadoraPlr.ParcelaCalculada;
import br.com.gestafinanceira.domain.modelo.ParcelaPlr;
import br.com.gestafinanceira.infra.persistencia.CargoValorRepo;
import br.com.gestafinanceira.infra.persistencia.Entidades.CargoValor;
import br.com.gestafinanceira.infra.persistencia.Entidades.EventoSalarial;
import br.com.gestafinanceira.infra.persistencia.EventoRepo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PLR do usuário: flag {@code ativo} (padrão false; inativo = nada entra na projeção) e parcelas (mês, percentual do
 * PLR anual do cargo e valor bruto opcional que substitui o calculado). Ao ativar sem parcelas valem as padrão:
 * setembro 50% (antecipação) e março 50% (saldo). Tributação em {@link CalculadoraPlr}.
 */
@Service
@Transactional
public class PlrService {
    static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final String TIPO = "PLR";

    private final EventoRepo eventos;
    private final PerfilService perfil;
    private final CargoValorRepo cargoValores;
    private final ParametrosService parametros;

    public PlrService(EventoRepo eventos, PerfilService perfil, CargoValorRepo cargoValores,
                      ParametrosService parametros) {
        this.eventos = eventos;
        this.perfil = perfil;
        this.cargoValores = cargoValores;
        this.parametros = parametros;
    }

    /** Parcelas e valores calculados com a estimativa e a tabela de IR de hoje (também quando inativo, como prévia). */
    @Transactional(readOnly = true)
    public PlrDto buscar() {
        List<ParcelaPlr> lista = listar();
        BigDecimal estimativa = estimativaAnual();
        List<ParcelaCalculada> calc = CalculadoraPlr.calcular(lista, estimativa, parametros.faixasPlr(LocalDate.now()));
        List<ParcelaPlrDto> dtos = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            ParcelaPlr p = lista.get(i);
            ParcelaCalculada c = calc.get(i);
            dtos.add(new ParcelaPlrDto(p.mes(), p.percentual(), p.valor(), c.bruto(), c.desconto(), c.liquido()));
        }
        BigDecimal total = lista.stream().map(ParcelaPlr::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean ativo = perfil.entidade().plrAtivo;
        String aviso = null;
        if (ativo && !lista.isEmpty() && total.compareTo(CEM) < 0) {
            aviso = "A soma dos percentuais é " + total + "%; os " + CEM.subtract(total)
                    + "% restantes não serão recebidos na projeção.";
        }
        return new PlrDto(ativo, dtos, total, aviso, estimativa);
    }

    /**
     * Grava {@code ativo} (null mantém) e as parcelas (null mantém). Ao ficar ativo sem nenhuma parcela, grava as padrão.
     */
    public PlrDto substituir(Boolean ativo, List<ParcelaPlrDto> novas) {
        var p = perfil.entidade();
        List<ParcelaPlr> lista = novas != null
                ? novas.stream().map(d -> new ParcelaPlr(d.mes(), d.percentual(), d.valor())).toList()
                : listar();
        boolean ativoFinal = ativo != null ? ativo : p.plrAtivo;
        if (ativoFinal && lista.isEmpty()) lista = padrao();
        gravar(lista);
        p.plrAtivo = ativoFinal;
        return buscar();
    }

    /** Parcelas para o cálculo da projeção: vazio quando o PLR está inativo. */
    @Transactional(readOnly = true)
    public List<ParcelaPlr> paraCalculo() {
        return perfil.entidade().plrAtivo ? listar() : List.of();
    }

    /** Parcelas gravadas, mesmo com o PLR inativo (prévia do plano de uso). */
    @Transactional(readOnly = true)
    public List<ParcelaPlr> parcelasCadastradas() {
        return listar();
    }

    /** Padrão: inativo, com as parcelas setembro 50% e março 50% prontas. */
    public void restaurarPadrao() {
        gravar(padrao());
        perfil.entidade().plrAtivo = false;
    }

    private static List<ParcelaPlr> padrao() {
        return List.of(new ParcelaPlr(9, BigDecimal.valueOf(50)), new ParcelaPlr(3, BigDecimal.valueOf(50)));
    }

    private List<ParcelaPlr> listar() {
        return eventos.findByTipoOrderByMesAsc(TIPO).stream()
                .map(e -> new ParcelaPlr(e.mes, e.percentual, e.valor)).toList();
    }

    private void gravar(List<ParcelaPlr> parcelas) {
        if (parcelas.stream().map(ParcelaPlr::mes).distinct().count() != parcelas.size()) {
            throw new RegraDeNegocio("Há mais de uma parcela do PLR no mesmo mês.");
        }
        BigDecimal total = soma(parcelas);
        if (total.compareTo(CEM) > 0) {
            throw new RegraDeNegocio("A soma dos percentuais do PLR não pode passar de 100% (atual: " + total + "%).");
        }
        eventos.deleteByTipo(TIPO);
        eventos.flush();
        for (ParcelaPlr p : parcelas) {
            EventoSalarial e = new EventoSalarial();
            e.tipo = TIPO;
            e.mes = (short) p.mes();
            e.percentual = p.percentual();
            e.valor = p.valor();
            eventos.save(e);
        }
        eventos.flush();
    }

    private static boolean usado(List<ParcelaPlr> parcelas, int mes) {
        return parcelas.stream().anyMatch(p -> p.mes() == mes);
    }

    private static BigDecimal soma(List<ParcelaPlr> parcelas) {
        return parcelas.stream().map(ParcelaPlr::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** PLR anual do cargo do perfil, na vigência de hoje (ou a mais antiga se todas começam depois). */
    private BigDecimal estimativaAnual() {
        Long cargoId = perfil.entidade().cargoId;
        CargoValor v = cargoValores.vigente(cargoId, LocalDate.now()).or(() -> cargoValores.maisAntigo(cargoId))
                .orElse(null);
        return v != null && v.plrAnual != null ? v.plrAnual : BigDecimal.ZERO;
    }
}
