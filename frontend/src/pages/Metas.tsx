import { Icon } from '../components/Icon'
import { Card, Field, NumberInput, Page } from '../components/ui'
import { fmt, rotuloCompetencia, rotuloCompetenciaLongo } from '../domain/format'
import type { Meta } from '../domain/types'
import { useApp } from '../store/AppContext'

/** Texto da previsão de conclusão; a contagem de aportes e o mês vêm prontos do back end. */
function previsao(m: Meta) {
  if (m.valorAlvo === 0) return 'sem alvo: aporta todo mês'
  if (m.mesesParaAtingir === 0) return 'meta atingida'
  if (m.previsao && m.mesesParaAtingir !== null) {
    const n = m.mesesParaAtingir
    return `${rotuloCompetenciaLongo(m.previsao)} (${n} ${n === 1 ? 'aporte' : 'aportes'})`
  }
  return '—'
}

export function Metas() {
  const { dados, adicionarMeta, editarMeta, removerMeta } = useApp()

  return (
    <Page title="Metas e reserva" subtitle="Acompanhe objetivos e veja quando você chega lá. O aporte mensal sai do saldo disponível na Projeção e continua seu, guardado na meta.">
      <div className="mgrid">
        {dados.metas.map((m) => (
          // percentual, previsão e saldo previsto vêm do back end
          <Card key={m.id}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
              <input type="text" value={m.nome} aria-label="Nome da meta" onChange={(e) => editarMeta(m.id, { nome: e.target.value })} style={{ fontWeight: 600 }} />
              <span className="tag p">{m.percentual.toFixed(0)}%</span>
              <button className="btn x" aria-label={`Remover ${m.nome}`} onClick={() => void removerMeta(m.id)}><Icon name="close" size={14} /></button>
            </div>
            <div className="pbar" style={{ margin: '14px 0 6px' }}><i style={{ width: `${m.percentual}%` }} /></div>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--mut)', fontSize: 12, marginBottom: 14 }}>
              <span>{fmt(m.valorAtual)}</span><span>{m.valorAlvo > 0 ? fmt(m.valorAlvo) : 'sem alvo'}</span>
            </div>
            <div className="row2">
              <Field label="Valor atual"><NumberInput min={0} step={100} value={m.valorAtual} onChange={(v) => editarMeta(m.id, { valorAtual: v })} /></Field>
              <Field label="Valor alvo"><NumberInput min={0} step={100} value={m.valorAlvo} onChange={(v) => editarMeta(m.id, { valorAlvo: v })} /></Field>
            </div>
            <Field label="Aporte mensal (R$)"><NumberInput min={0} step={50} value={m.aporteMensal} onChange={(v) => editarMeta(m.id, { aporteMensal: v })} /></Field>
            <Field label="Banco onde está guardado">
              <input type="text" maxLength={80} placeholder="Caixa Econômica Federal" value={m.banco ?? ''}
                onChange={(e) => editarMeta(m.id, { banco: e.target.value })} />
            </Field>
            <small style={{ color: 'var(--mut)', display: 'block' }}>
              Previsão de conclusão: <b style={{ color: 'var(--green)' }}>{previsao(m)}</b>
            </small>
            <small style={{ color: 'var(--mut)', display: 'block', marginTop: 4 }}>
              Guardado previsto em {rotuloCompetencia(m.fimProjecao)}: <b className="num" style={{ color: 'var(--ink)' }}>{fmt(m.guardadoFimProjecao)}</b>
            </small>
          </Card>
        ))}
        <Card style={{ display: 'grid', placeItems: 'center', minHeight: 160, borderStyle: 'dashed' }}>
          <button className="btn pri" onClick={() => void adicionarMeta()}>+ Nova meta</button>
        </Card>
      </div>
    </Page>
  )
}
