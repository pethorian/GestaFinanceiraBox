package br.com.gestafinanceira.api.dto;

import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
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
            @NotNull BigDecimal saldoInicial) {
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

    public record CategoriaDto(@NotBlank @Size(max = 60) String nome, int ordem) {
    }

    // ---- 13º ----------------------------------------------------------------------------------

    public record ParcelaDecimoDto(
            @Min(1) @Max(12) int mes,
            @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal percentual) {
    }

    public record DecimoDto(@NotNull @Valid List<ParcelaDecimoDto> parcelas, BigDecimal totalPercentual, String aviso) {
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

    public record MetaEntradaDto(
            @NotBlank @Size(max = 120) String nome,
            @NotNull @DecimalMin("0") BigDecimal valorAlvo,
            @NotNull @DecimalMin("0") BigDecimal valorAtual,
            @NotNull @DecimalMin("0") BigDecimal aporteMensal) {
    }

    public record MetaDto(
            Long id,
            String nome,
            BigDecimal valorAlvo,
            BigDecimal valorAtual,
            BigDecimal aporteMensal,
            BigDecimal falta,
            BigDecimal percentual,
            Integer mesesParaAtingir,
            YearMonth previsao) {
    }

    // ---- projeção -----------------------------------------------------------------------------

    public record DestaqueMesDto(YearMonth competencia, BigDecimal sobra) {
    }

    public record ResumoProjecaoDto(
            BigDecimal receitas, BigDecimal descontos, BigDecimal liquido,
            BigDecimal gastos, BigDecimal sobra, BigDecimal saldoFinal,
            BigDecimal variacaoSaldo, BigDecimal sobraMedia, BigDecimal liquidoMedio, BigDecimal gastosMedio,
            DestaqueMesDto melhorMes, DestaqueMesDto piorMes, List<YearMonth> mesesNegativos,
            BigDecimal maiorSobraAbsoluta) {
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

    // ---- agregado -----------------------------------------------------------------------------

    public record DadosDto(
            PerfilDto perfil, FeriasDto ferias, List<LancamentoDto> lancamentos,
            DecimoDto decimo, List<MetaDto> metas, BonusDto bonus, PlrDto plr) {
    }

    public record ErroDto(int status, String erro, String mensagem, List<String> detalhes) {
    }
}
