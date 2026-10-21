import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import type { PlanoAdicional } from '../domain/types'
import { PlanoUsoEditor, PlanoUsoResumo, type RascunhoDestino } from './adicionais'

const vazio: PlanoAdicional = {
  evento: 'PLR', ativo: true, destinos: [], totalPercentual: 0, semDestinoPercentual: 100,
  previa: [{ mes: '2027-03', liquido: 4000, destinos: [], semDestino: 4000 }],
}

const comDestinos: PlanoAdicional = {
  evento: 'PLR',
  ativo: true,
  destinos: [
    { id: 1, nome: 'Viagem', tipo: 'RESERVA', percentual: 30, metaId: 7, metaNome: 'Europa' },
    { id: 2, nome: 'Lazer', tipo: 'LAZER', percentual: 50, metaId: null, metaNome: null },
  ],
  totalPercentual: 80,
  semDestinoPercentual: 20,
  previa: [
    {
      mes: '2027-03', liquido: 4000, semDestino: 800,
      destinos: [
        { id: 1, nome: 'Viagem', tipo: 'RESERVA', metaId: 7, percentual: 30, valor: 1200 },
        { id: 2, nome: 'Lazer', tipo: 'LAZER', metaId: null, percentual: 50, valor: 2000 },
      ],
    },
    { mes: '2027-09', liquido: 5000, semDestino: 1000, destinos: [] },
  ],
}

const rascunho: RascunhoDestino[] = comDestinos.destinos.map(({ nome, tipo, percentual, metaId }, i) => ({ chave: `k${i}`, nome, tipo, percentual, metaId }))
const metas = [{ id: 7, nome: 'Europa' }, { id: 8, nome: 'Carro' }]

const editor = (plano: PlanoAdicional, r: RascunhoDestino[], erros: string[] | null = null) =>
  renderToStaticMarkup(<PlanoUsoEditor plano={plano} rascunho={r} metas={metas} erros={erros} onChange={() => {}} onAdicionar={() => {}} />)

describe('Plano de uso: editor', () => {
  it('estado vazio: explica e oferece "+ Destino"', () => {
    const html = editor(vazio, [])
    expect(html).toContain('Nenhum destino ainda')
    expect(html).toContain('Destino</button>')
    expect(html).not.toContain('role="listitem"')
    expect(html).toContain('Próxima parcela · Março 2027')
  })

  it('com destinos: linhas editáveis, tipo padronizado e meta só na reserva', () => {
    const html = editor(comDestinos, rascunho)
    expect(html.match(/role="listitem"/g)).toHaveLength(2)
    expect(html).toContain('value="Viagem"')
    expect(html).toContain('placeholder="Viagem"')
    for (const t of ['Lazer', 'Obrigatório', 'Reserva']) expect(html).toContain(`>${t}</option>`)
    // só a linha RESERVA tem o seletor de meta, com as metas existentes
    expect(html.match(/aria-label="Meta do destino/g)).toHaveLength(1)
    expect(html).toContain('Meta: Europa')
    expect(html).toContain('Meta: Carro')
    expect(html).toContain('Remover o destino 2')
  })

  it('mostra a divisão salva (com o sem destino) e os valores da próxima parcela', () => {
    const html = editor(comDestinos, rascunho)
    expect(html).toContain('80% com destino')
    expect(html).toContain('Sem destino')
    expect(html).toMatch(/R\$\s1\.200,00/)
    expect(html).toMatch(/R\$\s800,00/)
    expect(html).toContain('Set/27')
  })

  it('mostra os erros do servidor de forma amigável', () => {
    const html = editor(comDestinos, rascunho, ['A soma dos percentuais dos destinos não pode passar de 100% (atual: 120%).'])
    expect(html).toContain('role="alert"')
    expect(html).toContain('Não foi possível salvar o plano.')
    expect(html).toContain('atual: 120%')
  })
})

describe('Plano de uso: resumo no card', () => {
  it('sem destinos convida a definir', () => {
    const html = renderToStaticMarkup(<PlanoUsoResumo plano={vazio} onEditar={() => {}} />)
    expect(html).toContain('Definir')
    expect(html).toContain('fica no saldo disponível')
  })
  it('com destinos mostra a barra e a legenda', () => {
    const html = renderToStaticMarkup(<PlanoUsoResumo plano={comDestinos} onEditar={() => {}} />)
    expect(html).toContain('Editar')
    expect(html).toContain('ad-bar')
    expect(html).toContain('Viagem')
    expect(html).toContain('20%')
  })
})
