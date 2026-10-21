package br.com.gestafinanceira.domain.calculo.projecao;

import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.ParcelaPlr;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import java.util.List;

/**
 * {@code plr}: parcelas do PLR (lista vazia = PLR inativo); cada uma vale o percentual do PLR anual do cargo
 * ou o valor bruto informado. {@code metas}: metas/reservas cujos aportes mensais saem da sobra do mês
 * (lista vazia = sem aportes).
 */
public record DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo,
                            List<ParcelaPlr> plr, List<MetaCalculo> metas) {

    public DadosProjecao {
        metas = metas == null ? List.of() : List.copyOf(metas);
    }

    public DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo,
                         List<ParcelaPlr> plr) {
        this(perfil, gastos, decimo, plr, List.of());
    }

    public DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo) {
        this(perfil, gastos, decimo, List.of(), List.of());
    }
}
