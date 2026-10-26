import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/api'
import { FormCartao } from '../components/FormCartao'
import { Icon } from '../components/Icon'
import { Card, NumberInput, Page } from '../components/ui'
import { fmt, rotuloCompetenciaLongo } from '../domain/format'
import type {
  Cartao as CartaoT, CartaoEntrada, Categoria, CompraCartao, CompraEntrada, Fatura, TipoCompra,
} from '../domain/types'
import { useApp } from '../store/AppContext'

const TIPOS: Record<TipoCompra, string> = { A_VISTA: 'À vista', PARCELADA: 'Parcelada', ASSINATURA: 'Assinatura' }

/** Texto de apoio da compra, só com valores calculados pelo back end. */
function resumoCompra(c: CompraCartao): string {
  const primeira = rotuloCompetenciaLongo(c.primeiraFatura)
  if (c.tipo === 'ASSINATURA') return `${fmt(c.valorParcela)} por mês a partir da fatura de ${primeira}`
  if (c.tipo === 'A_VISTA') return `Entra na fatura de ${primeira}`
  const ultima = c.ultimaFatura ? rotuloCompetenciaLongo(c.ultimaFatura) : primeira
  return `${c.parcelas}x de ${fmt(c.valorParcela)} · faturas de ${primeira} a ${ultima} · restam ${c.parcelasRestantes ?? 0}`
}

export interface CartaoViewProps {
  cartoes: CartaoT[]
  selecionado: CartaoT | null
  categorias: Categoria[]
  compras: CompraCartao[]
  faturas: Fatura[]
  onSelecionar: (id: number) => void
  onSalvarCartao: (id: number | null, c: CartaoEntrada) => Promise<boolean> | void
  onRemoverCartao: (id: number) => void
  onNovaCompra: () => void
  onEditarCompra: (id: number, patch: Partial<CompraEntrada>) => void
  onRemoverCompra: (id: number) => void
  /** só para testes: abre o formulário de novo cartão / a fatura do mês indicado */
  formInicial?: 'novo' | 'editar'
  faturaAberta?: string
}

