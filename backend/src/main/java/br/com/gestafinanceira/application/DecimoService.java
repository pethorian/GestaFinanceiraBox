package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.DecimoDto;
import br.com.gestafinanceira.api.dto.Dtos.ParcelaDecimoDto;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 13º salário: parcelas fixas (fevereiro 50% e novembro 50%), com a opção de contar ou não na projeção. */
@Service
@Transactional
public class DecimoService {
    /** Parcelas fixas do 13º; não são editáveis. */
    static final List<ParcelaDecimo> PARCELAS = List.of(
            new ParcelaDecimo(2, BigDecimal.valueOf(50)), new ParcelaDecimo(11, BigDecimal.valueOf(50)));

    private final PerfilService perfil;

    public DecimoService(PerfilService perfil) {
        this.perfil = perfil;
    }

    @Transactional(readOnly = true)
    public DecimoDto buscar() {
        List<ParcelaDecimoDto> dtos = PARCELAS.stream().map(p -> new ParcelaDecimoDto(p.mes(), p.percentual())).toList();
        BigDecimal total = PARCELAS.stream().map(ParcelaDecimo::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DecimoDto(perfil.entidade().decimoAtivo, dtos, total);
    }

    public DecimoDto definirAtivo(boolean ativo) {
        perfil.entidade().decimoAtivo = ativo;
        return buscar();
    }

    /** Parcelas para o cálculo da projeção: vazio quando o 13º está inativo. */
    @Transactional(readOnly = true)
    public List<ParcelaDecimo> paraCalculo() {
        return perfil.entidade().decimoAtivo ? PARCELAS : List.of();
    }

    /** Padrão: ativo. */
    public void restaurarPadrao() {
        perfil.entidade().decimoAtivo = true;
    }
}
