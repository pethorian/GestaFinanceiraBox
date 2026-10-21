import { useLayoutEffect, useRef, useState } from 'react'
import { fmt, tituloCompetencia } from '../domain/format'
import type { MesProjecao } from '../domain/types'
import { cls } from './ui'

const P = { l: 46, r: 10, t: 30, b: 28 }

/** Mede o contêiner para o SVG ocupar exatamente o espaço disponível. */
export function useSize(minH: number, minW = 300) {
  const ref = useRef<HTMLDivElement>(null)
  const [s, setS] = useState({ w: 720, h: 260 })
  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    const medir = () => setS({ w: Math.max(minW, el.clientWidth), h: Math.max(minH, el.clientHeight) })
    medir()
    const ro = new ResizeObserver(medir)
    ro.observe(el)
    return () => ro.disconnect()
  }, [minH, minW])
  return [ref, s] as const
}

/**
 * Saídas do mês na ordem de empilhamento. As do 13º/PLR usam a mesma cor da saída equivalente em tom claro:
 * gastos com adicionais (lazer/obrigatório) junto dos gastos; reservas dos adicionais junto dos aportes.
 */
const segmentosSaidas = (m: MesProjecao) => [
  { chave: 'g', v: m.gastos, fill: 'url(#go)', op: 1 },
  { chave: 'ga', v: m.gastosAdicionais, fill: '#ffb86c', op: 0.5 },
  { chave: 'a', v: m.aportes, fill: '#bd93f9', op: 0.85 },
  { chave: 'ra', v: m.reservasAdicionais, fill: '#bd93f9', op: 0.45 },
]
/** topo da barra de saídas: só para a escala do eixo e a posição do tooltip */
const topoSaidas = (m: MesProjecao) => segmentosSaidas(m).reduce((a, s) => a + Math.max(0, s.v), 0)

