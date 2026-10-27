import { describe, expect, it } from 'vitest'
import type { Meta, Plr } from '../domain/types'
import { comCalculosMetas, comCalculosPlr } from './AppContext'

const meta = (id: number, patch: Partial<Meta> = {}): Meta => ({
  id, nome: `Meta ${id}`, valorAlvo: 1000, valorAtual: 0, aporteMensal: 100, banco: null, falta: 1000, percentual: 0,
  mesesParaAtingir: 10, previsao: '2027-07', guardadoFimProjecao: 1000, fimProjecao: '2027-09', ...patch,
})

describe('comCalculosMetas', () => {
  it('atualiza só os campos calculados e mantém o que o usuário digitou', () => {
    const tela = [meta(1, { nome: 'Digitando', valorAlvo: 5000 }), meta(2)]
    const salvas = [meta(1, { nome: 'Antigo', mesesParaAtingir: 2, previsao: '2026-11', guardadoFimProjecao: 6200 })]
    const r = comCalculosMetas(tela, salvas)
    expect(r[0].nome).toBe('Digitando')
    expect(r[0].valorAlvo).toBe(5000)
    expect(r[0].previsao).toBe('2026-11')
    expect(r[0].mesesParaAtingir).toBe(2)
    expect(r[0].guardadoFimProjecao).toBe(6200)
    // meta sem resposta do servidor fica como está
    expect(r[1]).toEqual(tela[1])
  })
})

describe('comCalculosPlr', () => {
  it('leva bruto, desconto e líquido recalculados para as parcelas da tela', () => {
    const tela: Plr = {
      ativo: true, totalPercentual: 100, aviso: null, estimativaAnual: 10000,
      parcelas: [
        { mes: 9, percentual: 60, valor: null, valorBruto: 5000, desconto: 0, valorLiquido: 5000 },
        { mes: 3, percentual: 40, valor: null, valorBruto: 5000, desconto: 300, valorLiquido: 4700 },
      ],
    }
    const salvo: Plr = {
      ativo: true, totalPercentual: 100, aviso: null, estimativaAnual: 20000,
      parcelas: [
        { mes: 9, percentual: 50, valor: null, valorBruto: 10000, desconto: 0, valorLiquido: 10000 },
        { mes: 3, percentual: 50, valor: null, valorBruto: 10000, desconto: 900, valorLiquido: 9100 },
      ],
    }
    const r = comCalculosPlr(tela, salvo)
    expect(r.estimativaAnual).toBe(20000)
    expect(r.parcelas[0].percentual).toBe(60) // digitado
    expect(r.parcelas[1].valorLiquido).toBe(9100) // calculado
    expect(r.parcelas[1].desconto).toBe(900)
  })
})
