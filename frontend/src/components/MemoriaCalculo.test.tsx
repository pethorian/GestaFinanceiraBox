import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import type { MesProjecao } from '../domain/types'
import { MemoriaCalculo } from './MemoriaCalculo'

const mesBase = {
  indice: 0,
  competencia: '2026-10',
  rotulo: 'Out/26',
  base: 5000,
  sobreaviso: 0,
  creche: 0,
  homeOffice: 0,
  vrVa: 0,
  decimo: 0,
  ferias: 0,
  inss: 500,
  irpf: { deducaoLegal: 0, deducaoMinima: 0, deducao: 0, baseCalculo: 0, aliquota: 0, bruto: 0, reducao: 0, imposto: 0 },
  funcef: 0,
  funcefDecimo: 0,
  previdencia: 0,
  patrimonioTotal: 1500,
  saude: 0,
  valeTransporte: 0,
  sindicato: 0,
  descontoDecimo: 0,
  receitas: 5000,
  descontos: 500,
  liquido: 4500,
  gastos: 3000,
  sobra: 1500,
  aportes: 0,
  acumulado: 1500,
  guardado: 0,
  patrimonio: 1500,
  metas: [],
  plr: 0,
  bonus: 0,
  evento: '',
  baseInss: 5000,
  tetoInssAplicado: false,
  adicionais: [],
  gastosAdicionais: 0,
  reservasAdicionais: 0,
  guardadoReservaAdicionais: 0,
} as MesProjecao

const AVISO = 'Não conte com esse dinheiro'

describe('MemoriaCalculo: aviso de PLR', () => {
  it('mostra o aviso quando o mês tem parcela de PLR', () => {
    const html = renderToStaticMarkup(
      <MemoriaCalculo mes={{ ...mesBase, plr: 2000, evento: 'PLR' }} onClose={() => {}} />,
    )
    expect(html).toContain(AVISO)
    expect(html).toContain('class="warn"')
  })

  it('não mostra o aviso quando o mês não tem PLR', () => {
    const html = renderToStaticMarkup(<MemoriaCalculo mes={mesBase} onClose={() => {}} />)
    expect(html).not.toContain(AVISO)
  })
})

describe('MemoriaCalculo: aportes em metas', () => {
  const comMeta = {
    ...mesBase,
    aportes: 900,
    sobra: 600,
    guardado: 4100,
    patrimonio: 5700,
    metas: [{ id: 1, nome: 'Reserva de emergência', aporte: 900, guardado: 4100, aporteAdicional: 0 }],
  }

  it('mostra o aporte como saída e o guardado/patrimônio', () => {
    const html = renderToStaticMarkup(<MemoriaCalculo mes={comMeta} onClose={() => {}} />)
    expect(html).toContain('Aportes em metas')
    expect(html).toContain('Reserva de emergência')
    expect(html).toContain('Guardado em metas')
    expect(html).toContain('Patrimônio')
  })

  it('sem metas não mostra linhas de aporte', () => {
    const html = renderToStaticMarkup(<MemoriaCalculo mes={mesBase} onClose={() => {}} />)
    expect(html).not.toContain('Aportes em metas')
    expect(html).not.toContain('Guardado em metas')
  })
})

describe('MemoriaCalculo: FUNCEF guardada', () => {
  it('mostra a previdência como guardada, não como gasto', () => {
    const html = renderToStaticMarkup(
      <MemoriaCalculo mes={{ ...mesBase, funcef: 300, previdencia: 10300, patrimonioTotal: 11800 }} onClose={() => {}} />,
    )
    expect(html).toContain('Guardada (previdência), não é gasto')
    expect(html).toContain('Saldo na FUNCEF ao fim do mês')
  })

  it('sem previdência não mostra o bloco', () => {
    const html = renderToStaticMarkup(<MemoriaCalculo mes={mesBase} onClose={() => {}} />)
    expect(html).not.toContain('Guardada (previdência)')
  })
})

describe('MemoriaCalculo: plano de uso do 13º/PLR', () => {
  const comPlr: MesProjecao = {
    ...mesBase,
    plr: 3000,
    evento: 'PLR',
    gastosAdicionais: 1500,
    reservasAdicionais: 1500,
    sobra: 1500,
    guardado: 7600,
    guardadoReservaAdicionais: 600,
    metas: [{ id: 7, nome: 'Viagem Europa', aporte: 1000, guardado: 7000, aporteAdicional: 900 }],
    adicionais: [{
      evento: 'PLR',
      liquido: 3000,
      semDestino: 0,
      destinos: [
        { id: 1, nome: 'Viagem', tipo: 'RESERVA', metaId: 7, percentual: 30, valor: 900 },
        { id: 2, nome: 'Lazer', tipo: 'LAZER', metaId: null, percentual: 50, valor: 1500 },
        { id: 3, nome: 'Reserva', tipo: 'RESERVA', metaId: null, percentual: 20, valor: 600 },
      ],
    }],
  }

  it('mostra o bloco do adicional com valor e % de cada destino', () => {
    const html = renderToStaticMarkup(<MemoriaCalculo mes={comPlr} onClose={() => {}} />)
    expect(html).toContain('Do seu PLR deste mês')
    expect(html).toMatch(/Do seu PLR deste mês \(R\$\s3\.000,00 líquido\)/)
    expect(html).toContain('Viagem')
    expect(html).toContain('30%')
    expect(html).toContain('→ Viagem Europa') // destino ligado à meta
    expect(html).toMatch(/R\$\s1\.500,00/)
  })

  it('mostra as saídas dos adicionais no resultado e o extra na meta', () => {
    const html = renderToStaticMarkup(<MemoriaCalculo mes={comPlr} onClose={() => {}} />)
    expect(html).toContain('Gastos com adicionais')
    expect(html).toContain('Reservas dos adicionais')
    expect(html).toMatch(/do 13º\/PLR/)
    expect(html).toContain('Reserva dos adicionais (sem meta)')
  })

  it('mostra o que ficou sem destino e o caso sem plano', () => {
    const parcial = { ...comPlr, adicionais: [{ ...comPlr.adicionais[0], semDestino: 1200, destinos: comPlr.adicionais[0].destinos.slice(0, 2) }] }
    expect(renderToStaticMarkup(<MemoriaCalculo mes={parcial} onClose={() => {}} />)).toContain('Sem destino')
    const semPlano = { ...comPlr, adicionais: [{ evento: 'DECIMO' as const, liquido: 2000, semDestino: 2000, destinos: [] }] }
    const html = renderToStaticMarkup(<MemoriaCalculo mes={semPlano} onClose={() => {}} />)
    expect(html).toContain('Do seu 13º deste mês')
    expect(html).toContain('Sem plano de uso')
  })

  it('sem adicional no mês não mostra o bloco nem as saídas', () => {
    const html = renderToStaticMarkup(<MemoriaCalculo mes={mesBase} onClose={() => {}} />)
    expect(html).not.toContain('deste mês')
    expect(html).not.toContain('Gastos com adicionais')
  })
})
