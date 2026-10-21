package br.com.gestafinanceira.domain.calculo.adicionais;

import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.domain.modelo.TipoDestino;
import java.math.BigDecimal;
import java.util.List;

/**
 * Divisão do líquido de uma parcela de adicional recebida no mês: {@code liquido} = soma dos destinos +
 * {@code semDestino} (o que não tem destino definido fica no saldo disponível).
 */
public record AdicionalMes(EventoAdicional evento, BigDecimal liquido, List<DestinoValor> destinos,
                           BigDecimal semDestino) {

    public AdicionalMes {
        destinos = List.copyOf(destinos);
    }

    /** Soma dos destinos do tipo informado. */
    public BigDecimal total(TipoDestino tipo) {
        return destinos.stream().filter(d -> d.tipo() == tipo).map(DestinoValor::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
