import { useId, useState } from 'react'
import { fmt } from '../domain/format'
import { FAIXAS, fmtPctFaixa, rotuloStatus } from '../domain/saude'
import type { FaixaMetodo, SaudeFinanceira, StatusFaixa } from '../domain/types'
import { Icon } from './Icon'
import { useSize } from './charts'

/*
 * Gráficos da aba Saúde financeira (SVG próprio, sem bibliotecas).
 * Cores por faixa nos tokens --f-nec / --f-des / --f-pou (validadas contra a superfície dos cards, tema escuro).
 * Nenhum cálculo de negócio: valores, percentuais e status vêm do back end; aqui só se converte valor em pixel.
 */

const TINTA_SOBRE_COR = '#14151c' // texto dentro das barras coloridas (contraste melhor que branco nessas cores)

/** Retângulo com cantos arredondados só na ponta do dado (direita); a base (esquerda) fica reta. */
function barraPath(x: number, y: number, w: number, h: number) {
  const r = Math.min(4, w / 2, h / 2)
  if (w <= 0) return ''
  return `M${x},${y}H${x + w - r}A${r},${r} 0 0 1 ${x + w},${y + r}V${y + h - r}A${r},${r} 0 0 1 ${x + w - r},${y + h}H${x}Z`
}

/** Escala do eixo em % da renda: cobre a meta e o maior valor com folga para o rótulo na ponta. */
export function escalaPct(valores: (number | null)[]) {
  const mx = Math.max(50, ...valores.map((v) => v ?? 0))
  return Math.max(60, Math.ceil((mx * 1.18) / 10) * 10)
}
/** Passo do eixo: o menor "redondo" que deixa pelo menos 38px entre as marcas. */
const passoEixo = (escala: number, largura: number) =>
  [10, 20, 25, 50, 100].find((p) => (p / escala) * largura >= 38) ?? 100
const ticks = (escala: number, largura: number) => {
  const p = passoEixo(escala, largura)
  return Array.from({ length: Math.floor(escala / p) + 1 }, (_, i) => i * p)
}

/** Uma faixa: barra do % atual da renda, com o marcador vertical da meta do método. */
function BarraMeta({ pct, meta, escala, cor, rotulo }: { pct: number | null; meta: number; escala: number; cor: string; rotulo: string }) {
  const [ref, { w: W, h: H }] = useSize(34, 40)
  const x = (v: number) => (Math.min(v, escala) / escala) * W
  const t = Math.min(20, Math.max(10, H - 16)) // espessura da barra (≤ 24px)
  const y0 = (H - t) / 2
  const xm = x(meta)
  const tip = pct === null ? 0 : x(pct)
  const rotuloFora = W - tip > 52
  return (
    <div className="sf-plot" ref={ref}>
      <svg width={W} height={H} role="img" aria-label={rotulo}>
        <title>{rotulo}</title>
        {ticks(escala, W).map((v) => (
          <line key={v} x1={x(v) + 0.5} x2={x(v) + 0.5} y1={0} y2={H} className="sf-grid" />
        ))}
        <rect x={0} y={y0} width={W} height={t} rx={4} className="sf-track" />
        {pct !== null && <path d={barraPath(0, y0, tip, t)} style={{ fill: cor }} />}
        {/* marcador da meta: anel na cor da superfície + traço na cor da tinta */}
        <line x1={xm} x2={xm} y1={y0 - 6} y2={y0 + t + 6} className="sf-ring" />
        <line x1={xm} x2={xm} y1={y0 - 6} y2={y0 + t + 6} className="sf-meta" />
        {pct !== null ? (
          <text
            x={rotuloFora ? tip + 8 : tip - 8} y={H / 2 + 4} textAnchor={rotuloFora ? 'start' : 'end'}
            className="sf-val" style={rotuloFora ? undefined : { fill: TINTA_SOBRE_COR }}
          >{fmtPctFaixa(pct)}</text>
        ) : (
          <text x={8} y={H / 2 + 4} className="sf-val">—</text>
        )}
      </svg>
    </div>
  )
}

function Eixo({ escala }: { escala: number }) {
  const [ref, { w: W }] = useSize(18, 40)
  const x = (v: number) => (v / escala) * W
  const ts = ticks(escala, W)
  return (
    <div className="sf-axis" ref={ref}>
      <svg width={W} height={18} aria-hidden="true">
        {ts.map((v, i) => (
          <text key={v} x={x(v)} y={13} textAnchor={i === 0 ? 'start' : i === ts.length - 1 && x(v) > W - 16 ? 'end' : 'middle'}>{v}%</text>
        ))}
      </svg>
    </div>
  )
}

