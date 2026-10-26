import { rotuloCompetencia } from '../domain/format'
import type {
  Beneficio, Bonus, Cargo, Categoria, Dados, GrupoGasto, SaudeFinanceira, Decimo, Ferias, Lancamento, Meta, MetaEntrada, MesProjecao, ParametrosVigentes,
  ParcelaPlr, Perfil, Plr, Projecao, ResumoGastos, TabelaFaixas, ChaveAdicional, DestinoEntrada, PlanoAdicional,
  GrupoLancamento, TipoLancamento, Cartao, CartaoEntrada, CompraCartao, CompraEntrada, Fatura,
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
  /** NECESSIDADE (obrigatório) ou DESEJO (lazer); depois recarregue a saúde financeira */
  salvarGrupoCategoria: (nome: string, grupo: GrupoGasto) =>
    http.put<Categoria>(`/api/categorias/${encodeURIComponent(nome)}/grupo`, { grupo }),

  // saúde financeira (método 50/30/20)
  saudeFinanceira: () => http.get<SaudeFinanceira>('/api/saude-financeira'),
  salvarSaudeFinanceira: (participa: boolean) => http.put<SaudeFinanceira>('/api/saude-financeira', { participa }),
  /** grupo: FIXOS (todo mês + parcelado) ou EVENTUAIS (uma vez); sem grupo soma tudo */
  resumoGastos: (grupo?: GrupoLancamento) =>
    http.get<ResumoGastos>(grupo ? `/api/lancamentos/resumo?grupo=${grupo}` : '/api/lancamentos/resumo'),

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

  // plano de uso do 13º e do PLR
  planoAdicional: (evento: ChaveAdicional) => http.get<PlanoAdicional>(`/api/adicionais/${evento}/destinos`),
  /** substitui a lista inteira (vazia = sem destinos); devolve o plano com os valores da prévia */
  salvarDestinos: (evento: ChaveAdicional, destinos: DestinoEntrada[]) =>
    http.put<PlanoAdicional>(
      `/api/adicionais/${evento}/destinos`,
      destinos.map(({ nome, tipo, percentual, metaId }) => ({ nome, tipo, percentual, metaId: tipo === 'RESERVA' ? metaId : null })),
    ),

  /** nova linha com os valores padrão definidos pelo servidor */
  novoLancamento: (tipo?: TipoLancamento) =>
    http.post<Lancamento>(tipo ? `/api/lancamentos/novo?tipo=${tipo}` : '/api/lancamentos/novo'),
  salvarLancamento: (l: Lancamento) => http.put<Lancamento>(`/api/lancamentos/${l.id}`, l),
  removerLancamento: (id: number) => http.delete(`/api/lancamentos/${id}`),

  novaMeta: () => http.post<Meta>('/api/metas/novo'),
  salvarMeta: (id: number, m: MetaEntrada) => http.put<Meta>(`/api/metas/${id}`, m),
  removerMeta: (id: number) => http.delete(`/api/metas/${id}`),

  // cartão de crédito
  cartoes: () => http.get<Cartao[]>('/api/cartoes'),
  criarCartao: (c: CartaoEntrada) => http.post<Cartao>('/api/cartoes', c),
  salvarCartao: (id: number, c: CartaoEntrada) => http.put<Cartao>(`/api/cartoes/${id}`, c),
  removerCartao: (id: number) => http.delete(`/api/cartoes/${id}`),
  comprasCartao: (cartaoId: number) => http.get<CompraCartao[]>(`/api/cartoes/${cartaoId}/compras`),
  criarCompra: (cartaoId: number, c: CompraEntrada) =>
    http.post<CompraCartao>(`/api/cartoes/${cartaoId}/compras`, c),
  salvarCompra: (id: number, c: CompraEntrada) => http.put<CompraCartao>(`/api/cartoes/compras/${id}`, c),
  removerCompra: (id: number) => http.delete(`/api/cartoes/compras/${id}`),
  /** 12 faturas por mês de vencimento, a partir de `inicio` (AAAA-MM; padrão: mês atual) */
  faturasCartao: (cartaoId: number, inicio?: string) =>
    http.get<Fatura[]>(inicio ? `/api/cartoes/${cartaoId}/faturas?inicio=${inicio}` : `/api/cartoes/${cartaoId}/faturas`),
}
