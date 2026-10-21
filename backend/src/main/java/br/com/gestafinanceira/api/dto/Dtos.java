package br.com.gestafinanceira.api.dto;

import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.saude.Faixas;
import br.com.gestafinanceira.domain.calculo.saude.StatusFaixa;
import br.com.gestafinanceira.domain.modelo.GrupoGasto;
import br.com.gestafinanceira.domain.modelo.Saude;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Contratos da API REST. Meses de 13º/férias são 1..12; competências são "AAAA-MM". */
public final class Dtos {
    private Dtos() {}

    // ---- perfil -------------------------------------------------------------------------------

    public record PerfilDto(
            @NotNull Long cargoId,
            @Min(0) int delta,
            @Min(0) @Max(31) int diasSobreaviso,
            @Min(0) int filhos,
            boolean homeOffice,
            boolean vrVa,
            @NotNull Saude saude,
            @Min(0) int dependentes,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal funcefPct,
            boolean valeTransporte,
            boolean sindicato,
            @NotNull BigDecimal saldoInicial,
            @Size(max = 60) String nome,
            @DecimalMin("0") BigDecimal saldoFuncef) {
        /** Aparado antes da validação de tamanho; nulo (campo ausente) fica nulo e o serviço mantém o atual. */
        public PerfilDto {
            if (nome != null) {
                nome = nome.strip();
            }
        }
    }

    public record FeriasDto(boolean ativo, @Min(1) @Max(12) int mes, boolean abono) {
    }

    // ---- lançamentos / gastos -----------------------------------------------------------------

    public record LancamentoDto(
            Long id,
            @NotBlank @Size(max = 160) String descricao,
            @NotBlank @Size(max = 60) String categoria,
            @NotNull TipoLancamento tipo,
            @NotNull @DecimalMin("0") BigDecimal valor,
            @NotNull YearMonth mesInicial,
            @Min(0) int parcelas) {
    }

    public record CategoriaDto(@NotBlank @Size(max = 60) String nome, int ordem, GrupoGasto grupo) {
    }

    // ---- 13º ----------------------------------------------------------------------------------

    /** Parcela fixa do 13º (somente leitura): mês 1..12 e percentual do salário base. */
    public record ParcelaDecimoDto(int mes, BigDecimal percentual) {
    }

    /** Saída de {@code GET/PUT /api/decimo}: flag {@code ativo} e parcelas fixas (fev 50% / nov 50%). */
    public record DecimoDto(boolean ativo, List<ParcelaDecimoDto> parcelas, BigDecimal totalPercentual) {
    }

    /** Entrada de {@code PUT /api/decimo}: só a flag. */
    public record DecimoAtivoDto(@NotNull Boolean ativo) {
    }

    // ---- PLR ----------------------------------------------------------------------------------

    /**
     * Parcela do PLR. Entrada: {@code mes}, {@code percentual} e {@code valor} (bruto, opcional, substitui o calculado).
     * Saída: também {@code valorBruto}, {@code desconto} (IR exclusivo) e {@code valorLiquido}, calculados (ignorados na entrada).
     */
    public record ParcelaPlrDto(
            @Min(1) @Max(12) int mes,
            @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal percentual,
            @DecimalMin("0") BigDecimal valor,
            BigDecimal valorBruto, BigDecimal desconto, BigDecimal valorLiquido) {
    }

    /**
     * PLR. Entrada (PUT): {@code ativo} (null = mantém) e {@code parcelas} (null/vazia ao ativar = padrão set 50% + mar 50%).
     * {@code estimativaAnual} é o PLR anual do cargo do perfil (só leitura).
     */
    public record PlrDto(Boolean ativo, @Valid List<ParcelaPlrDto> parcelas, BigDecimal totalPercentual, String aviso,
                         BigDecimal estimativaAnual) {
    }

    // ---- bônus Caixa ---------------------------------------------------------------------------

    public record BonusDto(boolean ativo, @Min(1) @Max(12) int mes, @NotNull @DecimalMin("0") BigDecimal salarios) {
    }

