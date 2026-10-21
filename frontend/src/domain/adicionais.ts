/**
 * Apresentação do plano de uso do 13º e do PLR: rótulos, cores e segmentos para as barras.
 * Valores, percentuais e divisões chegam prontos do back end; aqui nada é calculado.
 */
import type { ChaveAdicional, EventoAdicional, FaixaMetodo, PlanoAdicional, ParcelaAdicional, TipoDestino } from './types'

export interface InfoTipo {
  value: TipoDestino
  label: string
  /** faixa do 50/30/20 correspondente (a cor é a mesma da faixa na Saúde financeira) */
  faixa: FaixaMetodo
  cor: string
}

/** Ordem fixa: a cor segue o tipo (= faixa do método), nunca a posição. */
export const TIPOS_DESTINO: InfoTipo[] = [
  { value: 'LAZER', label: 'Lazer', faixa: 'desejos', cor: 'var(--f-des)' },
  { value: 'OBRIGATORIO', label: 'Obrigatório', faixa: 'necessidades', cor: 'var(--f-nec)' },
  { value: 'RESERVA', label: 'Reserva', faixa: 'poupanca', cor: 'var(--f-pou)' },
]

export const infoTipo = (t: TipoDestino) => TIPOS_DESTINO.find((x) => x.value === t) ?? TIPOS_DESTINO[0]

/** cor neutra do que fica sem destino (no saldo disponível) */
export const COR_SEM_DESTINO = 'var(--f-sem)'

export const NOME_EVENTO: Record<EventoAdicional, string> = { DECIMO: '13º', PLR: 'PLR' }
export const CHAVE_EVENTO: Record<EventoAdicional, ChaveAdicional> = { DECIMO: 'decimo', PLR: 'plr' }

export interface SegmentoDestino {
  chave: string
  nome: string
  cor: string
  /** largura relativa (percentual ou valor; a barra normaliza pelo total informado) */
  peso: number
  /** textos já formatados para legenda/tooltip */
  pct?: number
  valor?: number
  semDestino?: boolean
}

/** Segmentos do plano (pelos percentuais gravados), com o "sem destino" no fim. */
export function segmentosPlano(plano: PlanoAdicional): SegmentoDestino[] {
  const s: SegmentoDestino[] = plano.destinos.map((d) => ({
    chave: String(d.id), nome: d.nome, cor: infoTipo(d.tipo).cor, peso: d.percentual, pct: d.percentual,
  }))
  if (plano.semDestinoPercentual > 0) {
    s.push({ chave: 'sem', nome: 'Sem destino', cor: COR_SEM_DESTINO, peso: plano.semDestinoPercentual, pct: plano.semDestinoPercentual, semDestino: true })
  }
  return s
}

/** Segmentos de uma parcela (pelos valores calculados no back end), com o "sem destino" no fim. */
export function segmentosParcela(p: ParcelaAdicional): SegmentoDestino[] {
  const s: SegmentoDestino[] = p.destinos.map((d, i) => ({
    chave: `${d.id ?? i}`, nome: d.nome, cor: infoTipo(d.tipo).cor, peso: d.valor, pct: d.percentual, valor: d.valor,
  }))
  if (p.semDestino > 0) s.push({ chave: 'sem', nome: 'Sem destino', cor: COR_SEM_DESTINO, peso: p.semDestino, valor: p.semDestino, semDestino: true })
  return s
}
