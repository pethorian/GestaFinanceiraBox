import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { api } from '../api/api'
import { ApiError } from '../api/http'
import type {
  Bonus, Categoria, Dados, Ferias, Lancamento, Meta, MesProjecao, MetaEntrada, ParametrosVigentes,
  ParcelaDecimo, Perfil, Projecao, ResumoGastos,
} from '../domain/types'

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
  resumoGastos: ResumoGastos
  parametros: ParametrosVigentes
  categorias: Categoria[]
  salvando: boolean
  aviso: string | null
  limparAviso: () => void

  editarPerfil: (patch: Partial<Perfil>) => void
  editarFerias: (patch: Partial<Ferias>) => void
  editarDecimo: (parcelas: ParcelaDecimo[]) => void
  adicionarParcelaDecimo: () => Promise<void>
  editarPlr: (parcelas: ParcelaDecimo[]) => void
  adicionarParcelaPlr: () => Promise<void>
  editarBonus: (patch: Partial<Bonus>) => void
  adicionarLancamento: () => Promise<void>
  editarLancamento: (id: number, patch: Partial<Lancamento>) => void
  removerLancamento: (id: number) => Promise<void>
  adicionarMeta: () => Promise<void>
  editarMeta: (id: number, patch: Partial<MetaEntrada>) => void
  removerMeta: (id: number) => Promise<void>
  restaurar: () => Promise<void>
  /** grava alterações de parâmetros e recarrega tudo (as projeções dependem deles) */
  salvarParametros: (op: () => Promise<unknown>) => Promise<void>
}

interface Carga {
  dados: Dados
  projecao: Projecao
  mesComum: MesProjecao
  resumoGastos: ResumoGastos
  parametros: ParametrosVigentes
  categorias: Categoria[]
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
  const cargaRef = useRef<Carga | null>(null)
  cargaRef.current = carga
  const timers = useRef(new Map<string, ReturnType<typeof setTimeout>>())
  const seqCalculos = useRef(0)

  const carregarTudo = useCallback(async () => {
    try {
      const [dados, projecao, mesComum, resumoGastos, parametros, categorias] = await Promise.all([
        api.dados(), api.projecao(), api.mesComum(), api.resumoGastos(), api.parametros(), api.categorias(),
      ])
      setCarga({ dados, projecao, mesComum, resumoGastos, parametros, categorias })
      setErroFatal(null)
    } catch (e) {
      setErroFatal(mensagem(e))
    }
  }, [])

  useEffect(() => { void carregarTudo() }, [carregarTudo])

  /** Pede ao back end a projeção e os resumos atualizados (ignora respostas antigas). */
  const recalcular = useCallback(async () => {
    const minha = ++seqCalculos.current
    const [projecao, mesComum, resumoGastos, plr] = await Promise.all([
      api.projecao(), api.mesComum(), api.resumoGastos(), api.plr(),
    ])
    if (minha === seqCalculos.current) {
      // a estimativa anual do PLR depende do cargo do perfil; as parcelas digitadas continuam como estão na tela
      setCarga((c) => (c ? {
        ...c, projecao, mesComum, resumoGastos,
        dados: { ...c.dados, plr: { ...c.dados.plr, estimativaAnual: plr.estimativaAnual } },
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
    editarDecimo: (parcelas: ParcelaDecimo[]) => {
      mudar((d) => ({ ...d, decimo: { ...d.decimo, parcelas } }))
      agendar('decimo', async () => {
        const salvo = await api.salvarDecimo(cargaRef.current!.dados.decimo.parcelas)
        mudar((d) => ({ ...d, decimo: { ...d.decimo, totalPercentual: salvo.totalPercentual, aviso: salvo.aviso } }))
      })
    },

    adicionarParcelaDecimo: () => executar(async () => {
      const decimo = await api.novaParcelaDecimo()
      mudar((d) => ({ ...d, decimo }))
    }),

    editarPlr: (parcelas: ParcelaDecimo[]) => {
      mudar((d) => ({ ...d, plr: { ...d.plr, parcelas } }))
      agendar('plr', async () => {
        const salvo = await api.salvarPlr(cargaRef.current!.dados.plr.parcelas)
        mudar((d) => ({
          ...d,
          plr: { ...d.plr, totalPercentual: salvo.totalPercentual, aviso: salvo.aviso, estimativaAnual: salvo.estimativaAnual },
        }))
      })
    },
    adicionarParcelaPlr: () => executar(async () => {
      const plr = await api.novaParcelaPlr()
      mudar((d) => ({ ...d, plr }))
    }),
    editarBonus: (patch: Partial<Bonus>) => {
      mudar((d) => ({ ...d, bonus: { ...d.bonus, ...patch } }))
      agendar('bonus', () => api.salvarBonus(cargaRef.current!.dados.bonus))
    },

    adicionarLancamento: () => executar(async () => {
      const criado = await api.novoLancamento()
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
            ? { ...m, falta: salva.falta, percentual: salva.percentual, mesesParaAtingir: salva.mesesParaAtingir, previsao: salva.previsao }
            : m)),
        }))
      })
    },
    removerMeta: (id: number) => executar(async () => {
      await api.removerMeta(id)
      mudar((d) => ({ ...d, metas: d.metas.filter((m) => m.id !== id) }))
    }),

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
      } catch (e) {
        setAviso(mensagem(e))
      }
    },
  }), [agendar, executar, mudar, carregarTudo])

  const valor = useMemo<AppCtx | null>(() => (carga ? {
    ...carga, ...acoes, salvando: pendentes > 0, aviso, limparAviso: () => setAviso(null),
  } : null), [carga, acoes, pendentes, aviso])

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
