package br.com.gestafinanceira.domain.modelo;

import java.math.BigDecimal;

/**
 * Meta (reserva) vista pelo motor de cálculo. {@code valorAtual} é o saldo já guardado no início do primeiro
 * mês da projeção; {@code valorAlvo = 0} significa "sem alvo" (aporta todo mês, sem previsão de conclusão).
 */
public record MetaCalculo(Long id, String nome, BigDecimal valorAlvo, BigDecimal valorAtual, BigDecimal aporteMensal) {

    public boolean semAlvo() {
        return valorAlvo.signum() == 0;
    }
}
