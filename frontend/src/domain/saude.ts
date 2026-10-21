/**
 * Apresentação do método 50/30/20 (aba Saúde financeira). Só rótulos, cores e formatação:
 * valores, percentuais, status e diagnósticos chegam prontos do back end.
 */
import { fmtPct } from './format'
import type { FaixaMetodo, GrupoGasto, StatusFaixa } from './types'

export interface InfoFaixa {
  faixa: FaixaMetodo
  nome: string
  /** o que entra na faixa, em linguagem do usuário */
  descricao: string
  /** a regra do método, como texto (posição do marcador de meta no gráfico) */
  meta: number
  rotuloMeta: string
  /** token CSS da cor da faixa (consistente em todos os gráficos da aba) */
  cor: string
}

/** Ordem fixa das faixas: a cor segue a faixa, nunca a posição. */
export const FAIXAS: InfoFaixa[] = [
  { faixa: 'necessidades', nome: 'Necessidades', descricao: 'Gastos obrigatórios: moradia, contas, mercado, saúde, transporte.', meta: 50, rotuloMeta: 'até 50%', cor: 'var(--f-nec)' },
  { faixa: 'desejos', nome: 'Desejos', descricao: 'Lazer e escolhas pessoais: passeios, assinaturas, compras.', meta: 30, rotuloMeta: 'até 30%', cor: 'var(--f-des)' },
  { faixa: 'poupanca', nome: 'Poupança', descricao: 'Aportes mensais em metas e na reserva de emergência.', meta: 20, rotuloMeta: 'pelo menos 20%', cor: 'var(--f-pou)' },
]

/** Percentual da renda (já com 1 casa, vindo do back end); sem renda, "—". */
export const fmtPctFaixa = (v: number | null | undefined) => (v === null || v === undefined ? '—' : fmtPct(v))

export type TomStatus = 'ok' | 'alerta'

export interface RotuloStatus {
  texto: string
  tom: TomStatus
}

/** Texto e tom do status da faixa. ACIMA só ocorre em necessidades/desejos; ABAIXO, na poupança. */
export function rotuloStatus(faixa: FaixaMetodo, status: StatusFaixa): RotuloStatus {
  if (status === 'OK') return { texto: faixa === 'poupanca' ? 'Meta atingida' : 'Dentro da meta', tom: 'ok' }
  if (status === 'ACIMA') return { texto: 'Acima da meta', tom: 'alerta' }
  return { texto: 'Abaixo da meta', tom: 'alerta' }
}

export const GRUPOS: { value: GrupoGasto; label: string }[] = [
  { value: 'NECESSIDADE', label: 'Obrigatório' },
  { value: 'DESEJO', label: 'Lazer' },
]
