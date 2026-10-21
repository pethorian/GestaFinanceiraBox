package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.DadosDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Visão agregada dos dados do usuário e restauração do estado inicial. */
@Service
@Transactional
public class DadosService {
    private final PerfilService perfil;
    private final LancamentoService lancamentos;
    private final DecimoService decimo;
    private final MetaService metas;
    private final PlrService plr;

    public DadosService(PerfilService perfil, LancamentoService lancamentos, DecimoService decimo, MetaService metas,
                        PlrService plr) {
        this.plr = plr;
        this.perfil = perfil;
        this.lancamentos = lancamentos;
        this.decimo = decimo;
        this.metas = metas;
    }

    public DadosDto buscar() {
        return new DadosDto(perfil.buscar(), perfil.buscarFerias(), lancamentos.listar(), decimo.buscar(), metas.listar(),
                perfil.buscarBonus(), plr.buscar());
    }

    /** Volta perfil, gastos, 13º, PLR, bônus e metas ao padrão. Tabelas de parâmetros não são alteradas. */
    public DadosDto restaurarPadrao() {
        perfil.restaurarPadrao();
        lancamentos.restaurarPadrao();
        decimo.restaurarPadrao();
        plr.restaurarPadrao();
        metas.restaurarPadrao();
        return buscar();
    }
}
