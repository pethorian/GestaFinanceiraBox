import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import type { Cartao, Categoria, CompraCartao, Fatura } from '../domain/types'
import { CartaoView } from './Cartao'
import { CartoesCard } from './CartoesCard'

const cartao: Cartao = {
  id: 7, nome: 'Visa Teste', diaFechamento: 10, diaVencimento: 20, limite: 5000,
  faturaAtual: 233.34, proximaFatura: 163.33, comprometido: 600, limiteDisponivel: 4400,
}
const outro: Cartao = { ...cartao, id: 8, nome: 'Master Teste', limite: null, limiteDisponivel: null }
const categorias: Categoria[] = [{ nome: 'Moradia', ordem: 1, grupo: 'NECESSIDADE' }, { nome: 'Lazer', ordem: 2, grupo: 'DESEJO' }]
const base = { cartaoId: 7, categoria: 'Lazer', dataCompra: '2026-10-05', parcelas: 0, ultimaFatura: null, parcelasRestantes: null }
const compras: CompraCartao[] = [
  { ...base, id: 1, descricao: 'Notebook teste', tipo: 'PARCELADA', valor: 600, parcelas: 3, valorParcela: 200, primeiraFatura: '2026-10', ultimaFatura: '2026-12', parcelasRestantes: 3 },
  { ...base, id: 2, descricao: 'Streaming teste', tipo: 'ASSINATURA', valor: 39.9, valorParcela: 39.9, primeiraFatura: '2026-11' },
  { ...base, id: 3, descricao: 'Jantar teste', tipo: 'A_VISTA', valor: 80, valorParcela: 80, primeiraFatura: '2026-10' },
]
const faturas: Fatura[] = [
  { mes: '2026-10', total: 280, itens: [
    { compraId: 1, descricao: 'Notebook teste', categoria: 'Lazer', parcela: '1/3', valor: 200 },
    { compraId: 3, descricao: 'Jantar teste', categoria: 'Lazer', parcela: null, valor: 80 },
  ] },
  { mes: '2026-11', total: 239.9, itens: [] },
]

const noop = () => {}
const view = (over: Partial<Parameters<typeof CartaoView>[0]> = {}) => renderToStaticMarkup(
  <CartaoView cartoes={[cartao]} selecionado={cartao} categorias={categorias} compras={compras} faturas={faturas}
    onSelecionar={noop} onSalvarCartao={noop} onRemoverCartao={noop} onNovaCompra={noop} onEditarCompra={noop}
    onRemoverCompra={noop} {...over} />)

describe('Aba Cartão de crédito', () => {
  const html = view()
  it('mostra fatura atual, próxima fatura e limite disponível vindos do back end', () => {
    expect(html).toContain('Fatura atual')
    expect(html).toContain('Próxima fatura')
    expect(html).toContain('Limite disponível')
    expect(html).toContain('Visa Teste')
    expect(html).toMatch(/233,34/)
    expect(html).toMatch(/4\.400,00/)
  })
  it('lista as compras com edição inline e valor da parcela/faturas do servidor', () => {
    expect(html).toContain('Notebook teste')
    expect(html).toContain('Streaming teste')
    expect(html).toContain('aria-label="Data da compra"')
    expect(html).toContain('À vista')
    expect(html).toContain('Assinatura')
    expect(html).toContain('3x de')
    expect(html).toContain('restam 3')
    expect(html).toContain('aria-label="Valor mensal"')
    expect(html).toContain('aria-label="Valor total"')
    expect(html).toContain('+ Nova compra')
  })
  it('parcelas só aparecem na compra parcelada', () => {
    expect(html.match(/aria-label="Parcelas"/g)).toHaveLength(1)
  })
  it('lista as faturas por mês com total e mantém os itens recolhidos', () => {
    expect(html).toContain('Faturas dos próximos 12 meses')
    expect(html).toContain('Out/2026')
    expect(html).toContain('aria-expanded="false"')
    expect(html).not.toContain('1/3')
  })
  it('expande os itens da fatura aberta, com rótulo de parcela', () => {
    const aberta = view({ faturaAberta: '2026-10' })
    expect(aberta).toContain('aria-expanded="true"')
    expect(aberta).toContain('1/3')
    expect(aberta).toContain('Jantar teste')
  })
  it('só mostra o seletor com mais de um cartão', () => {
    expect(html).not.toContain('aria-label="Cartão"')
    expect(view({ cartoes: [cartao, outro] })).toContain('aria-label="Cartão"')
  })
  it('sem limite informado, avisa em vez de mostrar valor', () => {
    expect(view({ cartoes: [outro], selecionado: outro })).toContain('Sem limite informado')
  })
  it('sem cartões, convida a cadastrar', () => {
    const vazio = view({ cartoes: [], selecionado: null, compras: [], faturas: [] })
    expect(vazio).toContain('Nenhum cartão cadastrado')
    expect(vazio).toContain('+ Novo cartão')
    expect(vazio).not.toContain('+ Nova compra')
  })
  it('abre o formulário de cartão', () => {
    const novo = view({ formInicial: 'novo' })
    expect(novo).toContain('Dia de fechamento')
    expect(novo).toContain('Dia de vencimento')
    expect(novo).toContain('Limite (opcional)')
  })
})

describe('Card de cartões nos Gastos fixos', () => {
  it('mostra cada cartão com a fatura do mês, link para as compras e a nota', () => {
    const html = renderToStaticMarkup(<CartoesCard cartoes={[cartao, outro]} onCadastrar={noop} />)
    expect(html).toContain('Cartões de crédito')
    expect(html).toContain('Visa Teste')
    expect(html).toContain('Master Teste')
    expect(html).toContain('href="#/cartao?id=7"')
    expect(html).toContain('Ver compras')
    expect(html).toContain('+ Cadastrar cartão')
    expect(html).toContain('entra nos gastos fixos')
  })
  it('sem cartões mostra o vazio e o botão de cadastro; o formulário tem os campos', () => {
    const vazio = renderToStaticMarkup(<CartoesCard cartoes={[]} onCadastrar={noop} />)
    expect(vazio).toContain('Nenhum cartão cadastrado')
    const form = renderToStaticMarkup(<CartoesCard cartoes={[]} onCadastrar={noop} formAberto />)
    expect(form).toContain('Nome do cartão')
    expect(form).toContain('Dia de fechamento')
    expect(form).toContain('Dia de vencimento')
    expect(form).toContain('Limite (opcional)')
  })
})
