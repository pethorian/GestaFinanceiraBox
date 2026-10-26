package br.com.gestafinanceira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gestafinanceira.domain.calculo.plr.ControlePlr;
import br.com.gestafinanceira.domain.calculo.plr.ControlePlr.Destino;
import br.com.gestafinanceira.domain.calculo.plr.ControlePlr.DestinoEntrada;
import br.com.gestafinanceira.domain.calculo.plr.ControlePlr.Entrada;
import br.com.gestafinanceira.domain.calculo.plr.ControlePlr.GastoEntrada;
import br.com.gestafinanceira.domain.calculo.plr.ControlePlr.Resumo;
import br.com.gestafinanceira.domain.modelo.TipoDestino;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ControlePlrTest {
    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static final DestinoEntrada GASTOS = new DestinoEntrada(1L, "Gastos", TipoDestino.OBRIGATORIO, d("80"));
    private static final DestinoEntrada PESSOAL = new DestinoEntrada(2L, "Pessoal", TipoDestino.LAZER, d("20"));

    @Test
    void exemploDoUsuarioPessoalComGasto() {
        Resumo r = ControlePlr.calcular(new Entrada(List.of(d("10000")), List.of(GASTOS, PESSOAL),
                List.of(new GastoEntrada(2L, d("350")))));
        assertThat(r.recebido()).isEqualByComparingTo("10000.00");
        assertThat(r.gasto()).isEqualByComparingTo("350.00");
        assertThat(r.saldo()).isEqualByComparingTo("9650.00");
        Destino gastos = r.destinos().get(0);
        Destino pessoal = r.destinos().get(1);
        assertThat(gastos.alocado()).isEqualByComparingTo("8000.00");
        assertThat(gastos.gasto()).isEqualByComparingTo("0.00");
        assertThat(gastos.percentualUsado()).isEqualByComparingTo("0");
        assertThat(pessoal.alocado()).isEqualByComparingTo("2000.00");
        assertThat(pessoal.gasto()).isEqualByComparingTo("350.00");
        assertThat(pessoal.saldo()).isEqualByComparingTo("1650.00");
        assertThat(pessoal.percentualUsado()).isEqualByComparingTo("17.50");
        assertThat(r.semDestino().alocado()).isEqualByComparingTo("0.00");
    }

    @Test
    void somaVariosRecebimentosEArredondaAlocado() {
        // 333,33 + 0,01 = 333,34; 33,33% → 111,1022 → 111,10
        Resumo r = ControlePlr.calcular(new Entrada(List.of(d("333.33"), d("0.01")),
                List.of(new DestinoEntrada(1L, "A", TipoDestino.RESERVA, d("33.33"))), List.of()));
        assertThat(r.recebido()).isEqualByComparingTo("333.34");
        assertThat(r.destinos().get(0).alocado()).isEqualByComparingTo("111.10");
        // sobra para "sem destino": 333,34 − 111,10
        assertThat(r.semDestino().alocado()).isEqualByComparingTo("222.24");
    }

    @Test
    void arredondaMeioParaCima() {
        // 10,05 × 50% = 5,025 → 5,03
        Resumo r = ControlePlr.calcular(new Entrada(List.of(d("10.05")),
                List.of(new DestinoEntrada(1L, "A", TipoDestino.LAZER, d("50"))), List.of()));
        assertThat(r.destinos().get(0).alocado()).isEqualByComparingTo("5.03");
    }

    @Test
    void estouroDeixaSaldoNegativoEPercentualAcimaDeCem() {
        Resumo r = ControlePlr.calcular(new Entrada(List.of(d("1000")), List.of(PESSOAL),
                List.of(new GastoEntrada(2L, d("150")), new GastoEntrada(2L, d("100.50")))));
        Destino p = r.destinos().get(0);
        assertThat(p.alocado()).isEqualByComparingTo("200.00");
        assertThat(p.gasto()).isEqualByComparingTo("250.50");
        assertThat(p.saldo()).isEqualByComparingTo("-50.50");
        assertThat(p.percentualUsado()).isEqualByComparingTo("125.25");
    }

    @Test
    void semDestinoRecebeSobraEGastosSemVinculo() {
        // destinos somam 70%; gasto sem destino (nulo) e gasto com id desconhecido caem em "sem destino"
        Resumo r = ControlePlr.calcular(new Entrada(List.of(d("1000")),
                List.of(new DestinoEntrada(1L, "A", TipoDestino.LAZER, d("70"))),
                List.of(new GastoEntrada(null, d("60")), new GastoEntrada(99L, d("15")), new GastoEntrada(1L, d("10")))));
        assertThat(r.semDestino().alocado()).isEqualByComparingTo("300.00");
        assertThat(r.semDestino().gasto()).isEqualByComparingTo("75.00");
        assertThat(r.semDestino().saldo()).isEqualByComparingTo("225.00");
        assertThat(r.semDestino().percentualUsado()).isEqualByComparingTo("25.00");
        assertThat(r.gasto()).isEqualByComparingTo("85.00");
        assertThat(r.saldo()).isEqualByComparingTo("915.00");
    }

    @Test
    void semRecebimentoTudoZeroSemDividirPorZero() {
        Resumo r = ControlePlr.calcular(new Entrada(List.of(), List.of(GASTOS, PESSOAL),
                List.of(new GastoEntrada(2L, d("50")), new GastoEntrada(null, d("5")))));
        assertThat(r.recebido()).isEqualByComparingTo("0");
        assertThat(r.saldo()).isEqualByComparingTo("-55.00");
        assertThat(r.destinos().get(1).alocado()).isEqualByComparingTo("0");
        assertThat(r.destinos().get(1).percentualUsado()).isEqualByComparingTo("0");
        assertThat(r.semDestino().alocado()).isEqualByComparingTo("0");
        assertThat(r.semDestino().saldo()).isEqualByComparingTo("-5.00");
        assertThat(r.semDestino().percentualUsado()).isEqualByComparingTo("0");
    }

    @Test
    void semDestinosTodoORecebidoFicaSemDestino() {
        Resumo r = ControlePlr.calcular(new Entrada(List.of(d("500")), List.of(), List.of(new GastoEntrada(null, d("125")))));
        assertThat(r.destinos()).isEmpty();
        assertThat(r.semDestino().alocado()).isEqualByComparingTo("500.00");
        assertThat(r.semDestino().percentualUsado()).isEqualByComparingTo("25.00");
    }

    @Test
    void percentualUsadoArredondaDuasCasas() {
        // 100 / 300 = 33,333...% → 33,33
        Resumo r = ControlePlr.calcular(new Entrada(List.of(d("3000")),
                List.of(new DestinoEntrada(1L, "A", TipoDestino.LAZER, d("10"))), List.of(new GastoEntrada(1L, d("100")))));
        assertThat(r.destinos().get(0).percentualUsado()).isEqualByComparingTo("33.33");
    }
}
