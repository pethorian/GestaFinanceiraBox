import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import type { PlrControle } from '../domain/types'
import { ControlePlrView, SUGESTAO_80_20 } from './Plr'

const acoes = {
  onCriarRecebimento: () => {}, onSalvarRecebimento: () => {}, onRemoverRecebimento: () => {},
  onCriarGasto: () => {}, onSalvarGasto: () => {}, onRemoverGasto: () => {}, onUsarSugestao: () => {},
}

// resposta de GET /api/plr/controle (os números vêm prontos do back end)
const controle: PlrControle = {
  recebido: 10000,
  gasto: 350,
  saldo: 9650,
  destinos: [
    { id: 1, nome: 'Gastos', tipo: 'OBRIGATORIO', percentual: 80, alocado: 8000, gasto: 0, saldo: 8000, percentualUsado: 0 },
    { id: 2, nome: 'Pessoal', tipo: 'LAZER', percentual: 20, alocado: 2000, gasto: 350, saldo: 1650, percentualUsado: 17.5 },
  ],
  semDestino: { alocado: 0, gasto: 0, saldo: 0, percentualUsado: 0 },
  recebimentos: [{ id: 10, data: '2026-10-20', descricao: 'PLR 2026', valor: 10000 }],
  gastos: [{ id: 20, data: '2026-10-22', descricao: 'Tênis', valor: 350, destinoId: 2, destinoNome: 'Pessoal' }],
}

const render = (c: PlrControle) => renderToStaticMarkup(<ControlePlrView controle={c} {...acoes} />)

describe('Controle do PLR recebido', () => {
  const html = render(controle)
  it('explica que não altera a projeção', () => {
    expect(html).toContain('não altera a projeção')
  })
  it('mostra recebido, gasto e saldo', () => {
    expect(html).toMatch(/Recebido<\/small><strong class="num">R\$\s10\.000,00/)
    expect(html).toMatch(/Gasto<\/small><strong class="num">R\$\s350,00/)
    expect(html).toMatch(/Saldo<\/small><strong class="num pos">R\$\s9\.650,00/)
  })
  it('mostra cada envelope com alocado, saldo e % usado', () => {
    expect(html).toContain('Pessoal')
    expect(html).toMatch(/R\$\s2\.000,00/)
    expect(html).toMatch(/R\$\s1\.650,00/)
    expect(html).toContain('17,5% usado')
    expect(html).toMatch(/R\$\s8\.000,00/)
    expect(html).not.toContain('Estourou')
    expect(html).not.toContain('Usar sugestão 80/20')
  })
  it('lista recebimentos e gastos com o destino', () => {
    expect(html).toContain('value="PLR 2026"')
    expect(html).toContain('value="Tênis"')
    expect(html).toContain('<option value="2" selected="">Pessoal</option>')
  })
})

describe('Controle do PLR: estouro e sem destinos', () => {
  it('destaca o envelope estourado', () => {
    const html = render({
      ...controle,
      destinos: [{ ...controle.destinos[1], gasto: 2500, saldo: -500, percentualUsado: 125 }],
    })
    expect(html).toContain('plr-env estourou')
    expect(html).toContain('Estourou')
    expect(html).toContain('125% usado')
    expect(html).toMatch(/num neg">[^<]*500,00/)
  })
  it('sem destinos oferece a sugestão 80/20 e mostra "Sem destino"', () => {
    const html = render({
      ...controle, destinos: [], semDestino: { alocado: 10000, gasto: 350, saldo: 9650, percentualUsado: 3.5 },
    })
    expect(html).toContain('Usar sugestão 80/20')
    expect(html).toContain('Sem destino')
    expect(html).toContain('3,5% usado')
  })
  it('a sugestão cria Gastos 80% (obrigatório) e Pessoal 20% (lazer)', () => {
    expect(SUGESTAO_80_20).toEqual([
      { nome: 'Gastos', tipo: 'OBRIGATORIO', percentual: 80, metaId: null },
      { nome: 'Pessoal', tipo: 'LAZER', percentual: 20, metaId: null },
    ])
  })
  it('sem nada registrado mostra os estados vazios', () => {
    const html = render({ ...controle, recebido: 0, gasto: 0, saldo: 0, recebimentos: [], gastos: [] })
    expect(html).toContain('Nenhum recebimento')
    expect(html).toContain('Nenhum gasto registrado')
  })
})
