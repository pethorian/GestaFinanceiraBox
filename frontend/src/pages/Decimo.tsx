import { useCallback, useEffect, useState } from 'react'
import { PlanoUsoEditor, PlanoUsoResumo, type RascunhoDestino } from '../components/adicionais'
import { AvisoPlr } from '../components/AvisoPlr'
import { Icon } from '../components/Icon'
import { Card, Field, NumberInput, Page, Switch } from '../components/ui'
import { fmt, MES_LONGO } from '../domain/format'
import { NOME_EVENTO } from '../domain/adicionais'
import type { ChaveAdicional, ParcelaPlr, PlanoAdicional } from '../domain/types'
import { useApp } from '../store/AppContext'

// meses 1..12, como no back end
const OPCOES_MES = MES_LONGO.map((n, i) => <option key={n} value={i + 1}>{n}</option>)

let seqChave = 0
const novaChave = () => `n${++seqChave}`
const paraRascunho = (p: PlanoAdicional): RascunhoDestino[] =>
  p.destinos.map(({ nome, tipo, percentual, metaId }) => ({ chave: novaChave(), nome, tipo, percentual, metaId }))

/**
 * Painel lateral com o editor do plano de uso de um evento. O rascunho (o que se digita) fica aqui;
 * cada alteração grava a lista inteira com atraso, e a divisão/prévia exibidas são as que o servidor devolveu.
 */
function PlanoUsoDrawer({ evento, onClose }: { evento: ChaveAdicional | null; onClose: () => void }) {
  const { dados, salvarDestinos, errosAdicionais } = useApp()
  const plano = evento ? dados.adicionais[evento] : null
  const [rascunho, setRascunho] = useState<RascunhoDestino[]>([])

  // ao abrir, começa do que está salvo
  useEffect(() => {
    if (evento) setRascunho(paraRascunho(dados.adicionais[evento]))
  }, [evento])
  useEffect(() => {
    if (!evento) return
    const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', h)
    return () => document.removeEventListener('keydown', h)
  }, [evento, onClose])

  const mudar = (lista: RascunhoDestino[]) => {
    setRascunho(lista)
    if (evento) salvarDestinos(evento, lista)
  }
  // a linha nova só é gravada quando o usuário a preenche (nome vazio seria recusado pelo servidor)
  const adicionar = () => setRascunho((l) => [...l, {
    chave: novaChave(), nome: '', tipo: 'LAZER', percentual: plano && plano.semDestinoPercentual > 0 ? plano.semDestinoPercentual : 0, metaId: null,
  }])

  return (
    <aside id="drawer" className={plano ? 'open' : ''} aria-hidden={!plano} aria-label="Plano de uso">
      {plano && evento && (
        <>
          <button className="btn x" style={{ float: 'right' }} onClick={onClose} aria-label="Fechar"><Icon name="close" size={16} /></button>
          <h2 style={{ fontSize: 18, margin: 0 }}>Plano de uso do {NOME_EVENTO[plano.evento]}</h2>
          <small style={{ color: 'var(--mut)', display: 'block', marginBottom: 12 }}>
            Divida o líquido de cada parcela. O que ficar sem destino continua no saldo disponível.
          </small>
          <PlanoUsoEditor
            plano={plano} rascunho={rascunho} metas={dados.metas} erros={errosAdicionais[evento]}
            onChange={mudar} onAdicionar={adicionar}
          />
        </>
      )}
    </aside>
  )
}

