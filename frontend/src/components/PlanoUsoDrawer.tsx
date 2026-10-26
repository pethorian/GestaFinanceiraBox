import { useEffect, useState } from 'react'
import { NOME_EVENTO } from '../domain/adicionais'
import type { ChaveAdicional, PlanoAdicional } from '../domain/types'
import { useApp } from '../store/AppContext'
import { PlanoUsoEditor, type RascunhoDestino } from './adicionais'
import { Icon } from './Icon'

let seqChave = 0
const novaChave = () => `n${++seqChave}`

/** O id acompanha o destino para que o servidor atualize (e não recrie) o que já existe: gastos do PLR dependem disso. */
const paraRascunho = (p: PlanoAdicional): RascunhoDestino[] =>
  p.destinos.map(({ id, nome, tipo, percentual, metaId }) => ({ chave: novaChave(), id, nome, tipo, percentual, metaId }))

/**
 * Painel lateral com o editor do plano de uso de um evento. O rascunho (o que se digita) fica aqui;
 * cada alteração grava a lista inteira com atraso, e a divisão/prévia exibidas são as que o servidor devolveu.
 */
export function PlanoUsoDrawer({ evento, onClose }: { evento: ChaveAdicional | null; onClose: () => void }) {
  const { dados, salvarDestinos, errosAdicionais } = useApp()
  const plano = evento ? dados.adicionais[evento] : null
  const [rascunho, setRascunho] = useState<RascunhoDestino[]>([])

  // ao abrir, começa do que está salvo
  useEffect(() => {
    if (evento) setRascunho(paraRascunho(dados.adicionais[evento]))
  }, [evento])
  useEffect(() => {
    if (!evento) return
    const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', h)
    return () => document.removeEventListener('keydown', h)
  }, [evento, onClose])

  // destinos recém-criados ganham id ao serem gravados (o servidor mantém a ordem): guarda-o no rascunho
  const salvos = plano?.destinos
  useEffect(() => {
    if (!salvos) return
    setRascunho((r) => {
      if (r.length !== salvos.length || r.every((d) => d.id !== undefined)) return r
      return r.map((d, i) => (d.id === undefined ? { ...d, id: salvos[i].id } : d))
    })
  }, [salvos])

  const mudar = (lista: RascunhoDestino[]) => {
    setRascunho(lista)
    if (evento) salvarDestinos(evento, lista)
  }
  // a linha nova só é gravada quando o usuário a preenche (nome vazio seria recusado pelo servidor)
  const adicionar = () => setRascunho((l) => [...l, {
    chave: novaChave(), nome: '', tipo: 'LAZER', percentual: plano && plano.semDestinoPercentual > 0 ? plano.semDestinoPercentual : 0, metaId: null,
  }])

  return (
    <aside id="drawer" className={plano ? 'open' : ''} aria-hidden={!plano} aria-label="Plano de uso">
      {plano && evento && (
        <>
          <button className="btn x" style={{ float: 'right' }} onClick={onClose} aria-label="Fechar"><Icon name="close" size={16} /></button>
          <h2 style={{ fontSize: 18, margin: 0 }}>Plano de uso do {NOME_EVENTO[plano.evento]}</h2>
          <small style={{ color: 'var(--mut)', display: 'block', marginBottom: 12 }}>
            {evento === 'plr'
              ? 'Cada destino é um envelope do controle do PLR recebido. O que ficar sem destino continua no saldo disponível.'
              : 'Divida o líquido de cada parcela. O que ficar sem destino continua no saldo disponível.'}
          </small>
          <PlanoUsoEditor
            plano={plano} rascunho={rascunho} metas={dados.metas} erros={errosAdicionais[evento]}
            onChange={mudar} onAdicionar={adicionar}
          />
        </>
      )}
    </aside>
  )
}
