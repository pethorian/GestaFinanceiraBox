import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api/api'
import { ApiError } from '../api/http'
import { PlanoUsoResumo } from '../components/adicionais'
import { AvisoPlr } from '../components/AvisoPlr'
import { Icon } from '../components/Icon'
import { PlanoUsoDrawer } from '../components/PlanoUsoDrawer'
import { Card, cls, NumberInput, Page, Switch } from '../components/ui'
import { infoTipo } from '../domain/adicionais'
import { fmt, fmtPct, MES_LONGO } from '../domain/format'
import type {
  DestinoEntrada, ParcelaPlr, PlrControle, PlrDestinoControle, PlrGasto, PlrGastoEntrada, PlrRecebimento, PlrRecebimentoEntrada,
} from '../domain/types'
import { useApp } from '../store/AppContext'

// meses 1..12, como no back end
const OPCOES_MES = MES_LONGO.map((n, i) => <option key={n} value={i + 1}>{n}</option>)

/** Sugestão 80/20 oferecida quando ainda não há destinos (os percentuais são só dados enviados ao servidor). */
export const SUGESTAO_80_20: DestinoEntrada[] = [
  { nome: 'Gastos', tipo: 'OBRIGATORIO', percentual: 80, metaId: null },
  { nome: 'Pessoal', tipo: 'LAZER', percentual: 20, metaId: null },
]

/** Data local de hoje (AAAA-MM-DD), só como valor inicial do formulário. */
const hoje = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
const centavos = (v: number) => Math.round(v * 100) / 100

// ---- controle (apresentação) ------------------------------------------------------------------

interface Acoes {
  onCriarRecebimento: (r: PlrRecebimentoEntrada) => void
  onSalvarRecebimento: (id: number, r: PlrRecebimentoEntrada) => void
  onRemoverRecebimento: (id: number) => void
  onCriarGasto: (g: PlrGastoEntrada) => void
  onSalvarGasto: (id: number, g: PlrGastoEntrada) => void
  onRemoverGasto: (id: number) => void
  onUsarSugestao: () => void
}

/** Cartão de um envelope: alocado, gasto, saldo e a barra do % usado (vermelha quando estoura). */
function Envelope({ nome, tipo, percentual, alocado, gasto, saldo, percentualUsado }: {
  nome: string; tipo?: PlrDestinoControle['tipo']; percentual?: number
} & Pick<PlrDestinoControle, 'alocado' | 'gasto' | 'saldo' | 'percentualUsado'>) {
  const estourou = saldo < 0
  return (
    <div className={`plr-env${estourou ? ' estourou' : ''}`}>
      <div className="plr-env-h">
        <b>
          {tipo && <i className="sf-sw" style={{ background: infoTipo(tipo).cor }} />}
          {nome}
          {percentual !== undefined && <small> {fmtPct(percentual)}</small>}
        </b>
        {estourou && <span className="plr-estouro">Estourou</span>}
      </div>
      <div className="plr-env-v">
        <span>Alocado</span><span className="num">{fmt(alocado)}</span>
        <span>Gasto</span><span className="num">{fmt(gasto)}</span>
        <span>Saldo</span><span className={`num ${cls(saldo)}`}>{fmt(saldo)}</span>
      </div>
      <div
        className="pbar" role="progressbar" aria-label={`Usado de ${nome}`} aria-valuemin={0} aria-valuemax={100}
        aria-valuenow={Math.min(100, percentualUsado)}
      >
        <i style={{ width: `${Math.min(100, Math.max(0, percentualUsado))}%` }} />
      </div>
      <small className="dim">{fmtPct(percentualUsado)} usado</small>
    </div>
  )
}

function LinhaRecebimento({ r, onSalvar, onRemover }: {
  r: PlrRecebimento; onSalvar: (id: number, r: PlrRecebimentoEntrada) => void; onRemover: (id: number) => void
}) {
  const [d, setD] = useState<PlrRecebimentoEntrada>({ data: r.data, descricao: r.descricao, valor: r.valor })
  useEffect(() => setD({ data: r.data, descricao: r.descricao, valor: r.valor }), [r.data, r.descricao, r.valor])
  // grava ao sair do campo, só se algo mudou e o que foi digitado é válido
  const gravar = (n: PlrRecebimentoEntrada) => {
    if (n.data && n.descricao.trim() && n.valor > 0 && (n.data !== r.data || n.descricao !== r.descricao || n.valor !== r.valor)) {
      onSalvar(r.id, { ...n, descricao: n.descricao.trim(), valor: centavos(n.valor) })
    }
  }
  return (
    <div className="plr-row rec">
      <input type="date" value={d.data} aria-label="Data do recebimento" onChange={(e) => setD({ ...d, data: e.target.value })} onBlur={() => gravar(d)} />
      <input type="text" className="desc" maxLength={120} value={d.descricao} aria-label="Descrição do recebimento"
        onChange={(e) => setD({ ...d, descricao: e.target.value })} onBlur={() => gravar(d)} />
      <NumberInput prefix="R$" min={0} step={0.01} value={d.valor} aria-label="Valor do recebimento"
        onChange={(v) => setD({ ...d, valor: v })} onBlur={() => gravar(d)} />
      <button className="btn x ghost" onClick={() => onRemover(r.id)} aria-label={`Remover ${r.descricao}`} title="Remover"><Icon name="close" size={14} /></button>
    </div>
  )
}

