import { useState } from 'react'
import { COR_SEM_DESTINO, NOME_EVENTO, TIPOS_DESTINO, infoTipo, segmentosParcela, segmentosPlano, type SegmentoDestino } from '../domain/adicionais'
import { fmt, fmtPct, rotuloCompetencia, tituloCompetencia } from '../domain/format'
import type { AdicionalMes, AporteMeta, DestinoEntrada, Meta, PlanoAdicional, SaudeAdicionais, TipoDestino } from '../domain/types'
import { Icon } from './Icon'
import { FAIXAS, fmtPctFaixa } from '../domain/saude'
import { StatusFaixaPill } from './saudeCharts'
import { Card, NumberInput } from './ui'

/*
 * Plano de uso do 13º e do PLR: barra da divisão, resumo do card, editor e bloco do mês na projeção.
 * Cores pelo tipo do destino = faixa do 50/30/20 (--f-nec / --f-des / --f-pou) e neutra para "sem destino".
 * Nenhum cálculo: percentuais e valores vêm do back end; a barra só converte peso em largura.
 */

const descreve = (s: SegmentoDestino) =>
  [s.nome, s.pct !== undefined ? fmtPct(s.pct) : null, s.valor !== undefined ? fmt(s.valor) : null].filter(Boolean).join(' · ')

/** Barra 100% empilhada (2px de respiro entre segmentos), com tooltip por segmento e legenda com rótulo direto. */
export function BarraDestinos({ segmentos, rotulo, legenda = true, valores = false }: {
  segmentos: SegmentoDestino[]; rotulo: string; legenda?: boolean; valores?: boolean
}) {
  const [hover, setHover] = useState<number | null>(null)
  const visiveis = segmentos.filter((s) => s.peso > 0)
  const h = hover !== null ? visiveis[hover] : null
  return (
    <div className="ad-barw">
      <div className="ad-bar" role="img" aria-label={`${rotulo}: ${visiveis.map(descreve).join('; ') || 'sem valores'}`}>
        {visiveis.length === 0 && <span className="ad-seg vazio" />}
        {visiveis.map((s, i) => (
          <span
            key={s.chave} className="ad-seg" style={{ flexGrow: s.peso, background: s.cor }}
            onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}
          />
        ))}
        {h && (
          <div className="tt ad-tt" style={{ opacity: 1 }}>
            <b>{h.nome}</b>
            {h.pct !== undefined && <div><span>Percentual</span><span className="num">{fmtPct(h.pct)}</span></div>}
            {h.valor !== undefined && <div><span>Valor</span><span className="num">{fmt(h.valor)}</span></div>}
          </div>
        )}
      </div>
      {legenda && visiveis.length > 0 && (
        <div className="ad-leg">
          {visiveis.map((s) => (
            <span key={s.chave} className={s.semDestino ? 'sem' : undefined}>
              <i className="sf-sw" style={{ background: s.cor }} />{s.nome}
              <b className="num">{valores && s.valor !== undefined ? fmt(s.valor) : s.pct !== undefined ? fmtPct(s.pct) : ''}</b>
            </span>
          ))}
        </div>
      )}
    </div>
  )
}

/** Resumo compacto no card do 13º/PLR: a divisão gravada e o botão que abre o editor. */
export function PlanoUsoResumo({ plano, onEditar }: { plano: PlanoAdicional; onEditar: () => void }) {
  const nome = NOME_EVENTO[plano.evento]
  const definido = plano.destinos.length > 0
  return (
    <div className="ad-resumo">
      <div className="ad-resumo-h">
        <b>Plano de uso</b>
        <button type="button" className="btn sm" onClick={onEditar} aria-label={`${definido ? 'Editar' : 'Definir'} o plano de uso do ${nome}`}>
          {definido ? 'Editar' : <><Icon name="plus" size={13} />Definir</>}
        </button>
      </div>
      {definido
        ? <BarraDestinos segmentos={segmentosPlano(plano)} rotulo={`Plano de uso do ${nome}`} />
        : <small className="dim">Sem plano: o {nome} inteiro fica no saldo disponível.</small>}
    </div>
  )
}

/** Linha do rascunho do editor: o destino digitado + uma chave estável para a lista. */
export interface RascunhoDestino extends DestinoEntrada {
  chave: string
}

const OPCOES_TIPO = TIPOS_DESTINO.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)

/**
 * Editor do plano de uso (sem estado próprio: o rascunho vem por props). A barra e a prévia mostram
 * o que o servidor gravou; o rascunho é o que o usuário está digitando.
 */
