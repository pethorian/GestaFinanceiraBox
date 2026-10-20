import { Icon } from '../components/Icon'
import { Card, NumberInput, Page } from '../components/ui'
import { fmt } from '../domain/format'
import type { TipoLancamento } from '../domain/types'
import { useApp } from '../store/AppContext'

const TIPOS: Record<TipoLancamento, string> = { RECORRENTE: 'Todo mês', PONTUAL: 'Uma vez', PARCELADO: 'Parcelado' }

export function Gastos() {
  const { dados, categorias, resumoGastos, adicionarLancamento, editarLancamento, removerLancamento } = useApp()
  const { lancamentos } = dados

  // A média por categoria é calculada pelo back end (GET /api/lancamentos/resumo).
  const cats = resumoGastos.categorias
  const maxCat = cats[0]?.mediaMensal || 1 // só escala visual das barras

  return (
    <Page title="Gastos" subtitle="Cadastre o que você paga todo mês, uma vez só ou em parcelas.">
      <div className="cols">
        <Card className="scrollx">
          <table className="tbl-gastos">
            <thead><tr><th>Descrição</th><th>Categoria</th><th>Tipo</th><th>Valor (R$)</th><th>Mês</th><th>Parcelas</th><th /></tr></thead>
            <tbody>
              {lancamentos.map((g) => {
                const id = g.id as number
                return (
                  <tr key={id}>
                    <td><input type="text" value={g.descricao} aria-label="Descrição" onChange={(e) => editarLancamento(id, { descricao: e.target.value })} /></td>
                    <td>
                      <select value={g.categoria} aria-label="Categoria" onChange={(e) => editarLancamento(id, { categoria: e.target.value })}>
                        {categorias.map((c) => <option key={c.nome}>{c.nome}</option>)}
                      </select>
                    </td>
                    <td>
                      <select value={g.tipo} aria-label="Tipo" onChange={(e) => {
                        const tipo = e.target.value as TipoLancamento
                        editarLancamento(id, { tipo })
                      }}>
                        {Object.entries(TIPOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                      </select>
                    </td>
                    <td><NumberInput min={0} step={10} value={g.valor} style={{ width: 100 }} aria-label="Valor" onChange={(v) => editarLancamento(id, { valor: v })} /></td>
                    <td><input type="month" value={g.mesInicial} aria-label="Mês inicial" onChange={(e) => e.target.value && editarLancamento(id, { mesInicial: e.target.value })} /></td>
                    <td><NumberInput min={1} max={60} value={g.parcelas} style={{ width: 70 }} disabled={g.tipo !== 'PARCELADO'} aria-label="Parcelas" onChange={(v) => editarLancamento(id, { parcelas: v })} /></td>
                    <td><button className="btn x" onClick={() => void removerLancamento(id)} aria-label={`Remover ${g.descricao}`}><Icon name="close" size={14} /></button></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          {lancamentos.length === 0 && <p className="empty">Nenhum gasto cadastrado ainda.</p>}
          <div className="row-actions">
            <button className="btn pri" onClick={() => void adicionarLancamento()}>+ Novo gasto</button>
            <span>Média mensal: <b className="num">{fmt(resumoGastos.mediaMensalTotal)}</b></span>
          </div>
        </Card>

        <Card className="pv">
          <h2>Por categoria <small>média por mês</small></h2>
          {cats.length === 0 && <p className="empty">Sem gastos cadastrados.</p>}
          {cats.map((c) => (
            <div className="cat-row" key={c.categoria}>
              <span>{c.categoria}</span>
              <div className="pbar"><i style={{ width: `${(c.mediaMensal / maxCat) * 100}%` }} /></div>
              <span className="num" style={{ textAlign: 'right' }}>{fmt(c.mediaMensal)}</span>
            </div>
          ))}
        </Card>
      </div>
    </Page>
  )
}
