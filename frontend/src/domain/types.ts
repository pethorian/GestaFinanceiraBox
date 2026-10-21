/** Tipos do contrato da API (espelham os DTOs do back end). Competências: "AAAA-MM". Meses de 13º/férias: 1..12. */

export type Saude = 'NAO' | 'SOZINHO' | 'DEP'
export type TipoLancamento = 'RECORRENTE' | 'PONTUAL' | 'PARCELADO'

export interface Perfil {
  /** como o usuário quer ser chamado (opcional, até 60 caracteres); null = sem nome */
  nome: string | null
  cargoId: number
  delta: number
  diasSobreaviso: number
  filhos: number
  homeOffice: boolean
  vrVa: boolean
  saude: Saude
  dependentes: number
  funcefPct: number
  valeTransporte: boolean
  sindicato: boolean
  saldoInicial: number
  /** saldo atual acumulado na FUNCEF (opcional, ≥ 0); ponto de partida da previdência na projeção */
  saldoFuncef: number
}

export interface Ferias {
  ativo: boolean
  mes: number
  abono: boolean
}

export interface Lancamento {
  id: number | null
  descricao: string
  categoria: string
  tipo: TipoLancamento
  valor: number
  mesInicial: string
  parcelas: number
}

export interface ParcelaDecimo {
  mes: number
  percentual: number
}

export interface Decimo {
  ativo: boolean
  /** parcelas fixas (somente leitura): fevereiro 50% e novembro 50% */
  parcelas: ParcelaDecimo[]
  totalPercentual: number
}

export interface Bonus {
  ativo: boolean
  mes: number
  salarios: number
}

/** Parcela de PLR devolvida pelo back end. `valor` é o bruto digitado (null = usar o calculado). */
export interface ParcelaPlr {
  mes: number
  percentual: number
  valor: number | null
  valorBruto: number
  desconto: number
  valorLiquido: number
}

export interface Plr {
  ativo: boolean
  parcelas: ParcelaPlr[]
  totalPercentual: number
  aviso: string | null
  estimativaAnual: number
}

export interface MetaEntrada {
  nome: string
  valorAlvo: number
  valorAtual: number
  aporteMensal: number
  /** onde o dinheiro está guardado (texto livre; o servidor apara e grava vazio como null) */
  banco: string | null
}

export interface Meta extends MetaEntrada {
  id: number
  falta: number
  percentual: number
  /** aportes que faltam, contando o mês atual (0 = atingida; null = sem alvo ou sem aporte) */
  mesesParaAtingir: number | null
  /** mês do último aporte (o mesmo em que a projeção atinge o alvo) */
  previsao: string | null
  /** saldo previsto da meta em fimProjecao (12º mês da projeção a partir de hoje) */
  guardadoFimProjecao: number
  fimProjecao: string
}

export interface Dados {
  perfil: Perfil
  ferias: Ferias
  lancamentos: Lancamento[]
  decimo: Decimo
  bonus: Bonus
  plr: Plr
  metas: Meta[]
  /** método 50/30/20, calculado pelo back end (sempre, mesmo sem adesão) */
  saudeFinanceira: SaudeFinanceira
  /** plano de uso do 13º e do PLR */
  adicionais: PlanosAdicionais
}

// ---- plano de uso do 13º e do PLR -------------------------------------------------------------

export type EventoAdicional = 'DECIMO' | 'PLR'
/** caminho da API: /api/adicionais/{decimo|plr}/destinos */
export type ChaveAdicional = 'decimo' | 'plr'
/** LAZER → desejos; OBRIGATORIO → necessidades; RESERVA → poupança (opcionalmente ligada a uma meta) */
export type TipoDestino = 'LAZER' | 'OBRIGATORIO' | 'RESERVA'

/** Item enviado no PUT (a lista inteira substitui a anterior). metaId só com RESERVA. */
export interface DestinoEntrada {
  nome: string
  tipo: TipoDestino
  percentual: number
  metaId: number | null
}

export interface DestinoAdicional extends DestinoEntrada {
  id: number
  /** nome da meta ligada (null sem meta) */
  metaNome: string | null
}

/** Valor de um destino em uma parcela (calculado pelo back end). */
export interface DestinoValor {
  id: number | null
  nome: string
  tipo: TipoDestino
  metaId: number | null
  percentual: number
  valor: number
}

/** Divisão do líquido de uma parcela: liquido = soma dos destinos + semDestino. */
export interface ParcelaAdicional {
  liquido: number
  destinos: DestinoValor[]
  semDestino: number
}

