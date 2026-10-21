package br.com.gestafinanceira.domain.modelo;

import java.math.BigDecimal;

/** Dados do perfil necessários ao cálculo (sem referência a persistência). */
public record PerfilCalculo(
        int delta,
        int diasSobreaviso,
        int filhos,
        boolean homeOffice,
        boolean vrVa,
        Saude saude,
        int dependentes,
        BigDecimal funcefPct,
        boolean valeTransporte,
        boolean sindicato,
        BigDecimal saldoInicial,
        Ferias ferias,
        Bonus bonus,
        BigDecimal saldoFuncef) {

    /** Saldo FUNCEF ausente equivale a zero. */
    public PerfilCalculo {
        if (saldoFuncef == null) saldoFuncef = BigDecimal.ZERO;
    }

    public PerfilCalculo(int delta, int diasSobreaviso, int filhos, boolean homeOffice, boolean vrVa, Saude saude,
                         int dependentes, BigDecimal funcefPct, boolean valeTransporte, boolean sindicato,
                         BigDecimal saldoInicial, Ferias ferias, Bonus bonus) {
        this(delta, diasSobreaviso, filhos, homeOffice, vrVa, saude, dependentes, funcefPct, valeTransporte,
                sindicato, saldoInicial, ferias, bonus, BigDecimal.ZERO);
    }

    public PerfilCalculo(int delta, int diasSobreaviso, int filhos, boolean homeOffice, boolean vrVa, Saude saude,
                         int dependentes, BigDecimal funcefPct, boolean valeTransporte, boolean sindicato,
                         BigDecimal saldoInicial, Ferias ferias) {
        this(delta, diasSobreaviso, filhos, homeOffice, vrVa, saude, dependentes, funcefPct, valeTransporte,
                sindicato, saldoInicial, ferias, Bonus.INATIVO, BigDecimal.ZERO);
    }
}
