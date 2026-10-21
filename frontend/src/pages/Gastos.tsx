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
  const temParcelado = lancamentos.some((g) => g.tipo === 'PARCELADO')
  const maxCat = cats[0]?.mediaMensal || 1 // só escala visual das barras

  return (
    <Page
      title="Gastos mensais"
      subtitle="Gastos “todo mês” se repetem; os de uma vez só saem da lista sozinhos depois que o mês passa.">
      <div className="cols gcols">
        <Card className={`gcard${temParcelado ? '' : ' semparc'}`}>
          <h2>Seus gastos <small>{lancamentos.length === 1 ? '1 item' : `${lancamentos.length} itens`}</small></h2>

          {lancamentos.length === 0 ? (
            <p className="empty">Nenhum gasto nos próximos meses. Use “+ Novo gasto” para cadastrar.</p>
          ) : (
            <>
              <div className="grow ghead" aria-hidden="true">
                <span>Descrição</span><span>Categoria</span><span>Tipo</span><span>Mês</span>{temParcelado ? <span>Parcelas</span> : <span />}<span className="r">Valor</span><span />
              </div>
              <div className="glist">
                {lancamentos.map((g) => {
                  const id = g.id as number
                  const parcelado = g.tipo === 'PARCELADO'
                  return (
                    <div className="grow" key={id}>
                      <input type="text" className="desc" value={g.descricao} aria-label="Descrição" onChange={(e) => editarLancamento(id, { descricao: e.target.value })} />
                      <select value={g.categoria} aria-label="Categoria" onChange={(e) => editarLancamento(id, { categoria: e.target.value })}>
                        {categorias.map((c) => <option key={c.nome}>{c.nome}</option>)}
                      </select>
                      <select value={g.tipo} aria-label="Tipo" onChange={(e) => editarLancamento(id, { tipo: e.target.value as TipoLancamento })}>
                        {Object.entries(TIPOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                      </select>
                      <input type="month" value={g.mesInicial} aria-label="Mês inicial" onChange={(e) => e.target.value && editarLancamento(id, { mesInicial: e.target.value })} />
                      {parcelado
                        ? <NumberInput stepper min={1} max={60} value={g.parcelas} aria-label="Parcelas" onChange={(v) => editarLancamento(id, { parcelas: v })} />
                        : <span />}
                      <NumberInput prefix="R$" min={0} step={10} value={g.valor} aria-label="Valor" onChange={(v) => editarLancamento(id, { valor: v })} />
                      <button className="btn x ghost" onClick={() => void removerLancamento(id)} aria-label={`Remover ${g.descricao}`} title="Remover"><Icon name="close" size={14} /></button>
                    </div>
                  )
                })}
              </div>
            </>
          )}

          <div className="row-actions">
            <button className="btn pri" onClick={() => void adicionarLancamento()}>+ Novo gasto</button>
            <span className="dim">Média mensal: <b className="num">{fmt(resumoGastos.mediaMensalTotal)}</b></span>
          </div>
        </Card>

        <Card className="pv">
          <h2>Por categoria <small>média por mês</small></h2>
          {cats.length === 0 && <p className="empty">Sem gastos cadastrados.</p>}
          {cats.map((c) => (
            <div className="cat-row" key={c.categoria}>
              <div className="cat-head">
                <span>{c.categoria}</span>
                <span className="num">{fmt(c.mediaMensal)}</span>
              </div>
              <div className="pbar"><i style={{ width: `${(c.mediaMensal / maxCat) * 100}%` }} /></div>
            </div>
          ))}
        </Card>
      </div>
    </Page>
  )
}
