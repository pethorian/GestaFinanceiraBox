package br.com.gestafinanceira.domain.modelo;

/** Agrupa os tipos de lançamento nas duas abas de gastos: fixos (todo mês e parcelados) e eventuais (uma vez). */
public enum GrupoLancamento {
    FIXOS, EVENTUAIS;

    public boolean contem(TipoLancamento tipo) {
        return switch (this) {
            case FIXOS -> tipo == TipoLancamento.RECORRENTE || tipo == TipoLancamento.PARCELADO;
            case EVENTUAIS -> tipo == TipoLancamento.PONTUAL;
        };
    }
}