export function PlanoUsoEditor({ plano, rascunho, metas, erros, onChange, onAdicionar }: {
  plano: PlanoAdicional
  rascunho: RascunhoDestino[]
  metas: Pick<Meta, 'id' | 'nome'>[]
  erros: string[] | null
  onChange: (lista: RascunhoDestino[]) => void
  onAdicionar: () => void
}) {
  const nome = NOME_EVENTO[plano.evento]
  const editar = (i: number, patch: Partial<DestinoEntrada>) =>
    onChange(rascunho.map((d, k) => (k === i ? { ...d, ...patch } : d)))
  const proxima = plano.previa[0]
  const outras = plano.previa.slice(1)
  return (
    <div className="ad-editor">
      {rascunho.length === 0 ? (
        <p className="dim ad-vazio">Nenhum destino ainda: todo o {nome} fica no saldo disponível. Ex.: Viagem 30% (reserva), Lazer 50%.</p>
      ) : (
        <div className="ad-lista" role="list" aria-label={`Destinos do ${nome}`}>
          <div className="ad-row ad-head" aria-hidden="true"><span>Destino</span><span>Tipo</span><span>%</span><span /></div>
          {rascunho.map((d, i) => (
            <div key={d.chave} className="ad-item" role="listitem">
              <div className="ad-row">
                <input
                  type="text" value={d.nome} maxLength={60} placeholder={i === 0 ? 'Viagem' : 'Ex.: Lazer, Reserva'}
                  aria-label={`Nome do destino ${i + 1}`} onChange={(e) => editar(i, { nome: e.target.value })}
                />
                <select
                  value={d.tipo} aria-label={`Tipo do destino ${i + 1}`}
                  style={{ borderLeft: `3px solid ${infoTipo(d.tipo).cor}` }}
                  onChange={(e) => {
                    const tipo = e.target.value as TipoDestino
                    editar(i, { tipo, metaId: tipo === 'RESERVA' ? d.metaId : null })
                  }}
                >{OPCOES_TIPO}</select>
                <NumberInput
                  min={0} max={100} step={1} value={d.percentual} aria-label={`Percentual do destino ${i + 1}`}
                  onChange={(v) => editar(i, { percentual: v })}
                />
                <button
                  type="button" className="btn x ghost" aria-label={`Remover o destino ${i + 1}`}
                  onClick={() => onChange(rascunho.filter((_, k) => k !== i))}
                ><Icon name="close" size={14} /></button>
              </div>
              {d.tipo === 'RESERVA' && (
                <label className="ad-meta">
                  <span>Guardar em</span>
                  <select
                    value={d.metaId ?? ''} aria-label={`Meta do destino ${i + 1}`}
                    onChange={(e) => editar(i, { metaId: e.target.value === '' ? null : Number(e.target.value) })}
                  >
                    <option value="">Reserva dos adicionais (sem meta)</option>
                    {metas.map((m) => <option key={m.id} value={m.id}>Meta: {m.nome}</option>)}
                  </select>
                </label>
              )}
            </div>
          ))}
        </div>
      )}
      <button type="button" className="btn sm ad-add" onClick={onAdicionar}><Icon name="plus" size={13} />Destino</button>

      {erros && erros.length > 0 && (
        <div className="ad-erro" role="alert">
          <Icon name="warn" size={15} />
          <div><b>Não foi possível salvar o plano.</b>{erros.map((e, i) => <span key={i}>{e}</span>)}</div>
        </div>
      )}

      <div className="memo ad-sec">
        <h4>Divisão salva <span className="num">{fmtPct(plano.totalPercentual)} com destino</span></h4>
        <BarraDestinos segmentos={segmentosPlano(plano)} rotulo={`Divisão do ${nome}`} />
      </div>

      {proxima && (
        <div className="memo ad-sec">
          <h4>Próxima parcela · {tituloCompetencia(proxima.mes)}</h4>
          <div><span>Líquido</span><span className="num">{fmt(proxima.liquido)}</span></div>
          {proxima.destinos.map((d, i) => (
            <div key={d.id ?? i} className="ad-linha">
              <span><i className="sf-sw" style={{ background: infoTipo(d.tipo).cor }} />{d.nome} <small>{fmtPct(d.percentual)}</small></span>
              <span className="num">{fmt(d.valor)}</span>
            </div>
          ))}
          {proxima.semDestino > 0 && (
            <div className="ad-linha">
              <span><i className="sf-sw" style={{ background: COR_SEM_DESTINO }} />Sem destino <small>fica no saldo</small></span>
              <span className="num">{fmt(proxima.semDestino)}</span>
            </div>
          )}
          {outras.length > 0 && (
            <p className="dim ad-outras">Depois: {outras.map((p) => `${rotuloCompetencia(p.mes)} ${fmt(p.liquido)}`).join(' · ')}</p>
          )}
        </div>
      )}
    </div>
  )
}

