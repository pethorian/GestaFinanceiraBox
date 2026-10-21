import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { fmtPctFaixa, rotuloStatus } from '../domain/saude'
import type { SaudeAdicionais, SaudeFinanceira } from '../domain/types'
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
  adicionais: {
    totalLiquidoPeriodo: 0,
    divisao: { necessidades: 0, desejos: 0, poupanca: 0, semDestino: 0 },
    percentuais: null,
    comparacao: null,
    definido: false,
    diagnostico: ['Não há 13º nem PLR na projeção dos próximos 12 meses.'],
  },
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

describe('Saúde financeira: card 13º e PLR', () => {
  const FRASE_DIV = 'Dos seus adicionais nos próximos 12 meses (13º e PLR), 50% vão para lazer e 30% para reservas; 20% ficam sem destino definido, no saldo disponível.'
  const FRASE_20 = 'Você guarda pelo menos 20% dos seus adicionais, como sugere o método 50/30/20.'
  const definido: SaudeAdicionais = {
    totalLiquidoPeriodo: 10000,
    divisao: { necessidades: 0, desejos: 5000, poupanca: 3000, semDestino: 2000 },
    percentuais: { necessidades: 0, desejos: 50, poupanca: 30, semDestino: 20 },
    comparacao: {
      recomendado: { necessidades: 5000, desejos: 3000, poupanca: 2000 },
      diferenca: { necessidades: -5000, desejos: 2000, poupanca: 1000 },
      status: { necessidades: 'OK', desejos: 'ACIMA', poupanca: 'OK' },
    },
    definido: true,
    diagnostico: [FRASE_DIV, FRASE_20],
  }
  const comAdicionais: SaudeFinanceira = {
    ...base, participa: true, adicionais: definido, diagnostico: [...base.diagnostico, FRASE_DIV, FRASE_20],
  }

  it('mostra a divisão × ideal, o status por faixa e as frases, com link para ajustar', () => {
    const html = render(comAdicionais)
    expect(html).toContain('13º e PLR')
    expect(html).toMatch(/R\$\s10\.000,00 líquido/)
    expect(html).toContain('Ideal')
    expect(html).toContain('Seu plano')
    expect(html).toContain('Sem destino')
    expect(html).toContain('Acima da meta')
    expect(html).toContain('Ajustar plano de uso')
    expect(html).toContain('href="#/decimo"')
  })

  it('participando: as frases aparecem uma vez, no Diagnóstico (o card fica compacto)', () => {
    const html = render(comAdicionais)
    expect(html.split(FRASE_20).length - 1).toBe(1)
    expect(html.split(FRASE_DIV).length - 1).toBe(1)
    expect(html).toContain('sf-adic compacto')
    expect(html.split('Seus gastos obrigatórios estão em 56%').length - 1).toBe(1)
  })

  it('sem adesão (sem Diagnóstico): as frases aparecem uma vez, no card', () => {
    const html = render({ ...comAdicionais, participa: false })
    expect(html).not.toContain('Diagnóstico')
    expect(html.split(FRASE_20).length - 1).toBe(1)
    expect(html).not.toContain('sf-adic compacto')
  })

  it('sem plano definido mostra o convite com link para a tela do 13º/PLR', () => {
    const frase = 'Você ainda não definiu como usar seu 13º.'
    const html = render({
      ...base,
      participa: true,
      diagnostico: [...base.diagnostico, frase],
      adicionais: {
        ...definido, definido: false, diagnostico: [frase],
        divisao: { necessidades: 0, desejos: 0, poupanca: 0, semDestino: 10000 },
        percentuais: { necessidades: 0, desejos: 0, poupanca: 0, semDestino: 100 },
      },
    })
    expect(html.split(frase).length - 1).toBe(1)
    expect(html).toContain('ainda não definiu como usar seus adicionais')
    expect(html).toContain('Definir como usar o 13º e o PLR')
    expect(html).toContain('href="#/decimo"')
    expect(html).not.toContain('Seu plano')
  })

  it('sem adicional no período mostra só o aviso, sem gráfico nem link', () => {
    const html = render({ ...base, participa: true })
    expect(html).toContain('Sem 13º nem PLR nos próximos 12 meses')
    expect(render(base)).toContain('Não há 13º nem PLR') // sem adesão, a frase do back end vai no card
    expect(html).not.toContain('Seu plano')
    expect(html).not.toContain('href="#/decimo"')
  })

  it('aparece também sem adesão ao método', () => {
    const html = render({ ...base, adicionais: definido })
    expect(html).toContain('13º e PLR')
    expect(html).toContain('Seu plano')
  })
})