export interface PreviaAdicional extends ParcelaAdicional {
  /** "AAAA-MM" */
  mes: string
}

export interface PlanoAdicional {
  evento: EventoAdicional
  /** o evento entra na projeção (a prévia vem mesmo inativo) */
  ativo: boolean
  destinos: DestinoAdicional[]
  totalPercentual: number
  /** 100 − totalPercentual */
  semDestinoPercentual: number
  /** parcelas dos 12 meses a partir do mês atual */
  previa: PreviaAdicional[]
}

export interface PlanosAdicionais {
  decimo: PlanoAdicional
  plr: PlanoAdicional
}

/** Adicional recebido no mês da projeção, já dividido pelo plano. */
export interface AdicionalMes extends ParcelaAdicional {
  evento: EventoAdicional
}

/** Totais do período por tipo de destino: liquido = lazer + obrigatorio + reserva + semDestino. */
export interface TotaisAdicionais {
  liquido: number
  lazer: number
  obrigatorio: number
  reserva: number
  semDestino: number
}

export interface DivisaoAdicionais {
  necessidades: number
  desejos: number
  poupanca: number
  semDestino: number
}

/** Bloco "adicionais" da saúde financeira: uso do 13º/PLR do período × 50/30/20 (à parte do mês comum). */
export interface SaudeAdicionais {
  totalLiquidoPeriodo: number
  divisao: DivisaoAdicionais
  /** % do total líquido dos adicionais (1 casa); null sem adicional no período */
  percentuais: DivisaoAdicionais | null
  comparacao: { recomendado: Faixas<number>; diferenca: Faixas<number>; status: Faixas<StatusFaixa> } | null
  /** há algum destino definido */
  definido: boolean
  /** estas frases também vêm no fim de SaudeFinanceira.diagnostico */
  diagnostico: string[]
}

/** NECESSIDADE = gasto obrigatório (faixa dos 50%); DESEJO = lazer (faixa dos 30%) */
export type GrupoGasto = 'NECESSIDADE' | 'DESEJO'

export interface Categoria {
  nome: string
  ordem: number
  grupo: GrupoGasto
}

/** Faixas do método 50/30/20. */
export type FaixaMetodo = 'necessidades' | 'desejos' | 'poupanca'
export type Faixas<T> = Record<FaixaMetodo, T>
/** OK; ACIMA = alerta em necessidades/desejos; ABAIXO = alerta na poupança */
export type StatusFaixa = 'OK' | 'ACIMA' | 'ABAIXO'

export interface CategoriaSaude {
  nome: string
  grupo: GrupoGasto
  mediaMensal: number
}

/** Saúde financeira pelo método 50/30/20. Todos os números vêm prontos do back end. */
export interface SaudeFinanceira {
  /** o usuário aderiu ao método (o cálculo é feito de qualquer forma) */
  participa: boolean
  /** líquido de um mês comum */
  rendaMensal: number
  recomendado: Faixas<number>
  atual: Faixas<number>
  /** % da renda com 1 casa; null se a renda é ≤ 0 */
  percentuais: Faixas<number> | null
  /** atual − recomendado, por faixa */
  diferenca: Faixas<number>
  /** necessidades/desejos: OK | ACIMA; poupança: OK | ABAIXO */
  status: Faixas<StatusFaixa>
  /** renda − necessidades − desejos − poupança (negativa se passa da renda) */
  sobraLivre: number
  /** quanto falta aportar por mês para chegar aos 20% */
  aporteSugerido: number
  /** todas as categorias, na ordem de cadastro (média 0 se não há gasto) */
  categorias: CategoriaSaude[]
  diagnostico: string[]
  /** composição de atual.poupanca: aportes em metas + contribuição FUNCEF */
  poupancaDetalhe: { metas: number; funcef: number }
  /** uso do 13º e do PLR dos próximos 12 meses, à parte do método mensal */
  adicionais: SaudeAdicionais
}

export interface DetalheIrpf {
  deducaoLegal: number
  deducaoMinima: number
  deducao: number
  baseCalculo: number
  aliquota: number
  bruto: number
  reducao: number
  imposto: number
}

export interface AporteMeta {
  id: number
  nome: string
  aporte: number
  guardado: number
  /** reserva do 13º/PLR ligada a esta meta no mês (entra antes do aporte mensal) */
  aporteAdicional: number
}

