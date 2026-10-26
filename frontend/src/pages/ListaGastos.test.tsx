import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import type { Categoria, Lancamento, ResumoGastos } from '../domain/types'
import { ListaGastos } from './ListaGastos'

const lancamentos: Lancamento[] = [
  { id: 1, descricao: 'Aluguel teste', categoria: 'Moradia', tipo: 'RECORRENTE', valor: 2000, mesInicial: '2027-01', parcelas: 0 },
  { id: 2, descricao: 'Notebook teste', categoria: 'Pessoal', tipo: 'PARCELADO', valor: 500, mesInicial: '2027-01', parcelas: 10 },
  { id: 3, descricao: 'Viagem teste', categoria: 'Lazer', tipo: 'PONTUAL', valor: 3000, mesInicial: '2027-03', parcelas: 0 },
]
const categorias: Categoria[] = [
  { nome: 'Moradia', ordem: 1, grupo: 'NECESSIDADE' },
  { nome: 'Pessoal', ordem: 2, grupo: 'DESEJO' },
  { nome: 'Lazer', ordem: 3, grupo: 'DESEJO' },
]
const resumoFixos: ResumoGastos = { inicio: '2027-01', mediaMensalTotal: 2417, categorias: [{ categoria: 'Moradia', mediaMensal: 2000 }, { categoria: 'Pessoal', mediaMensal: 417 }] }
const resumoEventuais: ResumoGastos = { inicio: '2027-01', mediaMensalTotal: 250, categorias: [{ categoria: 'Lazer', mediaMensal: 250 }] }

const noop = () => {}
const renderFixos = () => renderToStaticMarkup(
  <ListaGastos grupo="FIXOS" titulo="Gastos fixos" subtitulo="x" rotuloNovo="+ Novo gasto fixo" lancamentos={lancamentos}
    resumo={resumoFixos} categorias={categorias} onAdicionar={noop} onEditar={noop} onRemover={noop} />)
const renderEventuais = () => renderToStaticMarkup(
  <ListaGastos grupo="EVENTUAIS" titulo="Gastos eventuais" subtitulo="x" rotuloNovo="+ Novo gasto eventual" lancamentos={lancamentos}
    resumo={resumoEventuais} categorias={categorias} onAdicionar={noop} onEditar={noop} onRemover={noop} />)

describe('Gastos fixos', () => {
  const html = renderFixos()
  it('lista só todo mês e parcelado', () => {
    expect(html).toContain('Aluguel teste')
    expect(html).toContain('Notebook teste')
    expect(html).not.toContain('Viagem teste')
    expect(html).toContain('2 itens')
  })
  it('oferece só Todo mês e Parcelado, com coluna de tipo e botão próprio', () => {
    expect(html).toContain('Todo mês')
    expect(html).toContain('Parcelado')
    expect(html).not.toContain('Uma vez')
    expect(html).toContain('aria-label="Tipo"')
    expect(html).toContain('+ Novo gasto fixo')
  })
  it('usa o resumo dos fixos', () => {
    expect(html).toContain('<span>Moradia</span>')
    expect(html).not.toContain('<span>Lazer</span>')
  })
})

describe('Gastos eventuais', () => {
  const html = renderEventuais()
  it('lista só os de uma vez', () => {
    expect(html).toContain('Viagem teste')
    expect(html).not.toContain('Aluguel teste')
    expect(html).not.toContain('Notebook teste')
    expect(html).toContain('1 item')
  })
  it('não tem coluna de tipo nem de parcelas e tem botão próprio', () => {
    expect(html).not.toContain('aria-label="Tipo"')
    expect(html).not.toContain('aria-label="Parcelas"')
    expect(html).not.toContain('Parcelas')
    expect(html).toContain('+ Novo gasto eventual')
  })
  it('usa o resumo dos eventuais', () => {
    expect(html).toContain('<span>Lazer</span>')
    expect(html).not.toContain('<span>Moradia</span>')
  })
})
