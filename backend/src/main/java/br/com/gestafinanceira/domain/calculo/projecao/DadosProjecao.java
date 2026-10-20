package br.com.gestafinanceira.domain.calculo.projecao;

import br.com.gestafinanceira.domain.modelo.GastoCalculo;
import br.com.gestafinanceira.domain.modelo.ParcelaDecimo;
import br.com.gestafinanceira.domain.modelo.PerfilCalculo;
import java.util.List;

/** {@code plr} usa o mesmo formato do 13º (mês + percentual), aplicado sobre o PLR anual do cargo. */
public record DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo,
                            List<ParcelaDecimo> plr) {

    public DadosProjecao(PerfilCalculo perfil, List<GastoCalculo> gastos, List<ParcelaDecimo> decimo) {
        this(perfil, gastos, decimo, List.of());
    }
}
