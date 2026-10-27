import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { api } from '../api/api'
import { ApiError } from '../api/http'
import type {
  Bonus, Categoria, Dados, Ferias, GrupoGasto, Lancamento, Meta, MesProjecao, MetaEntrada, ParametrosVigentes,
  ParcelaPlr, Perfil, Plr, Projecao, ResumoGastos, ChaveAdicional, DestinoEntrada,
  TipoLancamento, Cartao, PeriodoSaude,
} from '../domain/types'

/** Erros do plano de uso por evento (null = sem erro). Ficam na tela, sem desfazer o que o usuário digitou. */
export type ErrosAdicionais = Record<ChaveAdicional, string[] | null>

/** 400: um item por detalhe ("destinos[1].nome: obrigatório" → "Destino 2 (nome): obrigatório"); 422: a mensagem. */
export function mensagensErroDestinos(e: unknown): string[] {
  if (!(e instanceof ApiError)) return ['Erro inesperado ao salvar o plano de uso.']
  if (e.status === 400 && e.detalhes.length > 0) {
    return e.detalhes.map((d) => d
      .replace(/^destinos\[(\d+)\]\.?(\w*):\s*/, (_, i: string, campo: string) => `Destino ${Number(i) + 1}${campo ? ` (${campo})` : ''}: `)
      .replace(/^destinos:\s*/, 'Destinos: '))
  }
  return [e.message]
}

/**
 * Estado da aplicação. Tudo vem do back end: dados do usuário (banco), projeção e demais cálculos.
 * As telas editam o estado local (para a digitação não travar), e as alterações são enviadas ao back end
 * com um pequeno atraso. Depois de salvar, a projeção e os resumos são recalculados pelo back end.
 */
interface AppCtx {
  dados: Dados
  projecao: Projecao
  /** mês "comum" (sem 13º e férias), calculado pelo back end */
  mesComum: MesProjecao
  resumoFixos: ResumoGastos
  resumoEventuais: ResumoGastos
  parametros: ParametrosVigentes
  categorias: Categoria[]
  /** cartões com fatura atual, próxima fatura e limite disponível (calculados pelo back end) */
  cartoes: Cartao[]
  salvando: boolean
  aviso: string | null
  limparAviso: () => void

  editarPerfil: (patch: Partial<Perfil>) => void
  editarFerias: (patch: Partial<Ferias>) => void
  alternarDecimo: (ativo: boolean) => Promise<void>
  /** liga/desliga o PLR; ao ligar, o back end devolve as parcelas padrão */
  alternarPlr: (ativo: boolean) => Promise<void>
  editarPlr: (parcelas: ParcelaPlr[]) => void
  editarBonus: (patch: Partial<Bonus>) => void
  adicionarLancamento: (tipo?: TipoLancamento) => Promise<void>
  editarLancamento: (id: number, patch: Partial<Lancamento>) => void
  removerLancamento: (id: number) => Promise<void>
  adicionarMeta: () => Promise<void>
  editarMeta: (id: number, patch: Partial<MetaEntrada>) => void
  removerMeta: (id: number) => Promise<void>
  restaurar: () => Promise<void>
  /** adere (ou não) ao método 50/30/20 da aba Saúde financeira */
  alternarMetodo: (participa: boolean) => Promise<void>
  /** troca a base da saúde financeira (média de 12 meses ou mês atual); vale até recarregar a página */
  definirPeriodoSaude: (periodo: PeriodoSaude) => Promise<void>
  /** classifica a categoria como obrigatória (NECESSIDADE) ou lazer (DESEJO) e recalcula a saúde financeira */
  definirGrupoCategoria: (nome: string, grupo: GrupoGasto) => Promise<void>
  /** grava alterações de parâmetros e recarrega tudo (as projeções dependem deles) */
  salvarParametros: (op: () => Promise<unknown>) => Promise<void>
  /** grava (com atraso) a lista inteira de destinos do 13º ou do PLR; erros 400/422 vão para errosAdicionais */
  salvarDestinos: (evento: ChaveAdicional, destinos: DestinoEntrada[]) => void
  errosAdicionais: ErrosAdicionais
  /**
   * Executa uma gravação de cartão ou compra e recarrega cartões, projeção, resumos e saúde financeira.
   * Devolve false (e mostra o aviso) se o servidor recusar; não descarta o que está na tela.
   */
  alterarCartoes: (op: () => Promise<unknown>) => Promise<boolean>
}

interface Carga {
  dados: Dados
  projecao: Projecao
  mesComum: MesProjecao
  resumoFixos: ResumoGastos
  resumoEventuais: ResumoGastos
  parametros: ParametrosVigentes
  categorias: Categoria[]
  cartoes: Cartao[]
}

