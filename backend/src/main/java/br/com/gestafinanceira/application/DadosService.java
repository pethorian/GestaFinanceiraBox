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
    private final SaudeFinanceiraService saude;
    private final AdicionalService adicionais;
    private final PlrControleService plrControle;

    public DadosService(PerfilService perfil, LancamentoService lancamentos, DecimoService decimo, MetaService metas,
                        PlrService plr, SaudeFinanceiraService saude, AdicionalService adicionais,
                        PlrControleService plrControle) {
        this.plrControle = plrControle;
        this.saude = saude;
        this.plr = plr;
        this.perfil = perfil;
        this.lancamentos = lancamentos;
        this.decimo = decimo;
        this.metas = metas;
        this.adicionais = adicionais;
    }

    public DadosDto buscar() {
        return new DadosDto(perfil.buscar(), perfil.buscarFerias(), lancamentos.listar(), decimo.buscar(), metas.listar(),
                perfil.buscarBonus(), plr.buscar(), saude.buscar(), adicionais.buscarTodos());
    }

    /**
     * Volta perfil, gastos, 13º, PLR (e o controle do PLR recebido, com exclusão lógica), bônus, metas e o plano de
     * uso do 13º/PLR (sem destinos) ao padrão.
     * Tabelas de parâmetros não são alteradas.
     */
    public DadosDto restaurarPadrao() {
        perfil.restaurarPadrao();
        lancamentos.restaurarPadrao();
        decimo.restaurarPadrao();
        plr.restaurarPadrao();
        plrControle.restaurarPadrao();
        adicionais.restaurarPadrao();
        metas.restaurarPadrao();
        return buscar();
    }
}
