package br.com.gestafinanceira.domain.modelo;

/**
 * Destino de uma fatia de um adicional: {@code LAZER} (desejo, sai da conta), {@code OBRIGATORIO} (necessidade,
 * sai da conta) ou {@code RESERVA} (sai da conta e entra em guardado: numa meta ou na reserva dos adicionais).
 */
public enum TipoDestino { LAZER, OBRIGATORIO, RESERVA }