const Ctx = createContext<AppCtx | null>(null)
const ATRASO_MS = 450

const mensagem = (e: unknown) =>
  e instanceof ApiError ? [e.message, ...e.detalhes].join(' — ') : 'Erro inesperado ao falar com o servidor.'

function Tela({ children }: { children: ReactNode }) {
  return <div style={{ display: 'grid', placeItems: 'center', minHeight: '100vh', textAlign: 'center', padding: 24 }}>{children}</div>
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [carga, setCarga] = useState<Carga | null>(null)
  const [erroFatal, setErroFatal] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [pendentes, setPendentes] = useState(0)
  const [errosAdicionais, setErrosAdicionais] = useState<ErrosAdicionais>({ decimo: null, plr: null })
  const cargaRef = useRef<Carga | null>(null)
  cargaRef.current = carga
  const timers = useRef(new Map<string, ReturnType<typeof setTimeout>>())
  const seqCalculos = useRef(0)
  // período escolhido na Saúde financeira; os recálculos pedem a saúde com ele
  const periodoSaude = useRef<PeriodoSaude>('MEDIA_12')

  const carregarTudo = useCallback(async () => {
    try {
      periodoSaude.current = 'MEDIA_12' // /api/dados traz a média de 12 meses
      const [dados, projecao, mesComum, resumoFixos, resumoEventuais, parametros, categorias, cartoes] = await Promise.all([
        api.dados(), api.projecao(), api.mesComum(), api.resumoGastos("FIXOS"), api.resumoGastos("EVENTUAIS"), api.parametros(), api.categorias(),
        api.cartoes(),
      ])
      setCarga({ dados, projecao, mesComum, resumoFixos, resumoEventuais, parametros, categorias, cartoes })
      setErroFatal(null)
    } catch (e) {
      setErroFatal(mensagem(e))
    }
  }, [])

  useEffect(() => { void carregarTudo() }, [carregarTudo])

  /** Pede ao back end a projeção e os resumos atualizados (ignora respostas antigas). */
  const recalcular = useCallback(async () => {
    const minha = ++seqCalculos.current
    const [projecao, mesComum, resumoFixos, resumoEventuais, plr, saudeFinanceira, planoDecimo, planoPlr, cartoes, categorias] = await Promise.all([
      api.projecao(), api.mesComum(), api.resumoGastos("FIXOS"), api.resumoGastos("EVENTUAIS"), api.plr(), api.saudeFinanceira(periodoSaude.current),
      api.planoAdicional('decimo'), api.planoAdicional('plr'), api.cartoes(), api.categorias(),
    ])
    if (minha === seqCalculos.current) {
      // a estimativa anual do PLR depende do cargo do perfil; as parcelas digitadas continuam como estão na tela.
      // A saúde financeira depende da renda, dos gastos e dos aportes: volta inteira do servidor.
      setCarga((c) => (c ? {
        ...c, projecao, mesComum, resumoFixos, resumoEventuais, cartoes,
        // uma compra de cartão pode criar categoria nova: a lista volta do servidor
        categorias,
        // o plano de uso (prévia, ativo, nome das metas) depende do salário, dos eventos e das metas: volta inteiro
        dados: {
          ...c.dados, saudeFinanceira, plr: { ...c.dados.plr, estimativaAnual: plr.estimativaAnual },
          adicionais: { decimo: planoDecimo, plr: planoPlr },
        },
      } : c))
    }
  }, [])

  const falhou = useCallback(async (e: unknown) => {
    setAviso(mensagem(e))
    await carregarTudo() // volta ao que está realmente salvo no banco
  }, [carregarTudo])

  /** Executa uma operação de gravação e, em seguida, recalcula; erros restauram o estado do servidor. */
  const executar = useCallback(async (op: () => Promise<unknown>) => {
    setPendentes((n) => n + 1)
    try {
      await op()
      await recalcular()
    } catch (e) {
      await falhou(e)
    } finally {
      setPendentes((n) => n - 1)
    }
  }, [recalcular, falhou])

  /** Grava depois de um instante sem novas edições da mesma chave. */
  const agendar = useCallback((chave: string, op: () => Promise<unknown>) => {
    const antigo = timers.current.get(chave)
    if (antigo) clearTimeout(antigo)
    else setPendentes((n) => n + 1)
    timers.current.set(chave, setTimeout(() => {
      timers.current.delete(chave)
      void executar(op).finally(() => setPendentes((n) => n - 1))
    }, ATRASO_MS))
  }, [executar])

  const mudar = useCallback((fn: (d: Dados) => Dados) =>
    setCarga((c) => (c ? { ...c, dados: fn(c.dados) } : c)), [])

  const acoes = useMemo(() => ({
    editarPerfil: (patch: Partial<Perfil>) => {
      mudar((d) => ({ ...d, perfil: { ...d.perfil, ...patch } }))
      agendar('perfil', () => api.salvarPerfil(cargaRef.current!.dados.perfil))
    },
    editarFerias: (patch: Partial<Ferias>) => {
      mudar((d) => ({ ...d, ferias: { ...d.ferias, ...patch } }))
      agendar('ferias', () => api.salvarFerias(cargaRef.current!.dados.ferias))
    },
    alternarDecimo: (ativo: boolean) => executar(async () => {
      mudar((d) => ({ ...d, decimo: { ...d.decimo, ativo } }))
      const decimo = await api.salvarDecimo(ativo)
      mudar((d) => ({ ...d, decimo }))
    }),

    alternarPlr: (ativo: boolean) => executar(async () => {
      mudar((d) => ({ ...d, plr: { ...d.plr, ativo } }))
      const salvo: Plr = await api.salvarPlr({ ativo, parcelas: cargaRef.current!.dados.plr.parcelas })
      // ao ligar, as parcelas padrão vêm do servidor: a tela recebe o PLR inteiro
      mudar((d) => ({ ...d, plr: salvo }))
    }),
    editarPlr: (parcelas: ParcelaPlr[]) => {
      mudar((d) => ({ ...d, plr: { ...d.plr, parcelas } }))
      agendar('plr', async () => {
        const atual = cargaRef.current!.dados.plr
        const salvo: Plr = await api.salvarPlr({ ativo: atual.ativo, parcelas: atual.parcelas })
        // só os campos calculados pelo back end; os digitados continuam como estão na tela
        mudar((d) => ({
          ...d,
          plr: {
            ...d.plr,
            totalPercentual: salvo.totalPercentual,
            aviso: salvo.aviso,
            estimativaAnual: salvo.estimativaAnual,
            parcelas: d.plr.parcelas.map((p, i) => {
              const calc = salvo.parcelas[i]
              return calc ? { ...p, valorBruto: calc.valorBruto, desconto: calc.desconto, valorLiquido: calc.valorLiquido } : p
            }),
          },
        }))
      })
    },
    editarBonus: (patch: Partial<Bonus>) => {
      mudar((d) => ({ ...d, bonus: { ...d.bonus, ...patch } }))
      agendar('bonus', () => api.salvarBonus(cargaRef.current!.dados.bonus))
    },

    adicionarLancamento: (tipo?: TipoLancamento) => executar(async () => {
      const criado = await api.novoLancamento(tipo)
      mudar((d) => ({ ...d, lancamentos: [...d.lancamentos, criado] }))
    }),
    editarLancamento: (id: number, patch: Partial<Lancamento>) => {
      mudar((d) => ({ ...d, lancamentos: d.lancamentos.map((l) => (l.id === id ? { ...l, ...patch } : l)) }))
      agendar(`lancamento:${id}`, async () => {
        const atual = cargaRef.current!.dados.lancamentos.find((l) => l.id === id)
        if (!atual) return
        const salvo = await api.salvarLancamento(atual)
        // o servidor pode normalizar (ex.: nº de parcelas); a tela segue o que foi salvo
        if (salvo.parcelas !== atual.parcelas) {
          mudar((d) => ({ ...d, lancamentos: d.lancamentos.map((l) => (l.id === id ? { ...l, parcelas: salvo.parcelas } : l)) }))
        }
      })
    },
    removerLancamento: (id: number) => executar(async () => {
      await api.removerLancamento(id)
      mudar((d) => ({ ...d, lancamentos: d.lancamentos.filter((l) => l.id !== id) }))
    }),

    adicionarMeta: () => executar(async () => {
      const criada = await api.novaMeta()
      mudar((d) => ({ ...d, metas: [...d.metas, criada] }))
    }),
    editarMeta: (id: number, patch: Partial<MetaEntrada>) => {
      mudar((d) => ({ ...d, metas: d.metas.map((m) => (m.id === id ? { ...m, ...patch } : m)) }))
      agendar(`meta:${id}`, async () => {
        const atual = cargaRef.current!.dados.metas.find((m) => m.id === id)
        if (!atual) return
        const salva: Meta = await api.salvarMeta(id, atual)
        // só os campos calculados pelo back end; os digitados continuam como estão na tela
        mudar((d) => ({
          ...d,
          metas: d.metas.map((m) => (m.id === id
            ? {
              ...m, falta: salva.falta, percentual: salva.percentual, mesesParaAtingir: salva.mesesParaAtingir,
              previsao: salva.previsao, guardadoFimProjecao: salva.guardadoFimProjecao, fimProjecao: salva.fimProjecao,
            }
            : m)),
        }))
      })
    },
    removerMeta: (id: number) => executar(async () => {
      await api.removerMeta(id)
      mudar((d) => ({ ...d, metas: d.metas.filter((m) => m.id !== id) }))
    }),

    alternarMetodo: (participa: boolean) => executar(async () => {
      mudar((d) => ({ ...d, saudeFinanceira: { ...d.saudeFinanceira, participa } }))
      const saudeFinanceira = await api.salvarSaudeFinanceira(participa, periodoSaude.current)
      mudar((d) => ({ ...d, saudeFinanceira }))
    }),
    definirPeriodoSaude: async (periodo: PeriodoSaude) => {
      periodoSaude.current = periodo
      mudar((d) => ({ ...d, saudeFinanceira: { ...d.saudeFinanceira, periodo } }))
      try {
        const saudeFinanceira = await api.saudeFinanceira(periodo)
        if (periodoSaude.current === periodo) mudar((d) => ({ ...d, saudeFinanceira }))
      } catch (e) {
        await falhou(e)
      }
    },
    definirGrupoCategoria: (nome: string, grupo: GrupoGasto) => executar(async () => {
      // otimista: o controle muda na hora; os números chegam do back end no recálculo (executar → recalcular)
      setCarga((c) => (c ? {
        ...c,
        categorias: c.categorias.map((k) => (k.nome === nome ? { ...k, grupo } : k)),
        dados: {
          ...c.dados,
          saudeFinanceira: {
            ...c.dados.saudeFinanceira,
            categorias: c.dados.saudeFinanceira.categorias.map((k) => (k.nome === nome ? { ...k, grupo } : k)),
          },
        },
      } : c))
      const salva: Categoria = await api.salvarGrupoCategoria(nome, grupo)
      setCarga((c) => (c ? { ...c, categorias: c.categorias.map((k) => (k.nome === nome ? { ...k, ...salva } : k)) } : c))
    }),

    salvarDestinos: (evento: ChaveAdicional, destinos: DestinoEntrada[]) => {
      // o rascunho fica no editor; aqui só se grava a lista inteira depois de um instante sem edições
      agendar(`adicional:${evento}`, async () => {
        try {
          const plano = await api.salvarDestinos(evento, destinos)
          mudar((d) => ({ ...d, adicionais: { ...d.adicionais, [evento]: plano } }))
          setErrosAdicionais((e) => ({ ...e, [evento]: null }))
        } catch (e) {
          // validação (400) ou regra (422): mostra no editor, sem recarregar a tela nem perder a digitação
          if (e instanceof ApiError && (e.status === 400 || e.status === 422)) {
            setErrosAdicionais((x) => ({ ...x, [evento]: mensagensErroDestinos(e) }))
            return
          }
          throw e
        }
      })
    },

    alterarCartoes: async (op: () => Promise<unknown>) => {
      setPendentes((n) => n + 1)
      try {
        await op()
        await recalcular()
        return true
      } catch (e) {
        setAviso(mensagem(e))
        return false
      } finally {
        setPendentes((n) => n - 1)
      }
    },

    salvarParametros: async (op: () => Promise<unknown>) => {
      try {
        await op()
        await carregarTudo()
      } catch (e) {
        setAviso(mensagem(e))
      }
    },

    restaurar: async () => {
      try {
        await api.restaurarPadrao()
        await carregarTudo()
        setErrosAdicionais({ decimo: null, plr: null })
      } catch (e) {
        setAviso(mensagem(e))
      }
    },
  }), [agendar, executar, mudar, carregarTudo, recalcular, falhou])

  const valor = useMemo<AppCtx | null>(() => (carga ? {
    ...carga, ...acoes, salvando: pendentes > 0, aviso, limparAviso: () => setAviso(null), errosAdicionais,
  } : null), [carga, acoes, pendentes, aviso, errosAdicionais])

  if (erroFatal) {
    return (
      <Tela>
        <div>
          <h2>Não foi possível carregar os dados</h2>
          <p>{erroFatal}</p>
          <button className="btn pri" onClick={() => { setErroFatal(null); void carregarTudo() }}>Tentar novamente</button>
        </div>
      </Tela>
    )
  }
  if (!valor) return <Tela><p>Conectando ao servidor…</p></Tela>
  return <Ctx.Provider value={valor}>{children}</Ctx.Provider>
}

export function useApp(): AppCtx {
  const c = useContext(Ctx)
  if (!c) throw new Error('useApp deve ser usado dentro de <AppProvider>')
  return c
}