/** Barras de líquido × saídas (gastos, aportes e o uso do 13º/PLR empilhados) com a linha do saldo disponível e tooltip. */
export function FluxoChart({ meses, onSelect }: { meses: MesProjecao[]; onSelect: (i: number) => void }) {
  const [ref, { w: W, h: H }] = useSize(150)
  const [hover, setHover] = useState<number | null>(null)
  const iw = W - P.l - P.r
  const ih = H - P.t - P.b
  // topo da barra de saídas só para a escala do eixo (os valores em si vêm do back end)
  const vals = meses.flatMap((m) => [m.liquido, topoSaidas(m), m.acumulado])
  const temAdicionais = meses.some((m) => m.gastosAdicionais > 0 || m.reservasAdicionais > 0)
  const max = Math.max(...vals) * 1.08
  const min = Math.min(0, ...vals) * 1.08
  const y = (v: number) => P.t + ih - ((v - min) / (max - min || 1)) * ih
  const x = (i: number) => P.l + (iw * (i + 0.5)) / 12
  const bw = (iw / 12) * 0.28
  const bar = (xx: number, v: number, fill: string) => (
    <rect x={xx} y={y(v)} width={bw} height={Math.max(0, y(0) - y(v))} rx={4} fill={fill} />
  )
  /** Saídas empilhadas a partir da base (gastos, gastos com adicionais, aportes, reservas dos adicionais), com 1,5px de respiro. */
  const pilhaSaidas = (m: MesProjecao, xx: number) => {
    let topo = y(0)
    return segmentosSaidas(m).filter((s) => s.v > 0).map((s, k) => {
      const respiro = k > 0 ? 1.5 : 0
      const alt = Math.max(0, y(0) - y(s.v) - respiro)
      topo -= respiro + alt
      return <rect key={s.chave} x={xx} y={topo} width={bw} height={alt} rx={k === 0 ? 4 : 3} fill={s.fill} fillOpacity={s.op} />
    })
  }
  const pontos = meses.map((m, i) => `${x(i)},${y(m.acumulado)}`).join(' ')
  const h = hover !== null ? meses[hover] : null

  return (
    <div className="chartw" ref={ref}>
      <svg width={W} height={H} role="img" aria-label="Gráfico de líquido, gastos, aportes em metas e saldo disponível por mês">
        <defs>
          <linearGradient id="gl" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#8be9fd" /><stop offset="1" stopColor="#8be9fd" stopOpacity=".35" /></linearGradient>
          <linearGradient id="go" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#ffb86c" /><stop offset="1" stopColor="#ffb86c" stopOpacity=".35" /></linearGradient>
        </defs>
        {[0, 1, 2, 3, 4].map((k) => {
          const v = min + ((max - min) * k) / 4
          return (
            <g key={k}>
              <line x1={P.l} x2={W - P.r} y1={y(v)} y2={y(v)} stroke="rgba(248,248,242,.07)" strokeDasharray="3 5" />
              <text x={P.l - 8} y={y(v) + 4} textAnchor="end">{(v / 1000).toFixed(0)}k</text>
            </g>
          )
        })}
        {meses.map((m, i) => (
          <g key={m.indice}>
            {bar(x(i) - bw - 1.5, m.liquido, 'url(#gl)')}
            {pilhaSaidas(m, x(i) + 1.5)}
          </g>
        ))}
        <polyline points={pontos} fill="none" stroke="#50fa7b" strokeWidth={2.5} strokeLinejoin="round" strokeLinecap="round" />
        {meses.map((m, i) => (
          <g key={m.indice}>
            <rect
              className="hv" x={P.l + (iw * i) / 12} y={P.t} width={iw / 12} height={ih}
              tabIndex={0} role="button" aria-label={`${m.rotulo}: sobra ${fmt(m.sobra)}, saldo disponível ${fmt(m.acumulado)}`}
              onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}
              onFocus={() => setHover(i)} onBlur={() => setHover(null)}
              onClick={() => onSelect(i)} onKeyDown={(e) => e.key === 'Enter' && onSelect(i)}
            />
            <circle cx={x(i)} cy={y(m.acumulado)} r={4} fill="#1e1f29" stroke="#50fa7b" strokeWidth={2.5} pointerEvents="none" />
            <text x={x(i)} y={H - 8} textAnchor="middle">{m.rotulo}</text>
          </g>
        ))}
        <g transform={`translate(${P.l},4)`}>
          <circle cx="5" cy="5" r="4.5" fill="#8be9fd" /><text x="14" y="9">Líquido</text>
          <circle cx="75" cy="5" r="4.5" fill="#ffb86c" /><text x="84" y="9">Gastos</text>
          <circle cx="140" cy="5" r="4.5" fill="#bd93f9" /><text x="149" y="9">Aportes</text>
          <circle cx="210" cy="5" r="4.5" fill="#50fa7b" /><text x="219" y="9">Saldo disponível</text>
          {temAdicionais && <><rect x="320" y="0.5" width="9" height="9" rx="2.5" fill="#ffb86c" fillOpacity={0.5} /><text x="334" y="9">tom claro: 13º/PLR</text></>}
        </g>
      </svg>
      <div
        className="tt"
        style={h ? {
          opacity: 1,
          left: Math.min(W - 95, Math.max(95, x(hover!))),
          top: Math.max(24, y(Math.max(h.liquido, topoSaidas(h), h.acumulado))),
        } : undefined}
      >
        {h && (
          <>
            <b>{tituloCompetencia(h.competencia)}{h.evento ? ` · ${h.evento}` : ''}</b>
            <div><span>Líquido</span><span className="num">{fmt(h.liquido)}</span></div>
            <div><span>Gastos</span><span className="num">{fmt(h.gastos)}</span></div>
            {h.aportes > 0 && <div><span>Aportes em metas</span><span className="num">{fmt(h.aportes)}</span></div>}
            {h.gastosAdicionais > 0 && <div><span>Gastos com adicionais</span><span className="num">{fmt(h.gastosAdicionais)}</span></div>}
            {h.reservasAdicionais > 0 && <div><span>Reservas dos adicionais</span><span className="num">{fmt(h.reservasAdicionais)}</span></div>}
            <div><span>Sobra</span><span className={`num ${cls(h.sobra)}`}>{fmt(h.sobra)}</span></div>
            <div><span>Saldo disponível</span><span className="num">{fmt(h.acumulado)}</span></div>
            {h.guardado > 0 && <div><span>Guardado em metas</span><span className="num">{fmt(h.guardado)}</span></div>}
            {h.previdencia > 0 && <div><span>Previdência FUNCEF</span><span className="num">{fmt(h.previdencia)}</span></div>}
          </>
        )}
      </div>
    </div>
  )
}

/** Mini gráfico da evolução do saldo disponível (acumulado, já sem os aportes). */
export function Spark({ meses, saldoInicial }: { meses: MesProjecao[]; saldoInicial: number }) {
  const [ref, { w, h }] = useSize(60)
  const pad = 8
  const v = meses.map((m) => m.acumulado)
  const mx = Math.max(...v, saldoInicial)
  const mn = Math.min(...v, saldoInicial, 0)
  const x = (i: number) => pad + ((w - 2 * pad) * i) / 11
  const y = (a: number) => pad + (h - 2 * pad) * (1 - (a - mn) / (mx - mn || 1))
  const pts = meses.map((m, i) => `${x(i)},${y(m.acumulado)}`).join(' ')
  return (
    <div className="spark" ref={ref}>
      <svg width={w} height={h} role="img" aria-label="Evolução do saldo acumulado">
        <defs>
          <linearGradient id="sg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#50fa7b" stopOpacity=".35" /><stop offset="1" stopColor="#50fa7b" stopOpacity="0" /></linearGradient>
        </defs>
        <polygon points={`${x(0)},${h} ${pts} ${x(11)},${h}`} fill="url(#sg)" />
        <polyline points={pts} fill="none" stroke="#50fa7b" strokeWidth={3} strokeLinejoin="round" strokeLinecap="round" />
        <circle cx={x(11)} cy={y(v[11])} r={6} fill="#1e1f29" stroke="#50fa7b" strokeWidth={3} />
      </svg>
    </div>
  )
}
