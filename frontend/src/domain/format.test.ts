import { describe, expect, it } from 'vitest'
import { fmtAliquota, fmtPct, rotuloCompetencia, rotuloCompetenciaLongo, tituloCompetencia } from './format'

describe('formatação', () => {
  it('monta rótulos a partir da competência', () => {
    expect(rotuloCompetencia('2026-10')).toBe('Out/26')
    expect(rotuloCompetenciaLongo('2027-01')).toBe('Jan/2027')
    expect(tituloCompetencia('2026-12')).toBe('Dezembro 2026')
  })
  it('formata faixa de FUNCEF e rótulos de dezembro', () => {
    expect(rotuloCompetencia('2026-12')).toBe('Dez/26')
    expect(fmtPct(12)).toBe('12%')
    expect(fmtPct(5)).toBe('5%')
  })
  it('formata percentuais sem ruído de ponto flutuante', () => {
    expect(fmtAliquota(0.14)).toBe('14%')
    expect(fmtAliquota(0.075)).toBe('7,5%')
    expect(fmtPct(6)).toBe('6%')
  })
})
