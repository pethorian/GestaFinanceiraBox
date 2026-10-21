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
  saude: 0,
  valeTransporte: 0,
  sindicato: 0,
  descontoDecimo: 0,
  receitas: 5000,
  descontos: 500,
  liquido: 4500,
  gastos: 3000,
  sobra: 1500,
  acumulado: 1500,
  plr: 0,
  bonus: 0,
  evento: '',
  baseInss: 5000,
  tetoInssAplicado: false,
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