function LinhaGasto({ g, destinos, onSalvar, onRemover }: {
  g: PlrGasto; destinos: PlrDestinoControle[]; onSalvar: (id: number, g: PlrGastoEntrada) => void; onRemover: (id: number) => void
}) {
  const original: PlrGastoEntrada = { data: g.data, descricao: g.descricao, valor: g.valor, destinoId: g.destinoId }
  const [d, setD] = useState<PlrGastoEntrada>(original)
  useEffect(() => setD(original), [g.data, g.descricao, g.valor, g.destinoId])
  const gravar = (n: PlrGastoEntrada) => {
    const mudou = n.data !== g.data || n.descricao !== g.descricao || n.valor !== g.valor || n.destinoId !== g.destinoId
    if (mudou && n.data && n.descricao.trim() && n.valor > 0) onSalvar(g.id, { ...n, descricao: n.descricao.trim(), valor: centavos(n.valor) })
  }
  return (
    <div className="plr-row gas">
      <input type="date" value={d.data} aria-label="Data do gasto" onChange={(e) => setD({ ...d, data: e.target.value })} onBlur={() => gravar(d)} />
      <input type="text" className="desc" maxLength={160} value={d.descricao} aria-label="Descrição do gasto"
        onChange={(e) => setD({ ...d, descricao: e.target.value })} onBlur={() => gravar(d)} />
      <select value={d.destinoId ?? ''} aria-label="Destino do gasto"
        onChange={(e) => { const n = { ...d, destinoId: e.target.value === '' ? null : Number(e.target.value) }; setD(n); gravar(n) }}>
        <option value="">Sem destino</option>
        {destinos.map((x) => <option key={x.id} value={x.id}>{x.nome}</option>)}
      </select>
      <NumberInput prefix="R$" min={0} step={0.01} value={d.valor} aria-label="Valor do gasto"
        onChange={(v) => setD({ ...d, valor: v })} onBlur={() => gravar(d)} />
      <button className="btn x ghost" onClick={() => onRemover(g.id)} aria-label={`Remover ${g.descricao}`} title="Remover"><Icon name="close" size={14} /></button>
    </div>
  )
}

function NovoRecebimento({ onCriar }: { onCriar: Acoes['onCriarRecebimento'] }) {
  const [d, setD] = useState<PlrRecebimentoEntrada>({ data: hoje(), descricao: '', valor: 0 })
  const ok = d.data !== '' && d.descricao.trim() !== '' && d.valor > 0
  const enviar = () => {
    if (!ok) return
    onCriar({ ...d, descricao: d.descricao.trim(), valor: centavos(d.valor) })
    setD({ data: hoje(), descricao: '', valor: 0 })
  }
  return (
    <form className="plr-row rec novo" onSubmit={(e) => { e.preventDefault(); enviar() }}>
      <input type="date" value={d.data} aria-label="Data do novo recebimento" onChange={(e) => setD({ ...d, data: e.target.value })} />
      <input type="text" className="desc" maxLength={120} value={d.descricao} placeholder="Ex.: PLR 2026"
        aria-label="Descrição do novo recebimento" onChange={(e) => setD({ ...d, descricao: e.target.value })} />
      <NumberInput prefix="R$" min={0} step={0.01} value={d.valor} aria-label="Valor do novo recebimento" onChange={(v) => setD({ ...d, valor: v })} />
      <button type="submit" className="btn pri sm" disabled={!ok}>Adicionar</button>
    </form>
  )
}