/** Tela do cartão (apenas exibição): seletor, resumo, compras editáveis e faturas dos próximos 12 meses. */
export function CartaoView(p: CartaoViewProps) {
  const [form, setForm] = useState<'novo' | 'editar' | null>(p.formInicial ?? null)
  const [aberta, setAberta] = useState<string | null>(p.faturaAberta ?? null)
  const sel = p.selecionado

  const salvar = async (id: number | null, c: CartaoEntrada) => {
    if ((await p.onSalvarCartao(id, c)) !== false) setForm(null)
  }

  return (
    <Page title="Cartão de crédito"
      subtitle="Cadastre seus cartões e todas as compras: a fatura de cada mês entra nos gastos fixos e na projeção.">
      <Card className="cartao-barra">
        {p.cartoes.length > 1 && sel && (
          <select aria-label="Cartão" value={sel.id} onChange={(e) => p.onSelecionar(Number(e.target.value))}>
            {p.cartoes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select>
        )}
        {p.cartoes.length === 1 && sel && <b className="cartao-nome">{sel.nome}</b>}
        {sel && (
          <span className="dim">fecha dia {sel.diaFechamento} · vence dia {sel.diaVencimento}
            {sel.limite != null && <> · limite {fmt(sel.limite)}</>}</span>
        )}
        <span className="cartao-barra-acoes">
          {sel && <button className="btn sm" onClick={() => setForm(form === 'editar' ? null : 'editar')}>Editar cartão</button>}
          {sel && (
            <button className="btn sm x" onClick={() => p.onRemoverCartao(sel.id)} aria-label={`Remover o cartão ${sel.nome}`}>Remover cartão</button>
          )}
          <button className="btn sm" onClick={() => setForm(form === 'novo' ? null : 'novo')}>+ Novo cartão</button>
        </span>
        {form === 'novo' && (
          <FormCartao rotuloSalvar="Cadastrar" onCancelar={() => setForm(null)} onSalvar={(c) => void salvar(null, c)} />
        )}
        {form === 'editar' && sel && (
          <FormCartao inicial={sel} rotuloSalvar="Salvar" onCancelar={() => setForm(null)} onSalvar={(c) => void salvar(sel.id, c)} />
        )}
      </Card>

      {!sel ? (
        <Card><p className="empty">Nenhum cartão cadastrado. Use “+ Novo cartão” para começar.</p></Card>
      ) : (
        <>
          <div className="cartao-resumo">
            <Card><small className="dim">Fatura atual</small><b className="num">{fmt(sel.faturaAtual)}</b></Card>
            <Card><small className="dim">Próxima fatura</small><b className="num">{fmt(sel.proximaFatura)}</b></Card>
            <Card>
              <small className="dim">Limite disponível</small>
              {sel.limiteDisponivel != null
                ? <b className={`num${sel.limiteDisponivel < 0 ? ' neg' : ''}`}>{fmt(sel.limiteDisponivel)}</b>
                : <b className="dim">Sem limite informado</b>}
            </Card>
          </div>

          <Card className="gcard ccompras">
            <h2>Compras <small>{p.compras.length === 1 ? '1 compra' : `${p.compras.length} compras`}</small></h2>
            {p.compras.length === 0 ? (
              <p className="empty">Nenhuma compra neste cartão. Use “+ Nova compra” para cadastrar.</p>
            ) : (
              <>
                <div className="grow crow ghead" aria-hidden="true">
                  <span>Descrição</span><span>Categoria</span><span>Tipo</span><span>Data da compra</span>
                  <span>Parcelas</span><span className="r">Valor</span><span />
                </div>
                <div className="glist">
                  {p.compras.map((c) => (
                    <div className="cwrap" key={c.id}>
                      <div className="grow crow">
                        <input type="text" className="desc" value={c.descricao} aria-label="Descrição"
                          onChange={(e) => p.onEditarCompra(c.id, { descricao: e.target.value })} />
                        <select value={c.categoria} aria-label="Categoria" onChange={(e) => p.onEditarCompra(c.id, { categoria: e.target.value })}>
                          {p.categorias.some((k) => k.nome === c.categoria) ? null : <option>{c.categoria}</option>}
                          {p.categorias.map((k) => <option key={k.nome}>{k.nome}</option>)}
                        </select>
                        <select value={c.tipo} aria-label="Tipo" onChange={(e) => p.onEditarCompra(c.id, { tipo: e.target.value as TipoCompra })}>
                          {Object.entries(TIPOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                        </select>
                        <input type="date" value={c.dataCompra} aria-label="Data da compra"
                          onChange={(e) => e.target.value && p.onEditarCompra(c.id, { dataCompra: e.target.value })} />
                        {c.tipo === 'PARCELADA'
                          ? <NumberInput stepper min={2} max={72} value={c.parcelas} aria-label="Parcelas" onChange={(v) => p.onEditarCompra(c.id, { parcelas: v })} />
                          : <span />}
                        <NumberInput prefix="R$" min={0} step={10} value={c.valor}
                          aria-label={c.tipo === 'ASSINATURA' ? 'Valor mensal' : 'Valor total'}
                          onChange={(v) => p.onEditarCompra(c.id, { valor: v })} />
                        <button className="btn x ghost" onClick={() => p.onRemoverCompra(c.id)} aria-label={`Remover ${c.descricao}`} title="Remover">
                          <Icon name="close" size={14} />
                        </button>
                      </div>
                      <div className="cinfo dim">
                        {c.tipo === 'ASSINATURA' ? 'Valor mensal. ' : 'Valor total da compra. '}{resumoCompra(c)}
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
            <div className="row-actions">
              <button className="btn pri" onClick={p.onNovaCompra}>+ Nova compra</button>
            </div>
          </Card>

          <Card className="cfaturas">
            <h2>Faturas dos próximos 12 meses <small>por mês de vencimento</small></h2>
            {p.faturas.map((f) => {
              const aberto = aberta === f.mes
              return (
                <div className="fat" key={f.mes}>
                  <button className="fat-head" aria-expanded={aberto} onClick={() => setAberta(aberto ? null : f.mes)}>
                    <span>{rotuloCompetenciaLongo(f.mes)}</span>
                    <span className="num">{fmt(f.total)}</span>
                  </button>
                  {aberto && (
                    <ul className="fat-itens">
                      {f.itens.length === 0 && <li className="dim">Sem lançamentos nesta fatura.</li>}
                      {f.itens.map((i) => (
                        <li key={`${i.compraId}`}>
                          <span>{i.descricao}{i.parcela && <small className="dim"> {i.parcela}</small>} <small className="dim">· {i.categoria}</small></span>
                          <span className="num">{fmt(i.valor)}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )
            })}
          </Card>
        </>
      )}
    </Page>
  )
}

const ATRASO_MS = 450
const hoje = () => {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
const entradaDe = (c: CompraCartao): CompraEntrada => ({
  descricao: c.descricao, categoria: c.categoria, tipo: c.tipo, dataCompra: c.dataCompra, valor: c.valor, parcelas: c.parcelas,
})

/** Aba "Cartão de crédito": carrega compras e faturas do cartão escolhido e grava as edições (com atraso). */
export function Cartao() {
  const { cartoes, categorias, alterarCartoes } = useApp()
  const [params, setParams] = useSearchParams()
  const idParam = Number(params.get('id'))
  const selecionado = cartoes.find((c) => c.id === idParam) ?? cartoes[0] ?? null
  const selId = selecionado?.id ?? null

  const [compras, setCompras] = useState<CompraCartao[]>([])
  const [faturas, setFaturas] = useState<Fatura[]>([])
  const comprasRef = useRef<CompraCartao[]>([])
  comprasRef.current = compras
  const timers = useRef(new Map<number, ReturnType<typeof setTimeout>>())
  const seq = useRef(0)

  /** Recarrega compras e faturas; compras em edição mantêm o que o usuário digitou (só recebem os calculados). */
  const carregar = useCallback(async (id: number) => {
    const minha = ++seq.current
    const [cs, fs] = await Promise.all([api.comprasCartao(id), api.faturasCartao(id)])
    if (minha !== seq.current) return
    setCompras((atual) => cs.map((s) => {
      const local = atual.find((x) => x.id === s.id)
      if (!local || !timers.current.has(s.id)) return s
      return { ...local, valorParcela: s.valorParcela, primeiraFatura: s.primeiraFatura, ultimaFatura: s.ultimaFatura, parcelasRestantes: s.parcelasRestantes }
    }))
    setFaturas(fs)
  }, [])

  // recarrega ao trocar de cartão e sempre que o contexto recalcula (cartoes muda de identidade)
  useEffect(() => {
    if (selId == null) { setCompras([]); setFaturas([]); return }
    void carregar(selId).catch(() => { /* o aviso de conexão vem das demais chamadas */ })
  }, [selId, cartoes, carregar])

  useEffect(() => () => timers.current.forEach(clearTimeout), [])

  const salvarCartao = async (id: number | null, c: CartaoEntrada) => {
    let criado: CartaoT | undefined
    const ok = await alterarCartoes(async () => {
      if (id == null) criado = await api.criarCartao(c)
      else await api.salvarCartao(id, c)
    })
    if (ok && criado) setParams({ id: String(criado.id) })
    return ok
  }

  const removerCartao = (id: number) => {
    const nome = cartoes.find((c) => c.id === id)?.nome ?? ''
    if (!window.confirm(`Remover o cartão ${nome} e todas as suas compras? A fatura deixa de contar nos gastos.`)) return
    void alterarCartoes(() => api.removerCartao(id)).then((ok) => { if (ok) setParams({}) })
  }

  const novaCompra = () => {
    if (selId == null) return
    void alterarCartoes(() => api.criarCompra(selId, {
      descricao: 'Nova compra', categoria: categorias[0]?.nome ?? 'Outros', tipo: 'A_VISTA',
      dataCompra: hoje(), valor: 1, parcelas: 0,
    }))
  }

  const editarCompra = (id: number, patch: Partial<CompraEntrada>) => {
    // ao virar parcelada, começa no mínimo aceito (2 parcelas)
    if (patch.tipo === 'PARCELADA' && (comprasRef.current.find((c) => c.id === id)?.parcelas ?? 0) < 2) patch = { ...patch, parcelas: 2 }
    setCompras((cs) => cs.map((c) => (c.id === id ? { ...c, ...patch } : c)))
    const antigo = timers.current.get(id)
    if (antigo) clearTimeout(antigo)
    timers.current.set(id, setTimeout(() => {
      timers.current.delete(id)
      const atual = comprasRef.current.find((c) => c.id === id)
      if (atual) void alterarCartoes(() => api.salvarCompra(id, entradaDe(atual)))
    }, ATRASO_MS))
  }

  const removerCompra = (id: number) => {
    const t = timers.current.get(id)
    if (t) clearTimeout(t)
    timers.current.delete(id)
    void alterarCartoes(() => api.removerCompra(id))
  }

  return (
    <CartaoView cartoes={cartoes} selecionado={selecionado} categorias={categorias} compras={compras} faturas={faturas}
      onSelecionar={(id) => setParams({ id: String(id) })} onSalvarCartao={salvarCartao} onRemoverCartao={removerCartao}
      onNovaCompra={novaCompra} onEditarCompra={editarCompra} onRemoverCompra={removerCompra} />
  )
}