export function Decimo() {
  const {
    dados, parametros, alternarDecimo, editarFerias,
    alternarPlr, editarPlr, editarBonus,
  } = useApp()
  const { decimo, ferias, plr, bonus } = dados
  const [tentouBonus, setTentouBonus] = useState(false)
  const [editando, setEditando] = useState<ChaveAdicional | null>(null)
  const fecharPlano = useCallback(() => setEditando(null), [])
  // o plano só é editável com o evento ativo; desligar o evento fecha o painel
  const planoAberto = editando === 'decimo' ? (decimo.ativo ? editando : null) : editando === 'plr' ? (plr.ativo ? editando : null) : null
  const cargo = parametros.cargos.find((c) => c.id === dados.perfil.cargoId)
  const bonusIndisponivel = cargo != null && cargo.jornada !== 8
  const maxSalariosBonus = parametros.beneficios.bonusCaixaMaxSalarios

  const alternarBonus = (v: boolean) => {
    // a regra de quem recebe é do back end; aqui só se avisa antes de tentar
    if (v && bonusIndisponivel) { setTentouBonus(true); return }
    setTentouBonus(false)
    editarBonus({ ativo: v })
  }
  const editarParcelaPlr = (i: number, patch: Partial<Pick<ParcelaPlr, 'mes' | 'percentual' | 'valor'>>) =>
    editarPlr(plr.parcelas.map((e, k) => (k === i ? { ...e, ...patch } : e)))

  return (
    <Page title="13º, férias, PLR e bônus" subtitle="Escolha o que entra na projeção. INSS e IR do 13º são tratados à parte.">
      <div className="dgrid">
        <Card>
          <h2>13º salário</h2>
          <Switch title="Contar com o 13º na projeção" checked={decimo.ativo} onChange={(v) => void alternarDecimo(v)} />
          {decimo.ativo && (
            <p className="sub" style={{ margin: '10px 0 0' }}>
              {decimo.parcelas.map((p) => `${MES_LONGO[p.mes - 1]} ${p.percentual}%`).join(' · ')}
            </p>
          )}
          {decimo.ativo && <PlanoUsoResumo plano={dados.adicionais.decimo} onEditar={() => setEditando('decimo')} />}
        </Card>

        <Card>
          <h2>Férias</h2>
          <Switch title="Tirar férias no período" hint="Adiciona o terço constitucional" checked={ferias.ativo} onChange={(v) => editarFerias({ ativo: v })} />
          {ferias.ativo && (
            <>
              <div style={{ marginTop: 12 }}>
                <Field label="Mês das férias">
                  <select value={ferias.mes} onChange={(e) => editarFerias({ mes: Number(e.target.value) })}>{OPCOES_MES}</select>
                </Field>
              </div>
              <Switch title="Vender 10 dias (abono)" hint="Recebe mais 1/3 em dinheiro" checked={ferias.abono} onChange={(v) => editarFerias({ abono: v })} />
            </>
          )}
        </Card>

        <Card>
          <h2>Bônus Caixa</h2>
          <Switch title="Receber bônus" hint="Pago em um mês, em quantidade de salários" checked={bonus.ativo && !bonusIndisponivel} onChange={alternarBonus} />
          {bonusIndisponivel && tentouBonus && (
            <div className="warn">Você não pode receber o Bônus Caixa: seu cargo cadastrado não é de 8 horas. Altere o cargo em Meu perfil se isso estiver errado.</div>
          )}
          {bonus.ativo && !bonusIndisponivel && (
            <div className="row2" style={{ marginTop: 12 }}>
              <Field label="Mês do bônus">
                <select value={bonus.mes} onChange={(e) => editarBonus({ mes: Number(e.target.value) })}>{OPCOES_MES}</select>
              </Field>
              <Field label="Quantidade de salários" hint={`De 0 a ${maxSalariosBonus}`}>
                <NumberInput min={0} max={maxSalariosBonus} step={0.5} value={bonus.salarios} onChange={(v) => editarBonus({ salarios: v })} />
              </Field>
            </div>
          )}
        </Card>

        <Card>
          <h2>PLR</h2>
          <Switch title="Contar com o PLR na projeção" checked={plr.ativo} onChange={(v) => void alternarPlr(v)} />
                    {plr.ativo && (
            <>
              <div style={{ marginTop: 4 }}><AvisoPlr /></div>
              {/* parcelas à esquerda, plano de uso à direita: o card não cresce na vertical */}
              <div className="plr-corpo">
              <div>
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
              <div className="plr-rodape">
                <span>Estimativa anual do cargo: {fmt(plr.estimativaAnual)} (líquido). Valor vazio usa o calculado.</span>
                {plr.aviso ? <span className="warn">{plr.aviso}</span> : <span className="ok">✓ {plr.totalPercentual}% distribuído.</span>}
              </div>
              </div>
              <PlanoUsoResumo plano={dados.adicionais.plr} onEditar={() => setEditando('plr')} />
              </div>
            </>
          )}
        </Card>

      </div>
      <PlanoUsoDrawer evento={planoAberto} onClose={fecharPlano} />
    </Page>
  )
}
