package br.com.gestafinanceira.domain.calculo.saude;

/** Um valor para cada faixa do método 50/30/20. */
public record Faixas<T>(T necessidades, T desejos, T poupanca) {
}
