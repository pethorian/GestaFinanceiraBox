import { useState } from 'react'
import { api } from '../api/api'
import { Card, NumberInput, Page } from '../components/ui'
import { rotuloCompetenciaLongo } from '../domain/format'
import type { Beneficio, Cargo, TabelaFaixas } from '../domain/types'
import { useApp } from '../store/AppContext'

type Aba = 'irpf' | 'inss' | 'cargos' | 'benef'
const ABAS: { id: Aba; label: string }[] = [
  { id: 'irpf', label: 'IRPF' }, { id: 'inss', label: 'INSS' }, { id: 'cargos', label: 'Cargos' }, { id: 'benef', label: 'Benefícios' },
]
const vig = (inicio: string | null) => <span className="tag">{inicio ? `${rotuloCompetenciaLongo(inicio.slice(0, 7))} →` : '—'}</span>
// alíquota: o banco guarda fração (0,075); a tela edita em % (7,5). Só conversão de unidade.
const paraPct = (f: number) => Number((f * 100).toFixed(3))
const paraFracao = (p: number) => Number((p / 100).toFixed(5))

/** Faixas de INSS ou IRPF da vigência atual: edição local e gravação em PUT /api/parametros/{inss|irpf}. */
function Faixas({ tabela, titulo, salvar }: { tabela: TabelaFaixas; titulo: string; salvar: (t: TabelaFaixas) => Promise<unknown> }) {
  const { salvarParametros } = useApp()
  const [faixas, setFaixas] = useState(tabela.faixas)
  const editar = (i: number, patch: Partial<TabelaFaixas['faixas'][number]>) =>
    setFaixas((fs) => fs.map((f, k) => (k === i ? { ...f, ...patch } : f)))
  const alterado = JSON.stringify(faixas) !== JSON.stringify(tabela.faixas)
  return (
    <>
      <table>
        <thead><tr><th>{titulo}</th><th>Alíquota (%)</th><th>Parcela a deduzir</th><th>Vigência</th></tr></thead>
        <tbody>{faixas.map((f, i) => (
          <tr key={i}>
            <td>{f.ate === null ? <input type="text" value="acima" readOnly /> : <NumberInput step={0.01} value={f.ate} onChange={(v) => editar(i, { ate: v })} />}</td>
            <td><NumberInput step={0.5} value={paraPct(f.aliquota)} onChange={(v) => editar(i, { aliquota: paraFracao(v) })} /></td>
            <td><NumberInput step={0.01} value={f.deducao} onChange={(v) => editar(i, { deducao: v })} /></td>
            <td>{vig(tabela.vigenciaInicio)}</td>
          </tr>
        ))}</tbody>
      </table>
      <div style={{ marginTop: 12 }}>
        <button className="btn pri" disabled={!alterado} onClick={() => void salvarParametros(() => salvar({ ...tabela, faixas }))}>Salvar tabela</button>
      </div>
    </>
  )
}

const CAMPOS_BENEFICIO: { chave: keyof Beneficio; rotulo: string }[] = [
  { chave: 'pai', rotulo: 'Auxílio creche (PAI) por filho' },
  { chave: 'homeOffice', rotulo: 'Auxílio home office' },
  { chave: 'vrVa', rotulo: 'VR + cesta-alimentação' },
  { chave: 'bonusCaixaMaxSalarios', rotulo: 'Bônus Caixa — máximo de salários' },
  { chave: 'sindicatoTeto', rotulo: 'Teto do sindicato' },
  { chave: 'planoPorDependente', rotulo: 'Valor por dependente (Saúde Caixa)' },
  { chave: 'inssTeto', rotulo: 'Teto de contribuição do INSS' },
  { chave: 'inssTetoSalario', rotulo: 'Salário a partir do qual vale o teto do INSS' },
]

