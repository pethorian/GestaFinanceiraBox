package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.domain.calculo.cartao.TipoCompra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Entidades do cartão de crédito (V14). Exclusão lógica por {@code deletadoEm}. */
public final class EntidadesCartao {
    private EntidadesCartao() {}

    @Entity(name = "Cartao")
    @Table(name = "cartao")
    public static class Cartao {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public String nome;
        @Column(name = "dia_fechamento") public short diaFechamento;
        @Column(name = "dia_vencimento") public short diaVencimento;
        public BigDecimal limite;
        @Column(name = "criado_em") public LocalDateTime criadoEm = LocalDateTime.now();
        @Column(name = "deletado_em") public LocalDateTime deletadoEm;
    }

    @Entity(name = "CompraCartao")
    @Table(name = "compra_cartao")
    public static class CompraCartao {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(name = "cartao_id") public Long cartaoId;
        public String descricao;
        public String categoria;
        @Enumerated(EnumType.STRING) public TipoCompra tipo;
        @Column(name = "data_compra") public LocalDate dataCompra;
        public BigDecimal valor;
        public int parcelas;
        @Column(name = "criado_em") public LocalDateTime criadoEm = LocalDateTime.now();
        @Column(name = "deletado_em") public LocalDateTime deletadoEm;
    }
}
