package br.com.gestafinanceira.domain.calculo.metas;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Aportes mensais das metas (regra única usada pela projeção e pela previsão de conclusão).
 *
 * <ul>
 *   <li>Com alvo ({@code valorAlvo > 0}): aporta o aporte mensal enquanto o guardado não atinge o alvo; no mês em
 *   que atinge, aporta só o que falta; depois, zero. Se o valor atual já cobre o alvo, nunca aporta.</li>
 *   <li>Sem alvo ({@code valorAlvo = 0}): aporta o aporte mensal todo mês (reserva contínua), sem previsão.</li>
 *   <li>Aporte mensal 0: nunca aporta; sem previsão (a menos que o alvo já esteja atingido).</li>
 * </ul>
 * O primeiro mês (índice 0) já tem aporte: o guardado no início dele é o {@code valorAtual}.
 */
public final class CalculadoraAportes {
    private CalculadoraAportes() {}

    /** Aporte do mês a partir do saldo guardado no início do mês. */
    public static BigDecimal aporte(MetaCalculo meta, BigDecimal guardadoAntes) {
        BigDecimal mensal = meta.aporteMensal();
        if (mensal.signum() <= 0) return Dinheiro.ZERO;
        if (meta.semAlvo()) return Dinheiro.arredondar(mensal);
        BigDecimal falta = meta.valorAlvo().subtract(guardadoAntes);
        if (falta.signum() <= 0) return Dinheiro.ZERO;
        return Dinheiro.arredondar(Dinheiro.min(mensal, falta));
    }

    /** Aplica um mês a todas as metas: devolve o aporte e o novo guardado de cada uma, na mesma ordem. */
    public static List<AporteMeta> aportarMes(List<MetaCalculo> metas, List<BigDecimal> guardadoAntes) {
        List<AporteMeta> r = new ArrayList<>(metas.size());
        for (int i = 0; i < metas.size(); i++) {
            MetaCalculo m = metas.get(i);
            BigDecimal antes = guardadoAntes.get(i);
            BigDecimal ap = aporte(m, antes);
            r.add(new AporteMeta(m.id(), m.nome(), ap, Dinheiro.arredondar(antes.add(ap))));
        }
        return r;
    }

    /**
     * Mês com reserva extra vinda do plano de uso do 13º/PLR ({@code extras}, na mesma ordem das metas): o extra
     * entra <b>antes</b> do aporte mensal, que é calculado sobre o guardado já com o extra (aporta só o que falta,
     * ou zero se o extra já cobriu o alvo). O extra não é limitado ao alvo e antecipa a conclusão da meta.
     */
    public static List<AporteMeta> aportarMes(List<MetaCalculo> metas, List<BigDecimal> guardadoAntes,
                                              List<BigDecimal> extras) {
        List<AporteMeta> r = new ArrayList<>(metas.size());
        for (int i = 0; i < metas.size(); i++) {
            MetaCalculo m = metas.get(i);
            BigDecimal extra = Dinheiro.arredondar(extras.get(i));
            BigDecimal antes = Dinheiro.arredondar(guardadoAntes.get(i).add(extra));
            BigDecimal ap = aporte(m, antes);
            r.add(new AporteMeta(m.id(), m.nome(), ap, Dinheiro.arredondar(antes.add(ap)), extra));
        }
        return r;
    }

    /** Saldo guardado previsto da meta ao fim de {@code meses} meses de aportes (o mesmo passo da projeção). */
    public static BigDecimal guardadoApos(MetaCalculo meta, int meses) {
        BigDecimal g = Dinheiro.arredondar(meta.valorAtual());
        for (int i = 0; i < meses; i++) {
            g = Dinheiro.arredondar(g.add(aporte(meta, g)));
        }
        return g;
    }

    /** Saldos guardados no início do primeiro mês (os valores atuais). */
    public static List<BigDecimal> guardadoInicial(List<MetaCalculo> metas) {
        return metas.stream().map(m -> Dinheiro.arredondar(m.valorAtual())).toList();
    }

    /**
     * Quantos aportes mensais faltam para atingir o alvo, contando o primeiro mês: 0 = já atingida;
     * vazio = sem alvo ou sem aporte. Equivale a repetir {@link #aporte} até o guardado chegar ao alvo
     * ({@code ceil(falta / aporte mensal)}, o último aporte sendo parcial).
     */
    public static Optional<Integer> aportesParaAtingir(MetaCalculo meta) {
        if (meta.semAlvo()) return Optional.empty();
        BigDecimal falta = meta.valorAlvo().subtract(meta.valorAtual());
        if (falta.signum() <= 0) return Optional.of(0);
        if (meta.aporteMensal().signum() <= 0) return Optional.empty();
        return Optional.of(falta.divide(meta.aporteMensal(), 0, RoundingMode.CEILING).intValueExact());
    }

    /**
     * Mês em que a meta atinge o alvo, com o primeiro aporte em {@code inicio}: o mês do último aporte
     * ({@code inicio + n − 1}); {@code inicio} se já atingida; vazio se sem alvo ou sem aporte.
     */
    public static Optional<YearMonth> previsao(MetaCalculo meta, YearMonth inicio) {
        return aportesParaAtingir(meta).map(n -> n == 0 ? inicio : inicio.plusMonths(n - 1L));
    }
}
