package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.domain.modelo.Saude;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
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

/** Entidades JPA (mapeamento 1:1 das tabelas criadas pelo Flyway). */
public final class Entidades {
    private Entidades() {}

    @Entity(name = "Cargo")
    @Table(name = "cargo")
    public static class Cargo {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public String nome;
        public short jornada;
        @Column(name = "usa_delta") public boolean usaDelta;
        public int ordem;
    }

    @Entity(name = "CargoValor")
    @Table(name = "cargo_valor")
    public static class CargoValor {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(name = "cargo_id") public Long cargoId;
        public BigDecimal piso;
        public BigDecimal gratificacao;
        @Column(name = "plr_anual") public BigDecimal plrAnual = BigDecimal.ZERO;
        @Column(name = "vigencia_inicio") public LocalDate vigenciaInicio;
        @Column(name = "vigencia_fim") public LocalDate vigenciaFim;
    }

    @Entity(name = "DeltaSalarial")
    @Table(name = "delta_salarial")
    public static class DeltaSalarial {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public int referencia;
        public BigDecimal valor;
        @Column(name = "vigencia_inicio") public LocalDate vigenciaInicio;
        @Column(name = "vigencia_fim") public LocalDate vigenciaFim;
    }

    @Entity(name = "InssFaixa")
    @Table(name = "tabela_inss_faixa")
    public static class InssFaixa {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public BigDecimal ate;
        public BigDecimal aliquota;
        public BigDecimal deducao;
        @Column(name = "vigencia_inicio") public LocalDate vigenciaInicio;
        @Column(name = "vigencia_fim") public LocalDate vigenciaFim;
    }

    @Entity(name = "IrpfFaixa")
    @Table(name = "tabela_irpf_faixa")
    public static class IrpfFaixa {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public BigDecimal ate;
        public BigDecimal aliquota;
        public BigDecimal deducao;
        @Column(name = "vigencia_inicio") public LocalDate vigenciaInicio;
        @Column(name = "vigencia_fim") public LocalDate vigenciaFim;
    }

    @Entity(name = "PlrFaixa")
    @Table(name = "tabela_plr_faixa")
    public static class PlrFaixa {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public BigDecimal ate;
        public BigDecimal aliquota;
        public BigDecimal deducao;
        @Column(name = "vigencia_inicio") public LocalDate vigenciaInicio;
        @Column(name = "vigencia_fim") public LocalDate vigenciaFim;
    }

    @Entity(name = "ParametroBeneficio")
    @Table(name = "parametro_beneficio")
    public static class ParametroBeneficio {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(name = "vigencia_inicio") public LocalDate vigenciaInicio;
        @Column(name = "vigencia_fim") public LocalDate vigenciaFim;
        public BigDecimal pai;
        @Column(name = "home_office") public BigDecimal homeOffice;
        @Column(name = "vr_va") public BigDecimal vrVa;
        @Column(name = "vt_percentual") public BigDecimal vtPercentual;
        @Column(name = "sindicato_percentual") public BigDecimal sindicatoPercentual;
        @Column(name = "sindicato_teto") public BigDecimal sindicatoTeto;
        @Column(name = "saude_percentual") public BigDecimal saudePercentual;
        @Column(name = "saude_teto_percentual") public BigDecimal saudeTetoPercentual;
        @Column(name = "plano_por_dependente") public BigDecimal planoPorDependente;
        @Column(name = "inss_teto") public BigDecimal inssTeto;
        @Column(name = "inss_teto_salario") public BigDecimal inssTetoSalario;
        @Column(name = "ir_deducao_minima") public BigDecimal irDeducaoMinima;
        @Column(name = "ir_limite_isencao") public BigDecimal irLimiteIsencao;
        @Column(name = "ir_limite_reducao") public BigDecimal irLimiteReducao;
        @Column(name = "ir_reducao_a") public BigDecimal irReducaoA;
        @Column(name = "ir_reducao_b") public BigDecimal irReducaoB;
        @Column(name = "divisor_jornada_6h") public int divisorJornada6h;
        @Column(name = "divisor_jornada_8h") public int divisorJornada8h;
        @Column(name = "bonus_caixa_max_salarios") public BigDecimal bonusCaixaMaxSalarios;
    }

    @Entity(name = "Perfil")
    @Table(name = "perfil")
    public static class Perfil {
        @Id public Short id = 1;
        @Column(name = "cargo_id") public Long cargoId;
        public int delta;
        @Column(name = "dias_sobreaviso") public int diasSobreaviso;
        public int filhos;
        @Column(name = "home_office") public boolean homeOffice;
        @Column(name = "vr_va") public boolean vrVa;
        @Enumerated(EnumType.STRING) public Saude saude;
        public int dependentes;
        @Column(name = "funcef_pct") public BigDecimal funcefPct;
        @Column(name = "vale_transporte") public boolean valeTransporte;
        public boolean sindicato;
        @Column(name = "saldo_inicial") public BigDecimal saldoInicial;
        @Column(name = "ferias_ativo") public boolean feriasAtivo;
        @Column(name = "ferias_mes") public short feriasMes;
        @Column(name = "ferias_abono") public boolean feriasAbono;
        @Column(name = "bonus_ativo") public boolean bonusAtivo;
        @Column(name = "bonus_mes") public short bonusMes = 12;
        @Column(name = "bonus_salarios") public BigDecimal bonusSalarios = BigDecimal.ZERO;
        @Column(name = "plr_ativo") public boolean plrAtivo;
        public String nome;
        @Column(name = "atualizado_em") public LocalDateTime atualizadoEm = LocalDateTime.now();
    }

    @Entity(name = "Categoria")
    @Table(name = "categoria")
    public static class Categoria {
        @Id public String nome;
        public int ordem;
    }

    @Entity(name = "Lancamento")
    @Table(name = "lancamento")
    public static class Lancamento {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public String descricao;
        public String categoria;
        @Enumerated(EnumType.STRING) public TipoLancamento tipo;
        public BigDecimal valor;
        @Column(name = "mes_inicial") public LocalDate mesInicial;
        public int parcelas;
        @Column(name = "criado_em") public LocalDateTime criadoEm = LocalDateTime.now();
        /** Soft delete: não nulo = excluído (fica no banco, mas some de leituras e cálculos). */
        @Column(name = "deletado_em") public LocalDateTime deletadoEm;
        /** 'USUARIO' ou 'EXPIRADO'. */
        @Column(name = "motivo_exclusao") public String motivoExclusao;
    }

    @Entity(name = "EventoSalarial")
    @Table(name = "evento_salarial")
    public static class EventoSalarial {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public String tipo = "DECIMO_TERCEIRO";
        public short mes;
        public BigDecimal percentual;
        public BigDecimal valor;
    }

    @Entity(name = "Meta")
    @Table(name = "meta")
    public static class Meta {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public String nome;
        @Column(name = "valor_alvo") public BigDecimal valorAlvo;
        @Column(name = "valor_atual") public BigDecimal valorAtual;
        @Column(name = "aporte_mensal") public BigDecimal aporteMensal;
    }
}
