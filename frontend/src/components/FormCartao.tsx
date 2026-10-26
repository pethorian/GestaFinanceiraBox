import { useState } from 'react'
import type { Cartao, CartaoEntrada } from '../domain/types'

/**
 * Formulário inline de cartão (nome, dia de fechamento, dia de vencimento e limite opcional). Só coleta os
 * campos: validação e regras ficam no back end, que responde 400 com a mensagem exibida no aviso da tela.
 */
export function FormCartao({ inicial, rotuloSalvar, onSalvar, onCancelar }: {
  inicial?: Cartao
  rotuloSalvar: string
  onSalvar: (c: CartaoEntrada) => void
  onCancelar: () => void
}) {
  const [nome, setNome] = useState(inicial?.nome ?? '')
  const [fechamento, setFechamento] = useState(String(inicial?.diaFechamento ?? ''))
  const [vencimento, setVencimento] = useState(String(inicial?.diaVencimento ?? ''))
  const [limite, setLimite] = useState(inicial?.limite != null ? String(inicial.limite) : '')

  const enviar = (e: React.FormEvent) => {
    e.preventDefault()
    onSalvar({
      nome,
      diaFechamento: Number(fechamento),
      diaVencimento: Number(vencimento),
      limite: limite.trim() === '' ? null : Number(limite),
    })
  }

  return (
    <form className="cform" onSubmit={enviar} aria-label="Dados do cartão">
      <label className="f"><span>Nome do cartão</span>
        <input type="text" value={nome} maxLength={60} required placeholder="Ex.: Nubank" onChange={(e) => setNome(e.target.value)} />
      </label>
      <label className="f"><span>Dia de fechamento</span>
        <input type="number" min={1} max={31} required value={fechamento} onChange={(e) => setFechamento(e.target.value)} />
      </label>
      <label className="f"><span>Dia de vencimento</span>
        <input type="number" min={1} max={31} required value={vencimento} onChange={(e) => setVencimento(e.target.value)} />
      </label>
      <label className="f"><span>Limite (opcional)</span>
        <input type="number" min={0} step="0.01" value={limite} placeholder="R$" onChange={(e) => setLimite(e.target.value)} />
      </label>
      <div className="cform-acoes">
        <button type="submit" className="btn pri">{rotuloSalvar}</button>
        <button type="button" className="btn" onClick={onCancelar}>Cancelar</button>
      </div>
    </form>
  )
}
