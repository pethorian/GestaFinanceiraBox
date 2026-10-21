import { AvisoPlr } from '../components/AvisoPlr'
import { Icon } from '../components/Icon'
import { Card, Field, NumberInput, Page, Switch } from '../components/ui'
import { fmt, MES_LONGO } from '../domain/format'
import type { ParcelaPlr } from '../domain/types'
import { useApp } from '../store/AppContext'

// meses 1..12, como no back end
const OPCOES_MES = MES_LONGO.map((n, i) => <option key={n} value={i + 1}>{n}</option>)

export function Decimo() {
  const {
    dados, parametros, editarDecimo, adicionarParcelaDecimo, editarFerias,
    alternarPlr, editarPlr, adicionarParcelaPlr, editarBonus,
  } = useApp()
  const { decimo, ferias, plr, bonus } = dados
  const { parcelas } = decimo
  const cargo = parametros.cargos.find((c) => c.id === dados.perfil.cargoId)
  const bonusIndisponivel = cargo?.jornada === 6
  const maxSalariosBonus = parametros.beneficios.bonusCaixaMaxSalarios

  const editar = (i: number, patch: Partial<{ mes: number; percentual: number }>) =>
    editarDecimo(parcelas.map((e, k) => (k === i ? { ...e, ...patch } : e)))
  const editarParcelaPlr = (i: number, patch: Partial<Pick<ParcelaPlr, 'mes' | 'percentual' | 'valor'>>) =>
    editarPlr(plr.parcelas.map((e, k) => (k === i ? { ...e, ...patch } : e)))

  return (
    <Page title="13º, férias, PLR e bônus" subtitle="Defina quando cada parcela cai. INSS e IR do 13º são tratados à parte.">
      <div className="dgrid">
        <Card>
          <h2>13º salário</h2>
          <table>
            <thead><tr><th>Mês</th><th>% do 13º</th><th /></tr></thead>
            <tbody>
              {parcelas.map((e, i) => (
                <tr key={i}>
                  <td><select value={e.mes} aria-label="Mês" onChange={(ev) => editar(i, { mes: Number(ev.target.value) })}>{OPCOES_MES}</select></td>
                  <td><NumberInput min={1} max={100} value={e.percentual} style={{ width: 90 }} aria-label="Percentual" onChange={(v) => editar(i, { percentual: v })} /></td>
                  <td><button className="btn x" aria-label="Remover parcela" onClick={() => editarDecimo(parcelas.filter((_, k) => k !== i))}><Icon name="close" size={14} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 10 }}>
            <button className="btn" onClick={() => void adicionarParcelaDecimo()}>+ Parcela</button>
          </div>
          {/* soma e aviso são validados e devolvidos pelo back end */}
          {decimo.aviso ? <div className="warn">{decimo.aviso}</div> : <div className="ok">✓ {decimo.totalPercentual}% distribuído.</div>}
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
          {bonusIndisponivel && <p className="sub">Disponível apenas para carreiras de 8 horas</p>}
          <fieldset disabled={bonusIndisponivel} style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}>
            <Switch title="Receber bônus" hint="Pago em um mês, em quantidade de salários" checked={bonus.ativo && !bonusIndisponivel} onChange={(v) => editarBonus({ ativo: v })} />
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
          </fieldset>
        </Card>

        <Card>
          <h2>PLR</h2>
          <Switch title="Contar com o PLR na projeção" checked={plr.ativo} onChange={(v) => void alternarPlr(v)} />
                    {plr.ativo && (
            <>
              <div style={{ marginTop: 4 }}><AvisoPlr /></div>
              <table style={{ marginTop: 10 }}>
                <thead>
                  <tr><th>Mês</th><th>% da estimativa</th><th>Valor (R$)</th><th>Bruto</th><th>Desconto</th><th>Líquido</th><th /></tr>
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
                      <td><button className="btn x" aria-label="Remover parcela" onClick={() => editarPlr(plr.parcelas.filter((_, k) => k !== i))}><Icon name="close" size={14} /></button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="plr-rodape">
                <button className="btn" onClick={() => void adicionarParcelaPlr()}>+ Parcela</button>
                <span className="sub">Estimativa anual do cargo: {fmt(plr.estimativaAnual)} (líquido). Valor vazio usa o calculado.</span>
                {plr.aviso ? <span className="warn" style={{ margin: 0 }}>{plr.aviso}</span> : <span className="ok" style={{ margin: 0 }}>✓ {plr.totalPercentual}% distribuído.</span>}
              </div>
            </>
          )}
        </Card>

      </div>
    </Page>
  )
}
