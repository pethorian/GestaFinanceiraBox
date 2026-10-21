package br.com.gestafinanceira.domain.calculo.adicionais;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.modelo.DestinoAdicional;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Plano de uso do 13º e do PLR: divide o líquido de uma parcela entre os destinos do evento.
 *
 * <p>Cada destino vale {@code líquido × percentual ÷ 100}, HALF_UP com 2 casas; o resíduo de centavos vai para o
 * último destino, de modo que a soma dos destinos feche exatamente em {@code líquido × (soma dos %) ÷ 100}
 * arredondado. {@code semDestino} = líquido − soma dos destinos (100% definidos → 0). Líquido ≤ 0: nada é
 * dividido (destinos zerados, tudo "sem destino"). Caso degenerado (centavos tão poucos que o último ficaria
 * negativo): usa o arredondamento acumulado, que mantém todos os valores ≥ 0 e a mesma soma.
 */
public final class CalculadoraDestinos {
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculadoraDestinos() {}

    /** Divide o líquido entre os destinos do {@code evento} (os de outros eventos são ignorados), na ordem dada. */
    public static AdicionalMes dividir(EventoAdicional evento, BigDecimal liquido, List<DestinoAdicional> destinos) {
        BigDecimal l = Dinheiro.arredondar(liquido);
        List<DestinoAdicional> doEvento = destinos.stream().filter(d -> d.evento() == evento).toList();
        List<DestinoValor> r = new ArrayList<>(doEvento.size());
        if (doEvento.isEmpty() || l.signum() <= 0) {
            for (DestinoAdicional d : doEvento) r.add(valor(d, Dinheiro.ZERO));
            return new AdicionalMes(evento, l, r, l);
        }
        BigDecimal totalPct = doEvento.stream().map(DestinoAdicional::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alvo = Dinheiro.percentual(l, totalPct);
        BigDecimal somaOutros = BigDecimal.ZERO;
        for (int i = 0; i < doEvento.size() - 1; i++) {
            BigDecimal v = Dinheiro.percentual(l, doEvento.get(i).percentual());
            somaOutros = somaOutros.add(v);
            r.add(valor(doEvento.get(i), v));
        }
        BigDecimal ultimo = alvo.subtract(somaOutros);
        if (ultimo.signum() < 0) return acumulado(evento, l, doEvento);
        r.add(valor(doEvento.get(doEvento.size() - 1), ultimo));
        return new AdicionalMes(evento, l, r, l.subtract(alvo));
    }

    /** Arredondamento acumulado: valor_i = round(L × %acum_i) − round(L × %acum_(i−1)). */
    private static AdicionalMes acumulado(EventoAdicional evento, BigDecimal l, List<DestinoAdicional> doEvento) {
        List<DestinoValor> r = new ArrayList<>(doEvento.size());
        BigDecimal pct = BigDecimal.ZERO;
        BigDecimal anterior = Dinheiro.ZERO;
        for (DestinoAdicional d : doEvento) {
            pct = pct.add(d.percentual());
            BigDecimal ate = Dinheiro.percentual(l, pct);
            r.add(valor(d, ate.subtract(anterior)));
            anterior = ate;
        }
        return new AdicionalMes(evento, l, r, l.subtract(anterior));
    }

    private static DestinoValor valor(DestinoAdicional d, BigDecimal v) {
        return new DestinoValor(d.id(), d.nome(), d.tipo(), d.metaId(), d.percentual(), Dinheiro.arredondar(v));
    }
}
