import { Icon } from '../components/Icon'
import { Card, NumberInput, Page } from '../components/ui'
import { fmt } from '../domain/format'
import type { Categoria, Lancamento, ResumoGastos, TipoLancamento } from '../domain/types'

const TIPOS_FIXOS: Partial<Record<TipoLancamento, string>> = { RECORRENTE: 'Todo mês', PARCELADO: 'Parcelado' }

export interface ListaGastosProps {
  /** FIXOS mostra tipo e parcelas; EVENTUAIS (só PONTUAL) esconde as duas colunas */
  grupo: 'FIXOS' | 'EVENTUAIS'
  titulo: string
  subtitulo: string
  rotuloNovo: string
  lancamentos: Lancamento[]
  /** média por categoria, calculada pelo back end (GET /api/lancamentos/resumo?grupo=...) */
  resumo: ResumoGastos
  categorias: Categoria[]
  onAdicionar: () => void
  onEditar: (id: number, patch: Partial<Lancamento>) => void
  onRemover: (id: number) => void
}

/** Lista editável de gastos e card "Por categoria"; usada pelas abas Gastos fixos e Gastos eventuais. */
export function ListaGastos(p: ListaGastosProps) {
  const fixos = p.grupo === 'FIXOS'
  const { resumo, categorias } = p
  const lancamentos = p.lancamentos.filter((g) => (g.tipo === 'PONTUAL') !== fixos)
  const cats = resumo.categorias
  const temParcelado = fixos && lancamentos.some((g) => g.tipo === 'PARCELADO')
  const maxCat = cats[0]?.mediaMensal || 1 // só escala visual das barras

  return (
    <Page title={p.titulo} subtitle={p.subtitulo}>
      <div className="cols gcols">
        <Card className={`gcard${fixos ? '' : ' eventuais'}${temParcelado ? '' : ' semparc'}`}>
          <h2>Seus gastos <small>{lancamentos.length === 1 ? '1 item' : `${lancamentos.length} itens`}</small></h2>

          {lancamentos.length === 0 ? (
            <p className="empty">Nenhum gasto nesta lista. Use “{p.rotuloNovo}” para cadastrar.</p>
          ) : (
            <>
              <div className="grow ghead" aria-hidden="true">
                <span>Descrição</span><span>Categoria</span>
                {fixos && <span>Tipo</span>}
                <span>Mês</span>
                {fixos && (temParcelado ? <span>Parcelas</span> : <span />)}
                <span className="r">Valor</span><span />
              </div>
              <div className="glist">
                {lancamentos.map((g) => {
                  const id = g.id as number
                  return (
                    <div className="grow" key={id}>
                      <input type="text" className="desc" value={g.descricao} aria-label="Descrição" onChange={(e) => p.onEditar(id, { descricao: e.target.value })} />
                      <select value={g.categoria} aria-label="Categoria" onChange={(e) => p.onEditar(id, { categoria: e.target.value })}>
                        {categorias.map((c) => <option key={c.nome}>{c.nome}</option>)}
                      </select>
                      {fixos && (
                        <select value={g.tipo} aria-label="Tipo" onChange={(e) => p.onEditar(id, { tipo: e.target.value as TipoLancamento })}>
                          {Object.entries(TIPOS_FIXOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                        </select>
                      )}
                      <input type="month" value={g.mesInicial} aria-label="Mês" onChange={(e) => e.target.value && p.onEditar(id, { mesInicial: e.target.value })} />
                      {fixos && (g.tipo === 'PARCELADO'
                        ? <NumberInput stepper min={1} max={60} value={g.parcelas} aria-label="Parcelas" onChange={(v) => p.onEditar(id, { parcelas: v })} />
                        : <span />)}
                      <NumberInput prefix="R$" min={0} step={10} value={g.valor} aria-label="Valor" onChange={(v) => p.onEditar(id, { valor: v })} />
                      <button className="btn x ghost" onClick={() => p.onRemover(id)} aria-label={`Remover ${g.descricao}`} title="Remover"><Icon name="close" size={14} /></button>
                    </div>
                  )
                })}
              </div>
            </>
          )}

          <div className="row-actions">
            <button className="btn pri" onClick={p.onAdicionar}>{p.rotuloNovo}</button>
            <span className="dim">Média mensal: <b className="num">{fmt(resumo.mediaMensalTotal)}</b></span>
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
