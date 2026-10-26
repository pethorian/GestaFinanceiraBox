package br.com.gestafinanceira.domain.calculo.plr;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.modelo.TipoDestino;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controle do PLR recebido: divide o total já recebido entre os destinos do plano de uso ("envelopes") e confronta
 * com os gastos feitos. Cálculo puro; não altera a projeção.
 */
public final class ControlePlr {
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private ControlePlr() {}

    public record DestinoEntrada(Long id, String nome, TipoDestino tipo, BigDecimal percentual) {
    }

    /** {@code destinoId} nulo (ou que não conste nos destinos) = gasto sem destino. */
    public record GastoEntrada(Long destinoId, BigDecimal valor) {
    }

    public record Entrada(List<BigDecimal> recebimentos, List<DestinoEntrada> destinos, List<GastoEntrada> gastos) {
    }

    /** {@code saldo} negativo = estourou o destino; {@code percentualUsado} = gasto / alocado × 100 (2 casas). */
    public record Destino(Long id, String nome, TipoDestino tipo, BigDecimal percentual, BigDecimal alocado,
                          BigDecimal gasto, BigDecimal saldo, BigDecimal percentualUsado) {
    }

    /** Parte do recebido não destinada a nenhum envelope (nunca negativa) e os gastos sem destino. */
    public record SemDestino(BigDecimal alocado, BigDecimal gasto, BigDecimal saldo, BigDecimal percentualUsado) {
    }

    public record Resumo(BigDecimal recebido, BigDecimal gasto, BigDecimal saldo, List<Destino> destinos,
                         SemDestino semDestino) {
    }

    public static Resumo calcular(Entrada e) {
        BigDecimal recebido = Dinheiro.ZERO;
        for (BigDecimal v : e.recebimentos()) recebido = recebido.add(Dinheiro.arredondar(v));

        Set<Long> ids = new HashSet<>();
        for (DestinoEntrada d : e.destinos()) ids.add(d.id());

        Map<Long, BigDecimal> gastoPorDestino = new HashMap<>();
        BigDecimal gastoSem = Dinheiro.ZERO;
        BigDecimal gastoTotal = Dinheiro.ZERO;
        for (GastoEntrada g : e.gastos()) {
            BigDecimal v = Dinheiro.arredondar(g.valor());
            gastoTotal = gastoTotal.add(v);
            if (g.destinoId() != null && ids.contains(g.destinoId())) gastoPorDestino.merge(g.destinoId(), v, BigDecimal::add);
            else gastoSem = gastoSem.add(v);
        }

        List<Destino> destinos = new ArrayList<>();
        BigDecimal somaAlocado = Dinheiro.ZERO;
        for (DestinoEntrada d : e.destinos()) {
            BigDecimal alocado = Dinheiro.percentual(recebido, d.percentual());
            BigDecimal gasto = gastoPorDestino.getOrDefault(d.id(), Dinheiro.ZERO);
            somaAlocado = somaAlocado.add(alocado);
            destinos.add(new Destino(d.id(), d.nome(), d.tipo(), d.percentual(), alocado, gasto,
                    alocado.subtract(gasto), usado(gasto, alocado)));
        }
        BigDecimal alocadoSem = Dinheiro.max(Dinheiro.ZERO, recebido.subtract(somaAlocado));
        SemDestino sem = new SemDestino(alocadoSem, gastoSem, alocadoSem.subtract(gastoSem), usado(gastoSem, alocadoSem));
        return new Resumo(recebido, gastoTotal, recebido.subtract(gastoTotal), List.copyOf(destinos), sem);
    }

    private static BigDecimal usado(BigDecimal gasto, BigDecimal alocado) {
        if (alocado.signum() == 0) return Dinheiro.ZERO;
        return gasto.multiply(CEM).divide(alocado, 2, RoundingMode.HALF_UP);
    }
}
