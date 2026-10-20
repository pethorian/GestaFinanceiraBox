/** Formatação para exibição. Nenhum cálculo de negócio aqui. */
export const MES = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez']
export const MES_LONGO = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
]

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
export const fmt = (v: number) => brl.format(v)
/** Formata um percentual já em escala 0..100 (ex.: 7.5 -> "7,5%"). */
export const fmtPct = (v: number) => `${String(Number(v.toFixed(2))).replace('.', ',')}%`
/** Alíquota em fração (0.075) -> "7,5%". */
export const fmtAliquota = (v: number) => fmtPct(v * 100)
export const fmtNum = (v: number) => v.toFixed(2).replace('.', ',')

const partes = (competencia: string) => {
  const [a, m] = competencia.split('-')
  return { ano: a, mes: Number(m) }
}

/** "2026-10" -> "Out/26" */
export const rotuloCompetencia = (c: string) => {
  const { ano, mes } = partes(c)
  return `${MES[mes - 1]}/${ano.slice(2)}`
}
/** "2026-10" -> "Out/2026" */
export const rotuloCompetenciaLongo = (c: string) => {
  const { ano, mes } = partes(c)
  return `${MES[mes - 1]}/${ano}`
}
/** "2026-10" -> "Outubro 2026" */
export const tituloCompetencia = (c: string) => {
  const { ano, mes } = partes(c)
  return `${MES_LONGO[mes - 1]} ${ano}`
}