/** Bloco do detalhe do mês: como a parcela do 13º/PLR recebida neste mês foi dividida. */
export function AdicionalDoMes({ adicional, metas }: { adicional: AdicionalMes; metas: AporteMeta[] }) {
  const nome = NOME_EVENTO[adicional.evento]
  const nomeMeta = (id: number | null) => (id === null ? null : metas.find((m) => m.id === id)?.nome ?? null)
  return (
    <div className="memo ad-mes">
      <h4>Do seu {nome} deste mês ({fmt(adicional.liquido)} líquido)</h4>
      <BarraDestinos segmentos={segmentosParcela(adicional)} rotulo={`Divisão do ${nome} deste mês`} legenda={false} />
      {adicional.destinos.length === 0 && <div className="st"><span>Sem plano de uso: tudo fica no saldo disponível.</span><span /></div>}
      {adicional.destinos.map((d, i) => {
        const meta = nomeMeta(d.metaId)
        return (
          <div key={d.id ?? i}>
            <span><i className="sf-sw" style={{ background: infoTipo(d.tipo).cor }} />{d.nome}
              <small className="ad-tipo">{fmtPct(d.percentual)} · {infoTipo(d.tipo).label}{meta ? ` → ${meta}` : ''}</small>
            </span>
            <span className="num">{fmt(d.valor)}</span>
          </div>
        )
      })}
      {adicional.destinos.length > 0 && adicional.semDestino > 0 && (
        <div>
          <span><i className="sf-sw" style={{ background: COR_SEM_DESTINO }} />Sem destino <small className="ad-tipo">fica no saldo</small></span>
          <span className="num">{fmt(adicional.semDestino)}</span>
        </div>
      )}
    </div>
  )
}

/** Linha "Ideal" × "Seu plano" (cada uma uma barra 100%) com uma legenda única por faixa. */
function DivisaoAdicionaisChart({ ad }: { ad: SaudeAdicionais }) {
  const pct = ad.percentuais!
  const rec = ad.comparacao!.recomendado
  const ideal: SegmentoDestino[] = FAIXAS.map((f) => ({ chave: f.faixa, nome: `Ideal · ${f.nome}`, cor: f.cor, peso: f.meta, pct: f.meta, valor: rec[f.faixa] }))
  const plano: SegmentoDestino[] = [
    ...FAIXAS.map((f) => ({ chave: f.faixa, nome: f.nome, cor: f.cor, peso: ad.divisao[f.faixa], pct: pct[f.faixa], valor: ad.divisao[f.faixa] })),
    { chave: 'sem', nome: 'Sem destino', cor: COR_SEM_DESTINO, peso: ad.divisao.semDestino, pct: pct.semDestino, valor: ad.divisao.semDestino, semDestino: true },
  ]
  return (
    <div className="ad-cmp">
      <div className="ad-cmp-row"><span>Ideal</span><BarraDestinos segmentos={ideal} rotulo="Ideal do método 50/30/20 para os adicionais" legenda={false} /></div>
      <div className="ad-cmp-row"><span>Seu plano</span><BarraDestinos segmentos={plano} rotulo="Como você planejou usar os adicionais" legenda={false} /></div>
    </div>
  )
}

/**
 * Card "13º e PLR" da Saúde financeira: divisão dos adicionais do período × ideal 50/30/20 e status por faixa.
 * As frases do back end (adicionais.diagnostico) aparecem uma única vez por tela: aqui quando não há o card
 * Diagnóstico (sem adesão, `frases`), ou no Diagnóstico principal, onde o back end já as acrescenta (participando).
 */
export function AdicionaisSaude({ ad, frases = true }: { ad: SaudeAdicionais; frases?: boolean }) {
  const comparar = ad.definido && ad.percentuais !== null && ad.comparacao !== null
  const semPlano = ad.percentuais !== null && !ad.definido
  return (
    <Card className={`col sf-adic${frases ? '' : ' compacto'}`}>
      <div className="ad-cab">
        <h2><Icon name="gift" size={15} />13º e PLR <small>próximos 12 meses{ad.totalLiquidoPeriodo > 0 ? ` · ${fmt(ad.totalLiquidoPeriodo)} líquido` : ''}</small></h2>
        {comparar && <DivisaoAdicionaisChart ad={ad} />}
      </div>
      {comparar && (
        <div className="ad-faixas">
          {FAIXAS.map((f) => (
            <div key={f.faixa}>
              <span><i className="sf-sw" style={{ background: f.cor }} />{f.nome}</span>
              <b className="num">{fmtPctFaixa(ad.percentuais![f.faixa])}</b>
              <StatusFaixaPill faixa={f.faixa} status={ad.comparacao!.status[f.faixa]} />
            </div>
          ))}
          {ad.divisao.semDestino > 0 && (
            <div className="sem">
              <span><i className="sf-sw" style={{ background: COR_SEM_DESTINO }} />Sem destino</span>
              <b className="num">{fmtPctFaixa(ad.percentuais!.semDestino)}</b>
              <small className="dim">fica no saldo</small>
            </div>
          )}
        </div>
      )}
      <div className="ad-txt">
        {frases ? (
          <ul className="ad-diag">
            {ad.diagnostico.map((d, i) => <li key={i}><Icon name="info" size={14} />{d}</li>)}
          </ul>
        ) : !comparar && (
          <p className="dim ad-nota">
            {semPlano ? 'Você ainda não definiu como usar seus adicionais: tudo fica no saldo disponível.' : 'Sem 13º nem PLR nos próximos 12 meses.'}
          </p>
        )}
        {ad.percentuais !== null && (
          <a className={`btn sm${ad.definido ? '' : ' pri'}`} href="#/decimo">
            {ad.definido ? 'Ajustar plano de uso' : 'Definir como usar o 13º e o PLR'}<Icon name="arrow" size={14} />
          </a>
        )}
      </div>
    </Card>
  )
}
