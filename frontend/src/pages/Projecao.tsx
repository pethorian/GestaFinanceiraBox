import { useState } from 'react'
import { FluxoChart, Spark } from '../components/charts'
import { Icon, type IconName } from '../components/Icon'
import { MemoriaCalculo } from '../components/MemoriaCalculo'
import { Card, Page, cls } from '../components/ui'
import { fmt, rotuloCompetencia, tituloProjecao } from '../domain/format'
import { useApp } from '../store/AppContext'

function Kpi({ icon, cor, titulo, valor, classe = '' }: { icon: IconName; cor: string; titulo: string; valor: string; classe?: string }) {
  return (
    <Card className="kpi">
      <div className="ic" style={{ background: `${cor}22`, color: cor }}><Icon name={icon} /></div>
      <div><small>{titulo}</small><strong className={`num ${classe}`}>{valor}</strong></div>
    </Card>
  )
}

export function Projecao() {
  const { dados, projecao, parametros } = useApp()
  const [sel, setSel] = useState<number | null>(null)

  // Todos os números (médias, melhor/pior mês, variação, meses no vermelho) vêm do back end.
  const ms = projecao.meses
  const r = projecao.resumo
  const ultimo = ms[ms.length - 1]
  const cargo = parametros.cargos.find((c) => c.id === dados.perfil.cargoId)
  const negativos = r.mesesNegativos.map(rotuloCompetencia)
  const mx = r.maiorSobraAbsoluta || 1 // referência de escala das barras, calculada no servidor

  const aviso = negativos.length > 0 && (
    <span className="pill-warn" role="alert" title="Revise gastos pontuais ou parcelas nesses meses">
      <Icon name="warn" size={15} />
      {negativos.length} {negativos.length > 1 ? 'meses no vermelho' : 'mês no vermelho'}: {negativos.join(', ')}
    </span>
  )

  return (
    <Page fit title={tituloProjecao(dados.perfil.nome)}subtitle={`${cargo?.nome ?? ''} · ${ms[0].rotulo} a ${ultimo.rotulo}`} aside={aviso || undefined}>
      <div className="proj-top">
        <Card className="hero">
          <div>
            <small>Saldo projetado em {ultimo.rotulo}</small>
            <div className={`big num ${r.saldoFinal < 0 ? 'neg' : ''}`}>{fmt(r.saldoFinal)}</div>
            <span className={`delta ${r.variacaoSaldo >= 0 ? 'up' : 'down'}`}>
              <Icon name={r.variacaoSaldo >= 0 ? 'up' : 'down'} size={14} />{fmt(Math.abs(r.variacaoSaldo))} em relação a hoje
            </span>
            <small className="hero-sub">Hoje {fmt(projecao.saldoInicial)} · sobra média {fmt(r.sobraMedia)}/mês</small>
          </div>
          <Spark meses={ms} saldoInicial={projecao.saldoInicial} />
        </Card>

        <div className="kpis2">
          <Kpi icon="wallet" cor="#8be9fd" titulo="Líquido médio" valor={fmt(r.liquidoMedio)} />
          <Kpi icon="card" cor="#ffb86c" titulo="Gastos médios" valor={fmt(r.gastosMedio)} />
          <Kpi icon="star" cor="#50fa7b" titulo="Melhor mês" valor={`${rotuloCompetencia(r.melhorMes.competencia)} · ${fmt(r.melhorMes.sobra)}`} classe="pos" />
          <Kpi icon="down" cor={r.piorMes.sobra < 0 ? '#ff5555' : '#bd93f9'} titulo="Pior mês" valor={`${rotuloCompetencia(r.piorMes.competencia)} · ${fmt(r.piorMes.sobra)}`} classe={r.piorMes.sobra < 0 ? 'neg' : ''} />
        </div>
      </div>

      <div className="proj-main">
        <Card className="col">
          <h2>Fluxo mensal <small>líquido × gastos e saldo acumulado</small></h2>
          <div className="fill"><FluxoChart meses={ms} onSelect={setSel} /></div>
        </Card>
        <Card className="col">
          <h2>Sobra por mês</h2>
          <div className="mlist">
            {ms.map((m) => (
              <div key={m.indice} className="mrow" tabIndex={0} role="button" onClick={() => setSel(m.indice)} onKeyDown={(e) => e.key === 'Enter' && setSel(m.indice)}>
                <span><b>{m.rotulo}</b>{m.evento && <> <span className="tag o">{m.evento}</span></>}</span>
                <div className="bar"><i style={{ width: `${(Math.abs(m.sobra) / mx) * 100}%`, background: m.sobra >= 0 ? 'var(--green)' : 'var(--red)' }} /></div>
                <span className={`num ${cls(m.sobra)}`} style={{ textAlign: 'right' }}>{fmt(m.sobra)}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <MemoriaCalculo mes={sel === null ? null : ms[sel]} onClose={() => setSel(null)} />
    </Page>
  )
}
