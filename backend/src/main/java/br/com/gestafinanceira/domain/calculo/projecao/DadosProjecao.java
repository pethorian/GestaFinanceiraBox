package br.com.gestafinanceira.domain.calculo.projecao;

import br.com.gestafinanceira.domain.modelo.DestinoAdicional;
import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.ParcelaPlr;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import java.util.List;

/**
 * {@code plr}: parcelas do PLR (lista vazia = PLR inativo); cada uma vale o percentual do PLR anual do cargo
 * ou o valor bruto informado. {@code metas}: metas/reservas cujos aportes mensais saem da sobra do mês
 * (lista vazia = sem aportes). {@code destinos}: plano de uso do 13º e do PLR (lista vazia = sem destinos, todo o
 * líquido fica no saldo disponível, comportamento padrão).
 */
public record DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo,
                            List<ParcelaPlr> plr, List<MetaCalculo> metas, List<DestinoAdicional> destinos) {

    public DadosProjecao {
        metas = metas == null ? List.of() : List.copyOf(metas);
        destinos = destinos == null ? List.of() : List.copyOf(destinos);
    }

    public DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo,
                         List<ParcelaPlr> plr, List<MetaCalculo> metas) {
        this(perfil, gastos, decimo, plr, metas, List.of());
    }

    public DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo,
                         List<ParcelaPlr> plr) {
        this(perfil, gastos, decimo, plr, List.of(), List.of());
    }

    public DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo) {
        this(perfil, gastos, decimo, List.of(), List.of(), List.of());
    }
}
