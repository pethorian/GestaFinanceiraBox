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