export interface MesProjecao {
  indice: number
  competencia: string
  /** rótulo curto ("Out/26"), montado a partir da competência apenas para exibição */
  rotulo: string
  base: number
  sobreaviso: number
  creche: number
  homeOffice: number
  vrVa: number
  decimo: number
  ferias: number
  inss: number
  irpf: DetalheIrpf
  funcef: number
  saude: number
  valeTransporte: number
  sindicato: number
  descontoDecimo: number
  receitas: number
  descontos: number
  liquido: number
  gastos: number
  /** total aportado nas metas no mês (sai da conta) */
  aportes: number
  /** líquido − gastos − aportes */
  sobra: number
  /** dinheiro disponível em conta (saldo inicial + sobras) */
  acumulado: number
  /** soma dos saldos das metas ao fim do mês */
  guardado: number
  /** acumulado + guardado */
  patrimonio: number
  /** parte da FUNCEF retida na última parcela do 13º (já dentro de descontoDecimo) */
  funcefDecimo: number
  /** saldo FUNCEF ao fim do mês: saldo informado + contribuições acumuladas, sem rendimento */
  previdencia: number
  /** patrimonio + previdencia */
  patrimonioTotal: number
  metas: AporteMeta[]
  plr: number
  bonus: number
  /** eventos do mês unidos por " + " (vazio se nenhum) */
  evento: string
  baseInss: number
  tetoInssAplicado: boolean
  /** parcelas de 13º/PLR do mês, divididas pelo plano de uso */
  adicionais: AdicionalMes[]
  /** destinos LAZER + OBRIGATORIO dos adicionais (saem da conta, à parte de gastos) */
  gastosAdicionais: number
  /** destinos RESERVA dos adicionais (saem do disponível e entram em guardado) */
  reservasAdicionais: number
  /** saldo da reserva dos adicionais sem meta, ao fim do mês (já dentro de guardado) */
  guardadoReservaAdicionais: number
}

export interface DestaqueMes {
  competencia: string
  sobra: number
}

export interface ResumoProjecao {
  receitas: number
  descontos: number
  liquido: number
  gastos: number
  sobra: number
  saldoFinal: number
  variacaoSaldo: number
  sobraMedia: number
  liquidoMedio: number
  gastosMedio: number
  melhorMes: DestaqueMes
  piorMes: DestaqueMes
  mesesNegativos: string[]
  /** maior |sobra| do período: referência para a escala das barras */
  maiorSobraAbsoluta: number
  /** total aportado em metas no período */
  aportes: number
  guardadoInicial: number
  guardadoFinal: number
  /** saldoFinal + guardadoFinal */
  patrimonioFinal: number
  previdenciaInicial: number
  previdenciaFinal: number
  /** patrimonioFinal + previdenciaFinal */
  patrimonioTotalFinal: number
  /** somas do período do plano de uso do 13º/PLR */
  gastosAdicionais: number
  reservasAdicionais: number
  adicionais: { decimo: TotaisAdicionais; plr: TotaisAdicionais; total: TotaisAdicionais }
}

export interface Projecao {
  inicio: string
  saldoInicial: number
  meses: MesProjecao[]
  resumo: ResumoProjecao
}

export interface ResumoGastos {
  inicio: string
  mediaMensalTotal: number
  categorias: { categoria: string; mediaMensal: number }[]
}

export interface Cargo {
  id: number
  nome: string
  jornada: number
  usaDelta: boolean
  ordem: number
  piso: number
  gratificacao: number
  plrAnual: number
  vigenciaInicio: string | null
}

export interface Faixa {
  ate: number | null
  aliquota: number
  deducao: number
}

export interface TabelaFaixas {
  vigenciaInicio: string
  vigenciaFim: string | null
  faixas: Faixa[]
}

export interface TabelaDeltas {
  vigenciaInicio: string
  vigenciaFim: string | null
  deltas: { referencia: number; valor: number }[]
}

export interface Beneficio {
  vigenciaInicio: string
  vigenciaFim: string | null
  pai: number
  homeOffice: number
  vrVa: number
  bonusCaixaMaxSalarios: number
  vtPercentual: number
  sindicatoPercentual: number
  sindicatoTeto: number
  saudePercentual: number
  saudeTetoPercentual: number
  planoPorDependente: number
  inssTeto: number
  inssTetoSalario: number
  irDeducaoMinima: number
  irLimiteIsencao: number
  irLimiteReducao: number
  irReducaoA: number
  irReducaoB: number
  divisorJornada6h: number
  divisorJornada8h: number
}

export interface ParametrosVigentes {
  data: string
  cargos: Cargo[]
  deltas: TabelaDeltas
  inss: TabelaFaixas
  irpf: TabelaFaixas
  beneficios: Beneficio
}
