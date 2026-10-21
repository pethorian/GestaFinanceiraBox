import { useEffect, type ReactNode } from 'react'
import { fmt, fmtAliquota, tituloCompetencia } from '../domain/format'
import type { MesProjecao } from '../domain/types'
import { AvisoPlr } from './AvisoPlr'
import { Icon } from './Icon'
import { cls } from './ui'

const L = ({ a, b, c = '' }: { a: ReactNode; b: ReactNode; c?: string }) => (
  <div><span>{a}</span><span className={`num ${c}`}>{b}</span></div>
)
const Passo = ({ a, b }: { a: string; b: string }) => (
  <div className="st"><span>{a}</span><span>{b}</span></div>
)

/** Painel lateral com a memória de cálculo de um mês. Todos os valores vêm prontos do back end. */
export function MemoriaCalculo({ mes, onClose }: { mes: MesProjecao | null; onClose: () => void }) {
  useEffect(() => {
    if (!mes) return
    const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', h)
    return () => document.removeEventListener('keydown', h)
  }, [mes, onClose])

  const m = mes
  return (
    <aside id="drawer" className={m ? 'open' : ''} aria-hidden={!m} aria-label="Memória de cálculo">
      {m && (
        <>
          <button className="btn x" style={{ float: 'right' }} onClick={onClose} aria-label="Fechar"><Icon name="close" size={16} /></button>
          <h2 style={{ fontSize: 18, margin: 0 }}>{tituloCompetencia(m.competencia)}</h2>
          <small style={{ color: 'var(--mut)' }}>Memória de cálculo {m.evento && <span className="tag o">{m.evento}</span>}</small>
          {m.plr > 0 && <AvisoPlr />}

          <div className="memo"><h4>Receitas</h4>
            <L a="Remuneração base" b={fmt(m.base)} />
            {m.sobreaviso > 0 && <L a="Sobreaviso" b={fmt(m.sobreaviso)} />}
            {m.creche > 0 && <L a="Auxílio creche" b={fmt(m.creche)} />}
            {m.homeOffice > 0 && <L a="Home office" b={fmt(m.homeOffice)} />}
            {m.vrVa > 0 && <L a="VR + VA" b={fmt(m.vrVa)} />}
            {m.decimo > 0 && <L a="13º (parcela)" b={fmt(m.decimo)} />}
            {m.ferias > 0 && <L a="Férias" b={fmt(m.ferias)} />}
            {m.plr > 0 && <L a="PLR (parcela)" b={fmt(m.plr)} />}
            {m.bonus > 0 && <L a="Bônus Caixa" b={fmt(m.bonus)} />}
            <L a={<b>Total</b>} b={<b>{fmt(m.receitas)}</b>} />
          </div>

          <div className="memo"><h4>INSS</h4>
            <Passo a={`Base ${fmt(m.baseInss)}${m.tetoInssAplicado ? ' · teto aplicado' : ''}`} b="" />
            <L a="INSS" b={`−${fmt(m.inss)}`} c="neg" />
          </div>

          <div className="memo"><h4>IRPF</h4>
            <Passo a="1. Dedução legal = INSS + FUNCEF" b={fmt(m.irpf.deducaoLegal)} />
            <Passo a={`2. Aplicável = MAX(legal; ${fmt(m.irpf.deducaoMinima).replace('R$', '').trim()})`} b={fmt(m.irpf.deducao)} />
            <Passo a="3. Base de cálculo" b={fmt(m.irpf.baseCalculo)} />
            <Passo a={`4. ${m.irpf.aliquota ? `Faixa ${fmtAliquota(m.irpf.aliquota)}` : 'Faixa isenta'} − parcela a deduzir`} b={fmt(m.irpf.bruto)} />
            <Passo a="5. Redução do imposto" b={m.irpf.reducao ? `−${fmt(m.irpf.reducao)}` : 'sem redução'} />
            <L a="IRPF" b={`−${fmt(m.irpf.imposto)}`} c="neg" />
          </div>

          <div className="memo"><h4>Outros descontos</h4>
            <L a="Saúde Caixa" b={fmt(m.saude)} />
            <L a="FUNCEF (guardada na previdência)" b={fmt(m.funcef)} />
            <L a="Vale-transporte" b={fmt(m.valeTransporte)} />
            <L a="Sindicato" b={fmt(m.sindicato)} />
            {m.descontoDecimo > 0 && <L a="INSS + IR do 13º" b={fmt(m.descontoDecimo)} />}
          </div>

          <div className="memo"><h4>Resultado</h4>
            <L a="Líquido" b={fmt(m.liquido)} />
            <L a="Gastos" b={`−${fmt(m.gastos)}`} />
            {m.aportes > 0 && <L a="Aportes em metas" b={`−${fmt(m.aportes)}`} />}
            <L a={<b>Sobra</b>} b={<b>{fmt(m.sobra)}</b>} c={cls(m.sobra)} />
            <L a="Saldo disponível acumulado" b={fmt(m.acumulado)} />
          </div>

          {m.metas.length > 0 && (
            <div className="memo"><h4>Metas</h4>
              {m.metas.map((a) => (
                <Passo key={a.id} a={`${a.nome}: aporte ${fmt(a.aporte)}`} b={fmt(a.guardado)} />
              ))}
              <L a="Guardado em metas" b={fmt(m.guardado)} />
              <L a={<b>Patrimônio (disponível + guardado)</b>} b={<b>{fmt(m.patrimonio)}</b>} />
            </div>
          )}

          {m.previdencia > 0 && (
            <div className="memo"><h4>Guardada (previdência), não é gasto</h4>
              {m.funcef + m.funcefDecimo > 0 && <L a={m.funcefDecimo > 0 ? 'FUNCEF do mês (com 13º)' : 'FUNCEF do mês'} b={fmt(m.funcef + m.funcefDecimo)} />}
              <L a="Saldo na FUNCEF ao fim do mês" b={fmt(m.previdencia)} />
              <L a={<b>Patrimônio total (com previdência)</b>} b={<b>{fmt(m.patrimonioTotal)}</b>} />
            </div>
          )}
        </>
      )}
    </aside>
  )
}