    // ---- metas --------------------------------------------------------------------------------

    /**
     * {@code banco}: onde o dinheiro está guardado (texto livre, opcional); aparado antes da validação de tamanho,
     * vazio/branco vira {@code null}. Ausente no PUT também grava {@code null} (a meta é substituída inteira).
     */
    public record MetaEntradaDto(
            @NotBlank @Size(max = 120) String nome,
            @NotNull @DecimalMin("0") BigDecimal valorAlvo,
            @NotNull @DecimalMin("0") BigDecimal valorAtual,
            @NotNull @DecimalMin("0") BigDecimal aporteMensal,
            @Size(max = 80) String banco) {
        public MetaEntradaDto {
            if (banco != null) {
                banco = banco.strip();
                if (banco.isEmpty()) banco = null;
            }
        }

        public MetaEntradaDto(String nome, BigDecimal valorAlvo, BigDecimal valorAtual, BigDecimal aporteMensal) {
            this(nome, valorAlvo, valorAtual, aporteMensal, null);
        }
    }

    /**
     * {@code mesesParaAtingir}: quantidade de aportes mensais que faltam, contando o mês atual (0 = atingida;
     * nulo = sem alvo ou sem aporte). {@code previsao}: mês do último aporte (mês atual + n − 1), igual ao mês
     * em que a projeção atinge o alvo. {@code guardadoFimProjecao}: saldo previsto da meta em {@code fimProjecao}
     * (último dos 12 meses da projeção a partir do mês atual).
     */
    public record MetaDto(
            Long id,
            String nome,
            BigDecimal valorAlvo,
            BigDecimal valorAtual,
            BigDecimal aporteMensal,
            String banco,
            BigDecimal falta,
            BigDecimal percentual,
            Integer mesesParaAtingir,
            YearMonth previsao,
            BigDecimal guardadoFimProjecao,
            YearMonth fimProjecao) {
    }

    // ---- projeção -----------------------------------------------------------------------------

    public record DestaqueMesDto(YearMonth competencia, BigDecimal sobra) {
    }

    /**
     * {@code sobra} (soma do período) já desconta os aportes; {@code saldoFinal} é o disponível em conta no último
     * mês. {@code aportes}: total aportado em metas no período; {@code guardadoInicial}/{@code guardadoFinal}: soma
     * dos saldos das metas hoje e ao fim do último mês; {@code patrimonioFinal} = saldoFinal + guardadoFinal.
     * {@code previdenciaInicial}/{@code previdenciaFinal}: saldo FUNCEF hoje e ao fim do último mês;
     * {@code patrimonioTotalFinal} = patrimonioFinal + previdenciaFinal.
     */
    public record ResumoProjecaoDto(
            BigDecimal receitas, BigDecimal descontos, BigDecimal liquido,
            BigDecimal gastos, BigDecimal sobra, BigDecimal saldoFinal,
            BigDecimal variacaoSaldo, BigDecimal sobraMedia, BigDecimal liquidoMedio, BigDecimal gastosMedio,
            DestaqueMesDto melhorMes, DestaqueMesDto piorMes, List<YearMonth> mesesNegativos,
            BigDecimal maiorSobraAbsoluta, BigDecimal aportes, BigDecimal guardadoInicial, BigDecimal guardadoFinal,
            BigDecimal patrimonioFinal, BigDecimal previdenciaInicial, BigDecimal previdenciaFinal,
            BigDecimal patrimonioTotalFinal) {
    }

    public record CategoriaResumoDto(String categoria, BigDecimal mediaMensal) {
    }

    public record ResumoGastosDto(YearMonth inicio, BigDecimal mediaMensalTotal, List<CategoriaResumoDto> categorias) {
    }

    /** Parâmetros vigentes em uma data (cargos, deltas, tabelas e benefícios), para as telas de perfil e parâmetros. */
    public record ParametrosVigentesDto(
            LocalDate data, List<CargoDto> cargos, TabelaDeltasDto deltas, TabelaFaixasDto inss,
            TabelaFaixasDto irpf, BeneficioDto beneficios) {
    }

