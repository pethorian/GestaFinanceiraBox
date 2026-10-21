import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { fmtPctFaixa, rotuloStatus } from '../domain/saude'
import type { SaudeFinanceira } from '../domain/types'
import { SaudeFinanceiraView } from './SaudeFinanceira'

const base: SaudeFinanceira = {
  participa: false,
  rendaMensal: 5000,
  recomendado: { necessidades: 2500, desejos: 1500, poupanca: 1000 },
  atual: { necessidades: 2800, desejos: 900, poupanca: 400 },
  percentuais: { necessidades: 56, desejos: 18, poupanca: 8 },
  diferenca: { necessidades: 300, desejos: -600, poupanca: -600 },
  status: { necessidades: 'ACIMA', desejos: 'OK', poupanca: 'ABAIXO' },
  sobraLivre: 900,
  aporteSugerido: 600,
  categorias: [
    { nome: 'Moradia', grupo: 'NECESSIDADE', mediaMensal: 2000 },
    { nome: 'Lazer', grupo: 'DESEJO', mediaMensal: 900 },
  ],
  diagnostico: ['Seus gastos obrigatórios estão em 56% da renda (meta: até 50%).'],
  poupancaDetalhe: { metas: 400, funcef: 0 },
}

const render = (s: SaudeFinanceira) =>
  renderToStaticMarkup(<SaudeFinanceiraView saude={s} onAlternar={() => {}} onGrupo={() => {}} />)

describe('Saúde financeira: sem adesão', () => {
  const html = render(base)
  it('mostra a explicação, a prévia e o convite', () => {
    expect(html).toContain('Método 50/30/20')
    expect(html).toContain('Prévia com os seus números')
    expect(html).toContain('Quero seguir o método 50/30/20')
    expect(html).toMatch(/role="switch"(?![^>]*checked)/)
  })
  it('não mostra a tela completa', () => {
    expect(html).not.toContain('Classifique suas categorias')
    expect(html).not.toContain('Diagnóstico')
  })
})

describe('Saúde financeira: participando', () => {
  const html = render({ ...base, participa: true })
  it('mostra as 3 faixas com percentual e status', () => {
    for (const nome of ['Necessidades', 'Desejos', 'Poupança']) expect(html).toContain(nome)
    expect(html).toContain('56%')
    expect(html).toContain('Acima da meta')
    expect(html).toContain('Abaixo da meta')
    expect(html).toContain('Dentro da meta')
  })
  it('mostra KPIs, diagnóstico, CTA da reserva com o aporte e as categorias', () => {
    expect(html).toContain('Renda de referência')
    expect(html).toContain('Sobra livre')
    expect(html).toContain('Diagnóstico')
    expect(html).toContain('href="#/metas"')
    expect(html).toMatch(/guardar <b class="num">R\$\s600,00<\/b> por mês/)
    expect(html).toContain('Classifique suas categorias')
    expect(html).toContain('Obrigatório')
    expect(html).toContain('Lazer')
    expect(html).not.toContain('Prévia com os seus números')
  })
  it('sem renda mostra "—" nos percentuais', () => {
    const semRenda = render({ ...base, participa: true, rendaMensal: 0, percentuais: null })
    expect(semRenda).toContain('—')
  })
})

describe('Saúde financeira: FUNCEF como poupança', () => {
  it('mostra o detalhe Metas + FUNCEF quando há FUNCEF', () => {
    const html = render({ ...base, participa: true, atual: { ...base.atual, poupanca: 900 }, poupancaDetalhe: { metas: 400, funcef: 500 } })
    expect(html).toContain('FUNCEF')
    expect(html).toContain('sf-split')
  })
  it('sem FUNCEF não mostra o detalhe', () => {
    expect(render({ ...base, participa: true })).not.toContain('sf-split')
  })
})

describe('Saúde financeira: formatação', () => {
  it('formata percentuais com vírgula e "—" sem renda', () => {
    expect(fmtPctFaixa(56)).toBe('56%')
    expect(fmtPctFaixa(18.5)).toBe('18,5%')
    expect(fmtPctFaixa(null)).toBe('—')
  })
  it('traduz o status de cada faixa', () => {
    expect(rotuloStatus('necessidades', 'OK')).toEqual({ texto: 'Dentro da meta', tom: 'ok' })
    expect(rotuloStatus('desejos', 'ACIMA')).toEqual({ texto: 'Acima da meta', tom: 'alerta' })
    expect(rotuloStatus('poupanca', 'ABAIXO')).toEqual({ texto: 'Abaixo da meta', tom: 'alerta' })
    expect(rotuloStatus('poupanca', 'OK')).toEqual({ texto: 'Meta atingida', tom: 'ok' })
  })
})
