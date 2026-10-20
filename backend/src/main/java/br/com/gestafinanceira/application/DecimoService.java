package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.DecimoDto;
import br.com.gestafinanceira.api.dto.Dtos.ParcelaDecimoDto;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.infra.persistencia.EventoRepo;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Parcelas do 13º definidas pelo usuário (mês + percentual). A soma não pode passar de 100%. */
@Service
@Transactional
public class DecimoService {
    private final ParcelasEvento parcelas;

    public DecimoService(EventoRepo eventos) {
        this.parcelas = new ParcelasEvento(eventos, "DECIMO_TERCEIRO", "13º");
    }

    @Transactional(readOnly = true)
    public DecimoDto buscar() {
        return montar(parcelas.listar());
    }

    public DecimoDto substituir(List<ParcelaDecimoDto> novas) {
        parcelas.substituir(novas);
        return buscar();
    }

    /** Acrescenta uma parcela com o que falta para 100%, no último mês livre do ano (dezembro para trás). */
    public DecimoDto adicionarParcela() {
        parcelas.adicionar();
        return buscar();
    }

    @Transactional(readOnly = true)
    public List<ParcelaDecimo> paraCalculo() {
        return parcelas.paraCalculo();
    }

    /** Padrão: 50% em fevereiro e 50% em novembro. */
    public void restaurarPadrao() {
        substituir(List.of(new ParcelaDecimoDto(2, BigDecimal.valueOf(50)), new ParcelaDecimoDto(11, BigDecimal.valueOf(50))));
    }

    private static DecimoDto montar(List<ParcelaDecimoDto> parcelas) {
        BigDecimal total = ParcelasEvento.soma(parcelas);
        String aviso = null;
        if (parcelas.isEmpty()) {
            aviso = "Nenhuma parcela do 13º cadastrada.";
        } else if (total.compareTo(ParcelasEvento.CEM) < 0) {
            aviso = "A soma dos percentuais é " + total + "%; os " + ParcelasEvento.CEM.subtract(total)
                    + "% restantes não serão recebidos na projeção.";
        }
        return new DecimoDto(parcelas, total, aviso);
    }
}
