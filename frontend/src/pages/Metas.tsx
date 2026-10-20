import { Icon } from '../components/Icon'
import { Card, Field, NumberInput, Page } from '../components/ui'
import { fmt, rotuloCompetenciaLongo } from '../domain/format'
import { useApp } from '../store/AppContext'

export function Metas() {
  const { dados, adicionarMeta, editarMeta, removerMeta } = useApp()

  return (
    <Page title="Metas e reserva" subtitle="Acompanhe objetivos e veja quando você chega lá com o aporte mensal.">
      <div className="mgrid">
        {dados.metas.map((m) => {
          // percentual e previsão vêm do back end
          const meses = m.mesesParaAtingir
          return (
            <Card key={m.id}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                <input type="text" value={m.nome} aria-label="Nome da meta" onChange={(e) => editarMeta(m.id, { nome: e.target.value })} style={{ fontWeight: 600 }} />
                <span className="tag p">{m.percentual.toFixed(0)}%</span>
                <button className="btn x" aria-label={`Remover ${m.nome}`} onClick={() => void removerMeta(m.id)}><Icon name="close" size={14} /></button>
              </div>
              <div className="pbar" style={{ margin: '14px 0 6px' }}><i style={{ width: `${m.percentual}%` }} /></div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--mut)', fontSize: 12, marginBottom: 14 }}>
                <span>{fmt(m.valorAtual)}</span><span>{fmt(m.valorAlvo)}</span>
              </div>
              <div className="row2">
                <Field label="Valor atual"><NumberInput min={0} step={100} value={m.valorAtual} onChange={(v) => editarMeta(m.id, { valorAtual: v })} /></Field>
                <Field label="Valor alvo"><NumberInput min={0} step={100} value={m.valorAlvo} onChange={(v) => editarMeta(m.id, { valorAlvo: v })} /></Field>
              </div>
              <Field label="Aporte mensal (R$)"><NumberInput min={0} step={50} value={m.aporteMensal} onChange={(v) => editarMeta(m.id, { aporteMensal: v })} /></Field>
              <small style={{ color: 'var(--mut)' }}>
                Previsão de conclusão:{' '}
                <b style={{ color: 'var(--green)' }}>
                  {m.previsao && meses !== null ? `${rotuloCompetenciaLongo(m.previsao)} (${meses} ${meses === 1 ? 'mês' : 'meses'})` : '—'}
                </b>
              </small>
            </Card>
          )
        })}
        <Card style={{ display: 'grid', placeItems: 'center', minHeight: 160, borderStyle: 'dashed' }}>
          <button className="btn pri" onClick={() => void adicionarMeta()}>+ Nova meta</button>
        </Card>
      </div>
    </Page>
  )
}