function NovoGasto({ destinos, onCriar }: { destinos: PlrDestinoControle[]; onCriar: Acoes['onCriarGasto'] }) {
  const [d, setD] = useState<PlrGastoEntrada>({ data: hoje(), descricao: '', valor: 0, destinoId: null })
  const ok = d.data !== '' && d.descricao.trim() !== '' && d.valor > 0
  const enviar = () => {
    if (!ok) return
    onCriar({ ...d, descricao: d.descricao.trim(), valor: centavos(d.valor) })
    setD({ ...d, descricao: '', valor: 0 }) // mantém data e destino para lançar vários seguidos
  }
  return (
    <form className="plr-row gas novo" onSubmit={(e) => { e.preventDefault(); enviar() }}>
      <input type="date" value={d.data} aria-label="Data do novo gasto" onChange={(e) => setD({ ...d, data: e.target.value })} />
      <input type="text" className="desc" maxLength={160} value={d.descricao} placeholder="Ex.: Tênis"
        aria-label="Descrição do novo gasto" onChange={(e) => setD({ ...d, descricao: e.target.value })} />
      <select value={d.destinoId ?? ''} aria-label="Destino do novo gasto"
        onChange={(e) => setD({ ...d, destinoId: e.target.value === '' ? null : Number(e.target.value) })}>
        <option value="">Sem destino</option>
        {destinos.map((x) => <option key={x.id} value={x.id}>{x.nome}</option>)}
      </select>
      <NumberInput prefix="R$" min={0} step={0.01} value={d.valor} aria-label="Valor do novo gasto" onChange={(v) => setD({ ...d, valor: v })} />
      <button type="submit" className="btn pri sm" disabled={!ok}>Adicionar</button>
    </form>
  )
}

/** Seção "Controle do PLR recebido": só exibe e envia; os números vêm do back end. */
export function ControlePlrView({ controle, ...acoes }: { controle: PlrControle } & Acoes) {
  const { destinos, semDestino } = controle
  const semDestinos = destinos.length === 0
  return (
    <Card className="plr-controle">
      <h2>Controle do PLR recebido</h2>
      <p className="sub plr-nota">Este controle registra o dinheiro que você já recebeu e não altera a projeção.</p>

      <div className="plr-kpis">
        <div><small>Recebido</small><strong className="num">{fmt(controle.recebido)}</strong></div>
        <div><small>Gasto</small><strong className="num">{fmt(controle.gasto)}</strong></div>
        <div><small>Saldo</small><strong className={`num ${cls(controle.saldo)}`}>{fmt(controle.saldo)}</strong></div>
      </div>

      {semDestinos && (
        <div className="plr-sug">
          <span>Divida o que recebeu em envelopes (os destinos do plano de uso do PLR). Quer começar com 80% para gastos e 20% para coisas pessoais?</span>
          <button type="button" className="btn pri sm" onClick={acoes.onUsarSugestao}>Usar sugestão 80/20</button>
        </div>
      )}

      <div className="plr-envs">
        {destinos.map((d) => <Envelope key={d.id} {...d} />)}
        {(!semDestinos || semDestino.gasto > 0 || semDestino.alocado > 0) && <Envelope nome="Sem destino" {...semDestino} />}
      </div>

      <h3 className="plr-h3">Recebimentos <small>{controle.recebimentos.length === 1 ? '1 item' : `${controle.recebimentos.length} itens`}</small></h3>
      <div className="plr-lista">
        <div className="plr-row rec plr-head" aria-hidden="true"><span>Data</span><span>Descrição</span><span className="r">Valor</span><span /></div>
        {controle.recebimentos.map((r) => <LinhaRecebimento key={r.id} r={r} onSalvar={acoes.onSalvarRecebimento} onRemover={acoes.onRemoverRecebimento} />)}
        {controle.recebimentos.length === 0 && <p className="empty">Nenhum recebimento. Registre abaixo o PLR que caiu na conta.</p>}
        <NovoRecebimento onCriar={acoes.onCriarRecebimento} />
      </div>

      <h3 className="plr-h3">Gastos do PLR <small>{controle.gastos.length === 1 ? '1 item' : `${controle.gastos.length} itens`}</small></h3>
      <div className="plr-lista">
        <div className="plr-row gas plr-head" aria-hidden="true"><span>Data</span><span>Descrição</span><span>Destino</span><span className="r">Valor</span><span /></div>
        {controle.gastos.map((g) => <LinhaGasto key={g.id} g={g} destinos={destinos} onSalvar={acoes.onSalvarGasto} onRemover={acoes.onRemoverGasto} />)}
        {controle.gastos.length === 0 && <p className="empty">Nenhum gasto registrado.</p>}
        <NovoGasto destinos={destinos} onCriar={acoes.onCriarGasto} />
      </div>
    </Card>
  )
}

// ---- página -----------------------------------------------------------------------------------

