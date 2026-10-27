import { useState } from 'react'
import { FormCartao } from '../components/FormCartao'
import { Card } from '../components/ui'
import { fmt } from '../domain/format'
import type { Cartao, CartaoEntrada } from '../domain/types'

/**
 * Card "Cartões de crédito" da aba Gastos fixos: fatura do mês atual de cada cartão, atalho para as compras e
 * cadastro rápido. Os valores vêm do back end; a fatura já está somada nos gastos fixos.
 */
export function CartoesCard({ cartoes, onCadastrar, formAberto }: {
  cartoes: Cartao[]
  onCadastrar: (c: CartaoEntrada) => Promise<boolean> | void
  /** só para testes: começa com o formulário aberto */
  formAberto?: boolean
}) {
  const [aberto, setAberto] = useState(!!formAberto)

  return (
    <Card className="cartoes-card">
      <h2>Cartões de crédito <small>{cartoes.length === 1 ? '1 cartão' : `${cartoes.length} cartões`}</small></h2>
      <p className="dim cartoes-nota">A fatura do cartão entra nos gastos fixos: ela já está somada na média e nas categorias desta aba.</p>

      {cartoes.length === 0 && <p className="empty">Nenhum cartão cadastrado.</p>}
      {cartoes.map((c) => (
        <div className="cartao-linha" key={c.id}>
          <div>
            <b>{c.nome}</b>
            <small className="dim">fecha dia {c.diaFechamento} · vence dia {c.diaVencimento}</small>
          </div>
          <span className="num" title="Fatura atual (aberta, ainda recebendo compras)">{fmt(c.faturaAtual)}</span>
          <a className="btn sm" href={`#/cartao?id=${c.id}`}>Ver compras</a>
        </div>
      ))}

      {aberto ? (
        <FormCartao rotuloSalvar="Cadastrar" onCancelar={() => setAberto(false)}
          onSalvar={async (e) => { if ((await onCadastrar(e)) !== false) setAberto(false) }} />
      ) : (
        <div className="row-actions">
          <button className="btn pri" onClick={() => setAberto(true)}>+ Cadastrar cartão</button>
        </div>
      )}
    </Card>
  )
}