export function StatusPill({ faixa, saude }: { faixa: FaixaMetodo; saude: SaudeFinanceira }) {
  return <StatusFaixaPill faixa={faixa} status={saude.status[faixa]} />
}

/** Pílula de status de uma faixa (ícone + texto, nunca só cor). */
export function StatusFaixaPill({ faixa, status }: { faixa: FaixaMetodo; status: StatusFaixa }) {
  const s = rotuloStatus(faixa, status)
  return (
    <span className={`sf-st ${s.tom}`}>
      <Icon name={s.tom === 'ok' ? 'check' : 'warn'} size={13} strokeWidth={2.2} />{s.texto}
    </span>
  )
}

/**
 * Gráfico principal: % da renda em cada faixa (barra) contra a meta do método (marcador).
 * Linhas em HTML (rótulo, valores e status ficam legíveis sem tooltip) com o traçado em SVG.
 */
export function MetaAtualChart({ saude }: { saude: SaudeFinanceira }) {
  const pcts = saude.percentuais
  const escala = escalaPct(FAIXAS.map((f) => pcts?.[f.faixa] ?? null))
  return (
    <div className="sf-bullet" role="group" aria-label="Recomendado pelo método × seu mês, por faixa, em % da renda">
      {FAIXAS.map((f) => {
        const pct = pcts?.[f.faixa] ?? null
        const st = rotuloStatus(f.faixa, saude.status[f.faixa])
        const rotulo = `${f.nome}: ${fmtPctFaixa(pct)} da renda (${fmt(saude.atual[f.faixa])}); meta ${f.rotuloMeta} (${fmt(saude.recomendado[f.faixa])}); ${st.texto}`
        return (
          <div key={f.faixa} className="sf-row">
            <div className="sf-lbl">
              <div className="sf-lbl-l1">
                <b><i className="sf-sw" style={{ background: f.cor }} />{f.nome}</b>
                <StatusPill faixa={f.faixa} saude={saude} />
              </div>
              <span className="num">{fmt(saude.atual[f.faixa])} <em>de {fmt(saude.recomendado[f.faixa])}</em></span>
            </div>
            <BarraMeta pct={pct} meta={f.meta} escala={escala} cor={f.cor} rotulo={rotulo} />
          </div>
        )
      })}
      <div className="sf-row sf-row-axis">
        <span className="sf-key"><i className="sf-mk" />meta do método</span>
        <Eixo escala={escala} />
      </div>
    </div>
  )
}

interface Segmento { chave: string; nome: string; valor: number; pct: string; cor: string; livre?: boolean }
interface Dica { x: number; y: number; titulo: string; linhas: [string, string][] }

/**
 * Como a renda se divide: barra 100% "Ideal" (50/30/20) acima da barra "Você".
 * As duas usam a mesma escala em reais; se os gastos e aportes passam da renda, aparece o marcador da renda.
 */
