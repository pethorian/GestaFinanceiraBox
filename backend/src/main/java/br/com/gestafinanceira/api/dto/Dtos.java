package br.com.gestafinanceira.api.dto;

import br.com.gestafinanceira.domain.calculo.adicionais.DestinoValor;
import br.com.gestafinanceira.domain.calculo.adicionais.TotaisAdicionais;
import br.com.gestafinanceira.domain.calculo.cartao.TipoCompra;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.saude.Faixas;
import br.com.gestafinanceira.domain.calculo.saude.SaudeAdicionais;
import br.com.gestafinanceira.domain.calculo.saude.StatusFaixa;
import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.domain.modelo.GrupoGasto;
import br.com.gestafinanceira.domain.modelo.Saude;
import br.com.gestafinanceira.domain.modelo.TipoDestino;
import br.com.gestafinanceira.domain.modelo.TipoLancamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    // ---- cartão de crédito --------------------------------------------------------------------

    public record CartaoEntradaDto(
            @NotBlank @Size(max = 60) String nome,
            @NotNull @Min(1) @Max(31) Integer diaFechamento,
            @NotNull @Min(1) @Max(31) Integer diaVencimento,
            @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal limite) {
        public CartaoEntradaDto {
            if (nome != null) nome = nome.strip();
        }
    }

    /**
     * {@code faturaAtual}/{@code proximaFatura}: total das faturas que vencem no mês atual e no seguinte.
     * {@code comprometido}: parcelas com vencimento a partir do mês atual (sem assinaturas).
     * {@code limiteDisponivel}: limite menos comprometido (nulo sem limite).
     */
    public record CartaoDto(
            Long id, String nome, int diaFechamento, int diaVencimento, BigDecimal limite,
            BigDecimal faturaAtual, BigDecimal proximaFatura, BigDecimal comprometido, BigDecimal limiteDisponivel) {
    }

    /** {@code valor}: total da compra (assinatura: valor mensal). {@code parcelas}: 2..72 em PARCELADA, ignorado nos demais. */
    public record CompraEntradaDto(
            @NotBlank @Size(max = 160) String descricao,
            @NotBlank @Size(max = 60) String categoria,
            @NotNull TipoCompra tipo,
            @NotNull LocalDate dataCompra,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal valor,
            Integer parcelas) {
        public CompraEntradaDto {
            if (descricao != null) descricao = descricao.strip();
            if (categoria != null) categoria = categoria.strip();
        }
    }

    /** Compra com os campos calculados: parcela, primeira/última fatura (AAAA-MM; última nula em assinatura). */
    public record CompraDto(
            Long id, Long cartaoId, String descricao, String categoria, TipoCompra tipo, LocalDate dataCompra,
            BigDecimal valor, int parcelas, BigDecimal valorParcela, YearMonth primeiraFatura, YearMonth ultimaFatura,
            Integer parcelasRestantes) {
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
     * {@code gastosAdicionais}/{@code reservasAdicionais}: somas do período do plano de uso do 13º/PLR;
     * {@code adicionais}: totais por tipo de destino (o {@code guardadoFinal} inclui a reserva dos adicionais).
     */
    public record ResumoProjecaoDto(
            BigDecimal receitas, BigDecimal descontos, BigDecimal liquido,
            BigDecimal gastos, BigDecimal sobra, BigDecimal saldoFinal,
            BigDecimal variacaoSaldo, BigDecimal sobraMedia, BigDecimal liquidoMedio, BigDecimal gastosMedio,
            DestaqueMesDto melhorMes, DestaqueMesDto piorMes, List<YearMonth> mesesNegativos,
            BigDecimal maiorSobraAbsoluta, BigDecimal aportes, BigDecimal guardadoInicial, BigDecimal guardadoFinal,
            BigDecimal patrimonioFinal, BigDecimal previdenciaInicial, BigDecimal previdenciaFinal,
            BigDecimal patrimonioTotalFinal, BigDecimal gastosAdicionais, BigDecimal reservasAdicionais,
            ResumoAdicionaisDto adicionais) {
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
            List<String> diagnostico, PoupancaDetalhe poupancaDetalhe, SaudeAdicionais.Resultado adicionais) {
    }

    /** Composição da poupança mensal: média dos aportes em metas e contribuição FUNCEF do participante. */
    public record PoupancaDetalhe(BigDecimal metas, BigDecimal funcef) {
    }

    // ---- plano de uso do 13º e do PLR ---------------------------------------------------------

    /**
     * Entrada de {@code PUT /api/adicionais/{evento}/destinos} (um item da lista). Validado no serviço:
     * {@code nome} 1..60 depois de aparado, {@code tipo} obrigatório, {@code percentual} 0 &lt; p ≤ 100 (2 casas),
     * {@code metaId} opcional, só com {@code RESERVA} e de meta existente.
     */
    public record DestinoEntradaDto(Long id, String nome, TipoDestino tipo, BigDecimal percentual, Long metaId) {
    }

    /** Entrada de um recebimento de PLR (data, descrição até 120, valor > 0 com até 2 casas). */
    public record PlrRecebimentoEntradaDto(java.time.LocalDate data, String descricao, BigDecimal valor) {
    }

    public record PlrRecebimentoDto(Long id, java.time.LocalDate data, String descricao, BigDecimal valor) {
    }

    /** Entrada de um gasto do PLR; {@code destinoId} opcional (destino do plano de uso do PLR). */
    public record PlrGastoEntradaDto(java.time.LocalDate data, String descricao, BigDecimal valor, Long destinoId) {
    }

    public record PlrGastoDto(Long id, java.time.LocalDate data, String descricao, BigDecimal valor, Long destinoId,
                              String destinoNome) {
    }

    /** Resumo (cálculo do domínio, achatado) + listas por data decrescente. */
    public record PlrControleDto(BigDecimal recebido, BigDecimal gasto, BigDecimal saldo,
                                 List<br.com.gestafinanceira.domain.calculo.plr.ControlePlr.Destino> destinos,
                                 br.com.gestafinanceira.domain.calculo.plr.ControlePlr.SemDestino semDestino,
                                 List<PlrRecebimentoDto> recebimentos, List<PlrGastoDto> gastos) {
    }

    /** Destino gravado; {@code metaNome} é o nome da meta ligada (nulo sem meta). */
    public record DestinoDto(Long id, String nome, TipoDestino tipo, BigDecimal percentual, Long metaId,
                             String metaNome) {
    }

    /** Prévia de uma parcela do evento nos 12 meses a partir do mês atual. */
    public record PreviaAdicionalDto(YearMonth mes, BigDecimal liquido, List<DestinoValor> destinos,
                                     BigDecimal semDestino) {
    }

    /**
     * Plano de uso de um adicional. {@code ativo}: o evento entra na projeção (a prévia é calculada mesmo inativo).
     * {@code semDestinoPercentual} = 100 − {@code totalPercentual}.
     */
    public record PlanoAdicionalDto(EventoAdicional evento, boolean ativo, List<DestinoDto> destinos,
                                    BigDecimal totalPercentual, BigDecimal semDestinoPercentual,
                                    List<PreviaAdicionalDto> previa) {
    }

    public record PlanosAdicionaisDto(PlanoAdicionalDto decimo, PlanoAdicionalDto plr) {
    }

    /** Totais do período por tipo de destino: por evento e no total. */
    public record ResumoAdicionaisDto(TotaisAdicionais decimo, TotaisAdicionais plr, TotaisAdicionais total) {
    }

    // ---- agregado -----------------------------------------------------------------------------

    public record DadosDto(
            PerfilDto perfil, FeriasDto ferias, List<LancamentoDto> lancamentos,
            DecimoDto decimo, List<MetaDto> metas, BonusDto bonus, PlrDto plr, SaudeFinanceiraDto saudeFinanceira,
            PlanosAdicionaisDto adicionais) {
    }

    public record ErroDto(int status, String erro, String mensagem, List<String> detalhes) {
    }
}
