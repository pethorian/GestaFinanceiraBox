package br.com.gestafinanceira.domain.calculo.adicionais;

import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.domain.modelo.TipoDestino;
import java.math.BigDecimal;
import java.util.Collection;

/** Totais de parcelas de adicionais por tipo de destino: liquido = lazer + obrigatorio + reserva + semDestino. */
public record TotaisAdicionais(BigDecimal liquido, BigDecimal lazer, BigDecimal obrigatorio, BigDecimal reserva,
                               BigDecimal semDestino) {

    /** Soma todas as parcelas informadas. */
    public static TotaisAdicionais somar(Collection<AdicionalMes> parcelas) {
        BigDecimal liq = BigDecimal.ZERO, laz = BigDecimal.ZERO, obr = BigDecimal.ZERO, res = BigDecimal.ZERO,
                sem = BigDecimal.ZERO;
        for (AdicionalMes a : parcelas) {
            liq = liq.add(a.liquido());
            laz = laz.add(a.total(TipoDestino.LAZER));
            obr = obr.add(a.total(TipoDestino.OBRIGATORIO));
            res = res.add(a.total(TipoDestino.RESERVA));
            sem = sem.add(a.semDestino());
        }
        return new TotaisAdicionais(Dinheiro.arredondar(liq), Dinheiro.arredondar(laz), Dinheiro.arredondar(obr),
                Dinheiro.arredondar(res), Dinheiro.arredondar(sem));
    }

    /** Só as parcelas do evento informado. */
    public static TotaisAdicionais somar(Collection<AdicionalMes> parcelas, EventoAdicional evento) {
        return somar(parcelas.stream().filter(a -> a.evento() == evento).toList());
    }
}
