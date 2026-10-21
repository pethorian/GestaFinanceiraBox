package br.com.gestafinanceira.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.gestafinanceira.domain.calculo.saude.Metodo503020;
import br.com.gestafinanceira.domain.calculo.saude.Metodo503020.Resultado;
import br.com.gestafinanceira.domain.calculo.saude.StatusFaixa;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class Metodo503020Test {
    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static Resultado calc(String renda, String nec, String des, String poup) {
        return Metodo503020.calcular(d(renda), d(nec), d(des), d(poup));
    }

    @Test
    void faixasRecomendadas() {
        Resultado r = calc("10000", "5000", "3000", "2000");
        assertEquals(d("5000.00"), r.recomendado().necessidades());
        assertEquals(d("3000.00"), r.recomendado().desejos());
        assertEquals(d("2000.00"), r.recomendado().poupanca());
        assertEquals(d("50.0"), r.percentuais().necessidades());
        assertEquals(d("30.0"), r.percentuais().desejos());
        assertEquals(d("20.0"), r.percentuais().poupanca());
    }

    @Test
    void exatamenteNaMetaTudoOkESemSobra() {
        Resultado r = calc("10000", "5000", "3000", "2000");
        assertEquals(StatusFaixa.OK, r.status().necessidades());
        assertEquals(StatusFaixa.OK, r.status().desejos());
        assertEquals(StatusFaixa.OK, r.status().poupanca());
        assertEquals(d("0.00"), r.sobraLivre());
        assertEquals(d("0.00"), r.aporteSugerido());
    }

    @Test
    void necessidadesEDesejosAcimaESPoupancaAbaixo() {
        Resultado r = calc("10000", "6200", "3500", "300");
        assertEquals(StatusFaixa.ACIMA, r.status().necessidades());
        assertEquals(StatusFaixa.ACIMA, r.status().desejos());
        assertEquals(StatusFaixa.ABAIXO, r.status().poupanca());
        assertEquals(d("1200.00"), r.diferenca().necessidades());
        assertEquals(d("-1700.00"), r.diferenca().poupanca());
        assertEquals(d("0.00"), r.sobraLivre());
        assertEquals(d("1700.00"), r.aporteSugerido());
        assertEquals(d("62.0"), r.percentuais().necessidades());
        assertTrue(r.diagnostico().get(0).contains("62%"));
        assertTrue(r.diagnostico().get(2).contains("R$ 1.700,00"));
    }

    @Test
    void abaixoDaMetaEmNecessidadesNaoEAlerta() {
        Resultado r = calc("10000", "2000", "1000", "2500");
        assertEquals(StatusFaixa.OK, r.status().necessidades());
        assertEquals(StatusFaixa.OK, r.status().poupanca());
        assertEquals(d("0.00"), r.aporteSugerido());
        assertEquals(d("4500.00"), r.sobraLivre());
    }

    @Test
    void sobraLivreNegativaQuandoGastaMaisQueARenda() {
        Resultado r = calc("1000", "800", "400", "0");
        assertEquals(d("-200.00"), r.sobraLivre());
        assertEquals(d("200.00"), r.aporteSugerido());
        assertTrue(r.diagnostico().stream().anyMatch(s -> s.contains("passam da renda em R$ 200,00")));
    }

    @Test
    void rendaZeroSemPercentuais() {
        Resultado r = calc("0", "100", "50", "0");
        assertNull(r.percentuais());
        assertEquals(d("0.00"), r.recomendado().necessidades());
        assertEquals(d("0.00"), r.aporteSugerido());
        assertEquals(1, r.diagnostico().size());
        Resultado n = calc("-10", "0", "0", "0");
        assertNull(n.percentuais());
    }

    @Test
    void arredondamentoHalfUp() {
        // 50% de 100,01 = 50,005 -> 50,01 ; 33,3333.../100,01 -> 1 casa
        Resultado r = calc("100.01", "33.33", "0", "0");
        assertEquals(d("50.01"), r.recomendado().necessidades());
        assertEquals(d("30.00"), r.recomendado().desejos());
        assertEquals(d("20.00"), r.recomendado().poupanca());
        assertEquals(d("33.3"), r.percentuais().necessidades());
        assertEquals(d("20.00"), r.aporteSugerido());
    }
}