function Beneficios({ beneficio }: { beneficio: Beneficio }) {
  const { salvarParametros } = useApp()
  const [b, setB] = useState(beneficio)
  const alterado = JSON.stringify(b) !== JSON.stringify(beneficio)
  return (
    <>
      <table><tbody>{CAMPOS_BENEFICIO.map((c) => (
        <tr key={c.chave}>
          <td>{c.rotulo}</td>
          <td style={{ maxWidth: 160 }}><NumberInput step={0.01} value={b[c.chave] as number} onChange={(v) => setB({ ...b, [c.chave]: v })} /></td>
        </tr>
      ))}</tbody></table>
      <div style={{ marginTop: 12 }}>
        <button className="btn pri" disabled={!alterado} onClick={() => void salvarParametros(() => api.salvarBeneficio(b))}>Salvar benefícios</button>
        <span className="sub" style={{ marginLeft: 10 }}>Vigência: {vig(beneficio.vigenciaInicio)}</span>
      </div>
    </>
  )
}

function Cargos({ cargos }: { cargos: Cargo[] }) {
  const { salvarParametros } = useApp()
  const [lista, setLista] = useState(cargos)
  const editar = (id: number, patch: Partial<Cargo>) => setLista((l) => l.map((c) => (c.id === id ? { ...c, ...patch } : c)))
  return (
    <>
      <table><thead><tr><th>Cargo / função</th><th>Jornada</th><th>Piso</th><th>Gratificação</th><th>PLR anual (R$)</th><th /></tr></thead>
        <tbody>{lista.map((c, i) => {
          const alterado = JSON.stringify(c) !== JSON.stringify(cargos[i])
          return (
            <tr key={c.id}>
              <td><input type="text" value={c.nome} readOnly /></td>
              <td><input type="text" value={`${c.jornada}h`} readOnly /></td>
              <td><NumberInput step={0.01} value={c.piso} onChange={(v) => editar(c.id, { piso: v })} /></td>
              <td><NumberInput step={0.01} value={c.gratificacao} onChange={(v) => editar(c.id, { gratificacao: v })} /></td>
              <td><NumberInput step={0.01} value={c.plrAnual} onChange={(v) => editar(c.id, { plrAnual: v })} /></td>
              <td><button className="btn sm" disabled={!alterado} onClick={() => void salvarParametros(() => api.salvarCargo(c))}>Salvar</button></td>
            </tr>
          )
        })}</tbody></table>
      <p className="sub" style={{ margin: '12px 0 0' }}>Os valores vêm da Tabela Salarial 2026 da Caixa (piso, gratificação e PLR anual de cada cargo). A base é o maior entre o piso e (delta + gratificação).</p>
    </>
  )
}

/** Tabelas vigentes lidas e gravadas no banco (GET /api/parametros/vigentes e PUT /api/parametros/*). */
export function Parametros() {
  const [aba, setAba] = useState<Aba>('irpf')
  const { parametros: p, restaurar } = useApp()

  return (
    <Page title="Parâmetros" subtitle="Tabelas que mudam todo ano. Cada uma tem vigência, e a projeção usa a vigente em cada mês.">
      <div className="seg2" role="tablist">
        {ABAS.map((a) => (
          <button key={a.id} role="tab" aria-selected={aba === a.id} className={aba === a.id ? 'on' : ''} onClick={() => setAba(a.id)}>{a.label}</button>
        ))}
      </div>
      <Card className="scrollx">
        {/* key: ao recarregar os parâmetros, o rascunho local volta ao que está no banco */}
        {aba === 'irpf' && <Faixas key={JSON.stringify(p.irpf)} tabela={p.irpf} titulo="Base até (R$)" salvar={api.salvarIrpf} />}
        {aba === 'inss' && <Faixas key={JSON.stringify(p.inss)} tabela={p.inss} titulo="Salário até (R$)" salvar={api.salvarInss} />}
        {aba === 'cargos' && <Cargos key={JSON.stringify(p.cargos)} cargos={p.cargos} />}
        {aba === 'benef' && <Beneficios key={JSON.stringify(p.beneficios)} beneficio={p.beneficios} />}
      </Card>
      <p className="sub" style={{ marginTop: 18 }}>
        <button className="btn sm" onClick={() => { if (confirm('Restaurar os dados padrão? Isso apaga perfil, gastos, 13º e metas salvos no banco.')) void restaurar() }}>Restaurar dados padrão</button>
      </p>
    </Page>
  )
}