export function Plr() {
  const { dados, alternarPlr, editarPlr, salvarDestinos } = useApp()
  const { plr } = dados
  const planoPlr = dados.adicionais.plr
  const [controle, setControle] = useState<PlrControle | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [editando, setEditando] = useState(false)
  const fecharPlano = useCallback(() => setEditando(false), [])

  const recarregar = useCallback(async () => {
    try {
      setControle(await api.plrControle())
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível carregar o controle do PLR.')
    }
  }, [])

  // o controle depende dos destinos (nome, percentual, ids): recarrega quando o plano de uso do PLR muda
  const chaveDestinos = useMemo(
    () => planoPlr.destinos.map((d) => `${d.id}:${d.nome}:${d.percentual}`).join('|'),
    [planoPlr.destinos],
  )
  useEffect(() => { void recarregar() }, [recarregar, chaveDestinos])

  /** Grava e recarrega o resumo; 400/422 viram aviso na própria seção. */
  const gravar = useCallback(async (op: () => Promise<unknown>) => {
    try {
      await op()
      setErro(null)
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível salvar.')
    }
    await recarregar()
  }, [recarregar])

  const editarParcelaPlr = (i: number, patch: Partial<Pick<ParcelaPlr, 'mes' | 'percentual' | 'valor'>>) =>
    editarPlr(plr.parcelas.map((e, k) => (k === i ? { ...e, ...patch } : e)))

  return (
    <Page title="PLR" subtitle="A estimativa que entra na projeção, como você pretende usá-lo e o controle do dinheiro que já recebeu.">
      <div className="plr-pg">
        <Card>
          <h2>PLR na projeção</h2>
          <Switch title="Contar com o PLR na projeção" checked={plr.ativo} onChange={(v) => void alternarPlr(v)} />
          {plr.ativo && (
            <>
              <div style={{ marginTop: 4 }}><AvisoPlr /></div>
              <div className="scrollx">
                <table style={{ marginTop: 10 }}>
                  <thead>
                    <tr><th>Mês</th><th>% da estimativa</th><th>Valor (R$)</th><th>Bruto</th><th>Desconto</th><th>Líquido</th></tr>
                  </thead>
                  <tbody>
                    {plr.parcelas.map((e, i) => (
                      <tr key={i}>
                        <td><select value={e.mes} aria-label="Mês" onChange={(ev) => editarParcelaPlr(i, { mes: Number(ev.target.value) })}>{OPCOES_MES}</select></td>
                        <td><NumberInput min={1} max={100} value={e.percentual} style={{ width: 80 }} aria-label="Percentual" onChange={(v) => editarParcelaPlr(i, { percentual: v })} /></td>
                        <td>
                          <input
                            type="number"
                            min={0}
                            step={0.01}
                            className="plr-valor"
                            aria-label="Valor (R$)"
                            placeholder={String(e.valorBruto)}
                            value={e.valor ?? ''}
                            onChange={(ev) => editarParcelaPlr(i, { valor: ev.target.value === '' ? null : Number(ev.target.value) })}
                          />
                        </td>
                        <td>{fmt(e.valorBruto)}</td>
                        <td>{fmt(e.desconto)}</td>
                        <td>{fmt(e.valorLiquido)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="plr-rodape">
                <span>Estimativa anual do cargo: {fmt(plr.estimativaAnual)} (bruto). Valor vazio usa o calculado.</span>
                {plr.aviso ? <span className="warn">{plr.aviso}</span> : <span className="ok">✓ {plr.totalPercentual}% distribuído.</span>}
              </div>
            </>
          )}
          <PlanoUsoResumo plano={planoPlr} onEditar={() => setEditando(true)} />
          <p className="dim plr-hint">
            Os destinos do plano de uso são os envelopes do controle abaixo, mesmo com o PLR fora da projeção.
          </p>
        </Card>

        {erro && <div className="warn" role="alert">⚠️ {erro}</div>}
        {controle && (
          <ControlePlrView
            controle={controle}
            onCriarRecebimento={(r) => void gravar(() => api.criarPlrRecebimento(r))}
            onSalvarRecebimento={(id, r) => void gravar(() => api.salvarPlrRecebimento(id, r))}
            onRemoverRecebimento={(id) => void gravar(() => api.removerPlrRecebimento(id))}
            onCriarGasto={(g) => void gravar(() => api.criarPlrGasto(g))}
            onSalvarGasto={(id, g) => void gravar(() => api.salvarPlrGasto(id, g))}
            onRemoverGasto={(id) => void gravar(() => api.removerPlrGasto(id))}
            onUsarSugestao={() => salvarDestinos('plr', SUGESTAO_80_20)}
          />
        )}
      </div>
      <PlanoUsoDrawer evento={editando ? 'plr' : null} onClose={fecharPlano} />
    </Page>
  )
}