export function DivisaoRendaChart({ saude, compacto }: { saude: SaudeFinanceira; compacto?: boolean }) {
  const [ref, { w: W }] = useSize(40, 160)
  const id = useId()
  const [dica, setDica] = useState<Dica | null>(null)
  const renda = Math.max(0, saude.rendaMensal)
  const excesso = Math.max(0, -saude.sobraLivre)
  const total = renda + excesso || 1
  const LW = 46
  const PR = 4
  const bh = compacto ? 16 : 18
  const gapLinha = compacto ? 12 : 16
  const topo = excesso > 0 ? 14 : 2
  const H = topo + bh * 2 + gapLinha + 2
  const iw = Math.max(10, W - LW - PR)
  const u = iw / total
  const xRenda = LW + renda * u

  const ideal: Segmento[] = FAIXAS.map((f) => ({
    chave: f.faixa, nome: f.nome, valor: saude.recomendado[f.faixa], pct: `${f.meta}%`, cor: f.cor,
  }))
  const voce: Segmento[] = FAIXAS.map((f) => ({
    chave: f.faixa, nome: f.nome, valor: saude.atual[f.faixa], pct: fmtPctFaixa(saude.percentuais?.[f.faixa]), cor: f.cor,
  }))
  if (saude.sobraLivre > 0) voce.push({ chave: 'livre', nome: 'Sem destino', valor: saude.sobraLivre, pct: '', cor: 'var(--f-livre)', livre: true })

  const linha = (nome: string, segs: Segmento[], y: number, idx: number) => {
    let x = LW
    const visiveis = segs.filter((s) => s.valor > 0)
    const largura = visiveis.reduce((a, s) => a + s.valor * u, 0)
    const clip = `${id}-c${idx}`
    return (
      <g key={nome}>
        <text x={0} y={y + bh / 2 + 4} className="sf-lbl-svg">{nome}</text>
        <clipPath id={clip}><rect x={LW} y={y} width={Math.max(0, largura)} height={bh} rx={4} /></clipPath>
        {visiveis.length === 0 && <rect x={LW} y={y} width={iw} height={bh} rx={4} className="sf-track" />}
        <g clipPath={`url(#${clip})`}>
          {visiveis.map((s, i) => {
            const w = s.valor * u
            const x0 = x
            x += w
            const wDesenho = Math.max(0, w - (i < visiveis.length - 1 ? 2 : 0)) // 2px de respiro entre segmentos
            // rótulo interno só se cabe com folga e não fica embaixo do marcador da renda
            const cabe = !!s.pct && wDesenho >= 40 && !(excesso > 0 && Math.abs(xRenda - (x0 + wDesenho / 2)) < 24)
            const mostrar = () => setDica({
              x: x0 + w / 2, y,
              titulo: `${nome === 'Ideal' ? 'Ideal' : 'Você'} · ${s.nome}`,
              linhas: s.livre
                ? [['Valor', fmt(s.valor)]]
                : [['Valor', fmt(s.valor)], ['% da renda', s.pct]],
            })
            return (
              <g key={s.chave}>
                <rect x={x0} y={y} width={wDesenho} height={bh} style={{ fill: s.cor }} />
                {cabe && <text x={x0 + wDesenho / 2} y={y + bh / 2 + 4} textAnchor="middle" className="sf-in">{s.pct}</text>}
                <rect
                  x={x0} y={y - 3} width={w} height={bh + 6} className="hv"
                  tabIndex={0} role="img" aria-label={`${nome}, ${s.nome}: ${fmt(s.valor)}${s.pct ? `, ${s.pct} da renda` : ''}`}
                  onMouseEnter={mostrar} onFocus={mostrar} onMouseLeave={() => setDica(null)} onBlur={() => setDica(null)}
                />
              </g>
            )
          })}
        </g>
      </g>
    )
  }

  const yIdeal = topo
  const yVoce = topo + bh + gapLinha
  return (
    <div className="sf-div">
      <div className="chartw sf-divw" ref={ref} style={{ height: H }}>
        <svg width={W} height={H} role="group" aria-label="Como a renda se divide: ideal do método × seu mês">
          {linha('Ideal', ideal, yIdeal, 0)}
          {linha('Você', voce, yVoce, 1)}
          {excesso > 0 && (
            <g pointerEvents="none">
              <line x1={xRenda} x2={xRenda} y1={topo - 4} y2={yVoce + bh + 2} className="sf-ring" />
              <line x1={xRenda} x2={xRenda} y1={topo - 4} y2={yVoce + bh + 2} className="sf-meta" />
              <text x={xRenda} y={9} textAnchor={xRenda > W - 40 ? 'end' : 'middle'} className="sf-val">renda</text>
            </g>
          )}
        </svg>
        <div className="tt" style={dica ? { opacity: 1, left: Math.min(W - 90, Math.max(90, dica.x)), top: dica.y } : undefined}>
          {dica && (
            <>
              <b>{dica.titulo}</b>
              {dica.linhas.map(([k, v]) => <div key={k}><span>{k}</span><span className="num">{v}</span></div>)}
            </>
          )}
        </div>
      </div>
      <div className="sf-legend">
        {FAIXAS.map((f) => <span key={f.faixa}><i className="sf-sw" style={{ background: f.cor }} />{f.nome}</span>)}
        {saude.sobraLivre > 0 && <span><i className="sf-sw" style={{ background: 'var(--f-livre)' }} />Sem destino</span>}
        {excesso > 0 && <span><i className="sf-mk" />Renda (gastos passam dela)</span>}
      </div>
    </div>
  )
}
