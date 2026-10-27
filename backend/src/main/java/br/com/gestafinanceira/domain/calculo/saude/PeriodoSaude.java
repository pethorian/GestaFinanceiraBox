package br.com.gestafinanceira.domain.calculo.saude;

/** Base dos gastos e da poupança da saúde financeira: média dos 12 meses da projeção ou só o mês atual. */
public enum PeriodoSaude {
    MEDIA_12(12),
    MES_ATUAL(1);

    private final int meses;

    PeriodoSaude(int meses) {
        this.meses = meses;
    }

    /** Quantos meses, a partir do atual, entram na média. */
    public int meses() {
        return meses;
    }
}
