import { rotuloCompetencia } from '../domain/format'
import type {
  Beneficio, Bonus, Cargo, Categoria, Dados, Decimo, Ferias, Lancamento, Meta, MetaEntrada, MesProjecao, ParametrosVigentes,
  ParcelaPlr, Perfil, Plr, Projecao, ResumoGastos, TabelaFaixas,
} from '../domain/types'
import { http } from './http'

const comRotulo = (m: Omit<MesProjecao, 'rotulo'>): MesProjecao => ({ ...m, rotulo: rotuloCompetencia(m.competencia) })

/** Todas as operações do front passam por aqui; nenhuma regra de negócio ou cálculo mora no cliente. */
export const api = {
  dados: () => http.get<Dados>('/api/dados'),
  restaurarPadrao: () => http.post<Dados>('/api/dados/restaurar'),

  projecao: async (): Promise<Projecao> => {
    const p = await http.get<Omit<Projecao, 'meses'> & { meses: Omit<MesProjecao, 'rotulo'>[] }>('/api/projecao')
    return { ...p, meses: p.meses.map(comRotulo) }
  },
  mesComum: async (): Promise<MesProjecao> => comRotulo(await http.get('/api/projecao/mes-comum')),

  parametros: () => http.get<ParametrosVigentes>('/api/parametros/vigentes'),
  categorias: () => http.get<Categoria[]>('/api/categorias'),
  resumoGastos: () => http.get<ResumoGastos>('/api/lancamentos/resumo'),

  // parâmetros (tabelas com vigência)
  salvarInss: (t: TabelaFaixas) => http.put<TabelaFaixas[]>('/api/parametros/inss', t),
  salvarIrpf: (t: TabelaFaixas) => http.put<TabelaFaixas[]>('/api/parametros/irpf', t),
  salvarBeneficio: (b: Beneficio) => http.put<Beneficio[]>('/api/parametros/beneficios', b),
  salvarCargo: (c: Cargo) => http.put<Cargo>(`/api/parametros/cargos/${c.id}`, c),

  salvarPerfil: (p: Perfil) => http.put<Perfil>('/api/perfil', p),
  salvarFerias: (f: Ferias) => http.put<Ferias>('/api/ferias', f),
  salvarDecimo: (ativo: boolean) => http.put<Decimo>('/api/decimo', { ativo }),
  bonus: () => http.get<Bonus>('/api/bonus'),
  salvarBonus: (b: Bonus) => http.put<Bonus>('/api/bonus', b),
  plr: () => http.get<Plr>('/api/plr'),
  /** envia só o que o usuário edita; o back end devolve os valores calculados (bruto, desconto, líquido) */
  salvarPlr: (plr: { ativo: boolean; parcelas: ParcelaPlr[] }) =>
    http.put<Plr>('/api/plr', {
      ativo: plr.ativo,
      parcelas: plr.parcelas.map(({ mes, percentual, valor }) => ({ mes, percentual, valor })),
    }),

  /** nova linha com os valores padrão definidos pelo servidor */
  novoLancamento: () => http.post<Lancamento>('/api/lancamentos/novo'),
  salvarLancamento: (l: Lancamento) => http.put<Lancamento>(`/api/lancamentos/${l.id}`, l),
  removerLancamento: (id: number) => http.delete(`/api/lancamentos/${id}`),

  novaMeta: () => http.post<Meta>('/api/metas/novo'),
  salvarMeta: (id: number, m: MetaEntrada) => http.put<Meta>(`/api/metas/${id}`, m),
  removerMeta: (id: number) => http.delete(`/api/metas/${id}`),
}