    public record ProjecaoDto(YearMonth inicio, BigDecimal saldoInicial, List<MesProjecao> meses, ResumoProjecaoDto resumo) {
    }

    // ---- parâmetros ---------------------------------------------------------------------------

    public record CargoDto(
            Long id,
            @NotBlank @Size(max = 120) String nome,
            @Min(6) @Max(8) int jornada,
            boolean usaDelta,
            int ordem,
            @NotNull @DecimalMin("0") BigDecimal piso,
            @NotNull @DecimalMin("0") BigDecimal gratificacao,
            LocalDate vigenciaInicio,
            @DecimalMin("0") BigDecimal plrAnual) {
    }

    public record FaixaDto(BigDecimal ate, @NotNull @DecimalMin("0") BigDecimal aliquota, @NotNull @DecimalMin("0") BigDecimal deducao) {
    }

    public record TabelaFaixasDto(@NotNull LocalDate vigenciaInicio, LocalDate vigenciaFim, @NotEmpty @Valid List<FaixaDto> faixas) {
    }

    public record DeltaDto(int referencia, @NotNull @DecimalMin("0") BigDecimal valor) {
    }

    public record TabelaDeltasDto(@NotNull LocalDate vigenciaInicio, LocalDate vigenciaFim, @NotEmpty @Valid List<DeltaDto> deltas) {
    }

    public record BeneficioDto(
            @NotNull LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            @NotNull BigDecimal pai,
            @NotNull BigDecimal homeOffice,
            @NotNull BigDecimal vrVa,
            @NotNull BigDecimal vtPercentual,
            @NotNull BigDecimal sindicatoPercentual,
            @NotNull BigDecimal sindicatoTeto,
            @NotNull BigDecimal saudePercentual,
            @NotNull BigDecimal saudeTetoPercentual,
            @NotNull BigDecimal planoPorDependente,
            @NotNull BigDecimal inssTeto,
            @NotNull BigDecimal inssTetoSalario,
            @NotNull BigDecimal irDeducaoMinima,
            @NotNull BigDecimal irLimiteIsencao,
            @NotNull BigDecimal irLimiteReducao,
            @NotNull BigDecimal irReducaoA,
            @NotNull BigDecimal irReducaoB,
            @Min(1) int divisorJornada6h,
            @Min(1) int divisorJornada8h,
            @NotNull @DecimalMin("0") BigDecimal bonusCaixaMaxSalarios) {
    }

    // ---- saúde financeira (método 50/30/20) ---------------------------------------------------

    public record ParticipaDto(@NotNull Boolean participa) {
    }

    public record GrupoDto(@NotNull GrupoGasto grupo) {
    }

    public record CategoriaSaudeDto(String nome, GrupoGasto grupo, BigDecimal mediaMensal) {
    }

    public record SaudeFinanceiraDto(
            boolean participa, BigDecimal rendaMensal, Faixas<BigDecimal> recomendado, Faixas<BigDecimal> atual,
            Faixas<BigDecimal> percentuais, Faixas<BigDecimal> diferenca, Faixas<StatusFaixa> status,
            BigDecimal sobraLivre, BigDecimal aporteSugerido, List<CategoriaSaudeDto> categorias,
            List<String> diagnostico, PoupancaDetalhe poupancaDetalhe) {
    }

    /** Composição da poupança mensal: média dos aportes em metas e contribuição FUNCEF do participante. */
    public record PoupancaDetalhe(BigDecimal metas, BigDecimal funcef) {
    }

    // ---- agregado -----------------------------------------------------------------------------

    public record DadosDto(
            PerfilDto perfil, FeriasDto ferias, List<LancamentoDto> lancamentos,
            DecimoDto decimo, List<MetaDto> metas, BonusDto bonus, PlrDto plr, SaudeFinanceiraDto saudeFinanceira) {
    }

    public record ErroDto(int status, String erro, String mensagem, List<String> detalhes) {
    }
}
