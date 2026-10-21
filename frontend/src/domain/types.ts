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
}

export interface Categoria {
  nome: string
  ordem: number
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
  metas: AporteMeta[]
  plr: number
  bonus: number
  /** eventos do mês unidos por " + " (vazio se nenhum) */
  evento: string
  baseInss: number
  tetoInssAplicado: boolean
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
