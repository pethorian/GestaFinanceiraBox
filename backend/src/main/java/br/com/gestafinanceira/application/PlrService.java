package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.ParcelaDecimoDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrDto;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.infra.persistencia.CargoValorRepo;
import br.com.gestafinanceira.infra.persistencia.Entidades.CargoValor;
import br.com.gestafinanceira.infra.persistencia.EventoRepo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parcelas do PLR (mês + percentual do PLR anual do cargo). Valor da parcela = estimativa anual × percentual,
 * tratado como líquido. Padrão: nenhuma parcela.
 */
@Service
@Transactional
public class PlrService {
    private final ParcelasEvento parcelas;
    private final PerfilService perfil;
    private final CargoValorRepo cargoValores;

    public PlrService(EventoRepo eventos, PerfilService perfil, CargoValorRepo cargoValores) {
        this.parcelas = new ParcelasEvento(eventos, "PLR", "PLR");
        this.perfil = perfil;
        this.cargoValores = cargoValores;
    }

    @Transactional(readOnly = true)
    public PlrDto buscar() {
        List<ParcelaDecimoDto> lista = parcelas.listar();
        BigDecimal total = ParcelasEvento.soma(lista);
        String aviso = null;
        if (!lista.isEmpty() && total.compareTo(ParcelasEvento.CEM) < 0) {
            aviso = "A soma dos percentuais é " + total + "%; os " + ParcelasEvento.CEM.subtract(total)
                    + "% restantes não serão recebidos na projeção.";
        }
        return new PlrDto(lista, total, aviso, estimativaAnual());
    }

    public PlrDto substituir(List<ParcelaDecimoDto> novas) {
        parcelas.substituir(novas);
        return buscar();
    }

    public PlrDto adicionarParcela() {
        parcelas.adicionar();
        return buscar();
    }

    @Transactional(readOnly = true)
    public List<ParcelaDecimo> paraCalculo() {
        return parcelas.paraCalculo();
    }

    /** Padrão: nenhuma parcela. */
    public void restaurarPadrao() {
        parcelas.substituir(List.of());
    }

    /** PLR anual do cargo do perfil, na vigência de hoje (ou a mais antiga se todas começam depois). */
    private BigDecimal estimativaAnual() {
        Long cargoId = perfil.entidade().cargoId;
        CargoValor v = cargoValores.vigente(cargoId, LocalDate.now()).or(() -> cargoValores.maisAntigo(cargoId))
                .orElse(null);
        return v != null && v.plrAnual != null ? v.plrAnual : BigDecimal.ZERO;
    }
}
