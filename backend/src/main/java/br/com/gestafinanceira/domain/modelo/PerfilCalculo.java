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
        Bonus bonus) {

    public PerfilCalculo(int delta, int diasSobreaviso, int filhos, boolean homeOffice, boolean vrVa, Saude saude,
                         int dependentes, BigDecimal funcefPct, boolean valeTransporte, boolean sindicato,
                         BigDecimal saldoInicial, Ferias ferias) {
        this(delta, diasSobreaviso, filhos, homeOffice, vrVa, saude, dependentes, funcefPct, valeTransporte,
                sindicato, saldoInicial, ferias, Bonus.INATIVO);
    }
}
