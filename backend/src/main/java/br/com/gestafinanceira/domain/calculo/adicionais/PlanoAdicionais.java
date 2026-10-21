package br.com.gestafinanceira.domain.calculo.adicionais;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.modelo.DestinoAdicional;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.domain.modelo.MetaCalculo;
import br.com.gestafinanceira.domain.modelo.TipoDestino;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Aplica o plano de uso do 13º e do PLR a um mês da projeção.
 *
 * <ul>
 *   <li>Só há divisão no mês em que a parcela do evento cai e com o evento ativo (13º: líquido = parcela − desconto
 *   do 13º naquele mês; PLR: líquido da parcela).</li>
 *   <li>{@code LAZER} e {@code OBRIGATORIO} viram {@code gastos} do adicional (saída do mês).</li>
 *   <li>{@code RESERVA} vira {@code reservas} (saída do disponível): ligada a uma meta existente, soma no guardado
 *   dela (em {@code extrasPorMeta}, na ordem das metas); sem meta (ou meta inexistente), soma na reserva dos
 *   adicionais ({@code guardadoReserva}, acumulada mês a mês).</li>
 *   <li>O que não tem destino continua no saldo disponível (não sai da sobra).</li>
 * </ul>
 */
public final class PlanoAdicionais {
    private PlanoAdicionais() {}

    /** Resultado do mês. */
    public record Mes(List<AdicionalMes> adicionais, BigDecimal gastos, BigDecimal reservas,
                      List<BigDecimal> extrasPorMeta, BigDecimal guardadoReserva) {
    }

    public static Mes aplicar(List<DestinoAdicional> destinos, List<MetaCalculo> metas, boolean temDecimo,
                              BigDecimal liquidoDecimo, boolean temPlr, BigDecimal liquidoPlr,
                              BigDecimal guardadoReservaAntes) {
        List<AdicionalMes> adicionais = new ArrayList<>(2);
        if (temDecimo) adicionais.add(CalculadoraDestinos.dividir(EventoAdicional.DECIMO, liquidoDecimo, destinos));
        if (temPlr) adicionais.add(CalculadoraDestinos.dividir(EventoAdicional.PLR, liquidoPlr, destinos));

        BigDecimal gastos = Dinheiro.ZERO;
        BigDecimal reservas = Dinheiro.ZERO;
        BigDecimal reservaGeral = Dinheiro.arredondar(guardadoReservaAntes);
        List<BigDecimal> extras = new ArrayList<>(metas.size());
        for (int i = 0; i < metas.size(); i++) extras.add(Dinheiro.ZERO);
        for (AdicionalMes a : adicionais) {
            for (DestinoValor d : a.destinos()) {
                if (d.tipo() != TipoDestino.RESERVA) {
                    gastos = gastos.add(d.valor());
                    continue;
                }
                reservas = reservas.add(d.valor());
                int i = indiceMeta(metas, d.metaId());
                if (i >= 0) extras.set(i, extras.get(i).add(d.valor()));
                else reservaGeral = reservaGeral.add(d.valor());
            }
        }
        return new Mes(adicionais, Dinheiro.arredondar(gastos), Dinheiro.arredondar(reservas), extras,
                Dinheiro.arredondar(reservaGeral));
    }

    private static int indiceMeta(List<MetaCalculo> metas, Long metaId) {
        if (metaId == null) return -1;
        for (int i = 0; i < metas.size(); i++) {
            if (metaId.equals(metas.get(i).id())) return i;
        }
        return -1;
    }
}
