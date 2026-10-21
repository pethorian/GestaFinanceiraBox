import { Card, Field, NumberInput, Page, Segmented, Switch } from '../components/ui'
import { fmt, fmtPct } from '../domain/format'
import { useApp } from '../store/AppContext'

function Linha({ a, b, forte }: { a: string; b: string; forte?: boolean }) {
  return <div className="l"><span>{forte ? <b>{a}</b> : a}</span><span className="num">{forte ? <b>{b}</b> : b}</span></div>
}

/** Prévia de um mês comum: valores calculados pelo back end (GET /api/projecao/mes-comum). */
function Previa() {
  const { mesComum: m } = useApp()
  return (
    <Card className="pv" aria-label="Prévia de um mês comum">
      <h2>Prévia de um mês comum</h2>
      <small style={{ color: 'var(--mut)' }}>Líquido estimado</small>
      <div className="big num pos">{fmt(m.liquido)}</div>
      <Linha a="Remuneração base" b={fmt(m.base)} />
      {m.sobreaviso > 0 && <Linha a="Sobreaviso" b={fmt(m.sobreaviso)} />}
      <Linha a="Benefícios" b={fmt(m.creche + m.homeOffice + m.vrVa)} />
      <Linha a="Receitas" b={fmt(m.receitas)} forte />
      <div style={{ height: 8 }} />
      <Linha a="INSS" b={`−${fmt(m.inss)}`} />
      <Linha a="IRPF" b={`−${fmt(m.irpf.imposto)}`} />
      <Linha a="Saúde Caixa" b={`−${fmt(m.saude)}`} />
      <Linha a="FUNCEF" b={`−${fmt(m.funcef)}`} />
      <Linha a="VT + sindicato" b={`−${fmt(m.valeTransporte + m.sindicato)}`} />
      <Linha a="Descontos" b={`−${fmt(m.descontos)}`} forte />
      <div className="l t"><span>Líquido</span><span className="num">{fmt(m.liquido)}</span></div>
      <small style={{ color: 'var(--mut)' }}>Cálculo feito pelo servidor com as tabelas vigentes.</small>
    </Card>
  )
}

export function Perfil() {
  const { dados, parametros, editarPerfil } = useApp()
  const p = dados.perfil
  const cargo = parametros.cargos.find((c) => c.id === p.cargoId)

  return (
    <Page title="Meu perfil" subtitle="Informe seu cargo, benefícios e descontos. A projeção se atualiza sozinha.">
      <div className="cols">
        <div className="stack">
          <Card>
            <h2>Identificação</h2>
            <Field label="Seu nome" hint="Opcional. Aparece no título da projeção.">
              <input type="text" maxLength={60} placeholder="Como devemos te chamar?" value={p.nome ?? ''}
                onChange={(e) => editarPerfil({ nome: e.target.value })} />
            </Field>
          </Card>

          <Card>
            <h2>Cargo e remuneração</h2>
            <div className="row2">
              <Field label="Cargo / função">
                <select value={p.cargoId} onChange={(e) => editarPerfil({ cargoId: Number(e.target.value) })}>
                  {parametros.cargos.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                </select>
              </Field>
              {cargo?.usaDelta && (
                <Field label="Referência salarial (Delta)" hint="Base = maior entre o piso do cargo e (delta + gratificação)">
                  <select value={p.delta} onChange={(e) => editarPerfil({ delta: Number(e.target.value) })}>
                    {parametros.deltas.deltas.map((d) => <option key={d.referencia} value={d.referencia}>Delta {d.referencia} · {fmt(d.valor)}</option>)}
                  </select>
                </Field>
              )}
            </div>
            <div className="row2">
              <Field label="Jornada"><input type="text" value={cargo ? `${cargo.jornada} horas` : ''} disabled /></Field>
              <Field label="Dias de sobreaviso por mês" hint="Calculado sobre a base, com divisor da jornada">
                <NumberInput min={0} max={31} value={p.diasSobreaviso} onChange={(v) => editarPerfil({ diasSobreaviso: v })} />
              </Field>
            </div>
          </Card>

          <Card>
            <h2>Benefícios</h2>
            <Field label="Filhos com auxílio creche (PAI)">
              <NumberInput min={0} max={10} value={p.filhos} onChange={(v) => editarPerfil({ filhos: v })} />
            </Field>
            <Switch title="Auxílio home office" hint="Valor fixo mensal" checked={p.homeOffice} onChange={(v) => editarPerfil({ homeOffice: v })} />
            <Switch title="Vale-refeição + cesta-alimentação" hint="Considerar VR e VA na receita" checked={p.vrVa} onChange={(v) => editarPerfil({ vrVa: v })} />
          </Card>

          <Card>
            <h2>Descontos</h2>
            <div className="tg"><div><b>Imposto de Renda (IRPF)</b><small>Calculado pela tabela vigente</small></div><span className="badge-req">Obrigatório</span></div>
            <div className="tg"><div><b>Previdência social (INSS)</b><small>Faixas progressivas com teto</small></div><span className="badge-req">Obrigatório</span></div>

            <div style={{ padding: '12px 0', borderBottom: '1px solid var(--line)' }}>
              <b>Plano de saúde (Saúde Caixa)</b>
              <div style={{ margin: '8px 0' }}>
                <Segmented name="saude" value={p.saude} onChange={(v) => editarPerfil({ saude: v })} options={[
                  { value: 'NAO', label: 'Não participo' }, { value: 'SOZINHO', label: 'Sozinho' }, { value: 'DEP', label: 'Com dependentes' },
                ]} />
              </div>
              {p.saude === 'DEP' && (
                <Field label="Quantidade de dependentes" hint={`Limitado a ${fmtPct(parametros.beneficios.saudeTetoPercentual * 100)} da base`}>
                  <NumberInput min={1} max={10} value={p.dependentes} onChange={(v) => editarPerfil({ dependentes: v })} />
                </Field>
              )}
            </div>

            <div style={{ padding: '12px 0', borderBottom: '1px solid var(--line)' }}>
              <b>Previdência complementar (FUNCEF)</b>
              <Switch title="Participo da FUNCEF" hint="Sem FUNCEF, nenhuma contribuição é descontada" checked={p.funcefPct > 0}
                onChange={(v) => editarPerfil({ funcefPct: v ? (p.funcefPct >= 5 ? p.funcefPct : 5) : 0 })} />
              {p.funcefPct > 0 && (
                <div className="range" style={{ marginTop: 6 }}>
                  <input type="range" min={5} max={12} step={1} value={p.funcefPct} aria-label="Percentual FUNCEF" onChange={(e) => editarPerfil({ funcefPct: Number(e.target.value) })} />
                  <output>{fmtPct(p.funcefPct)}</output>
                </div>
              )}
              <small style={{ color: 'var(--mut)' }}>Contribuição permitida: de 5% a 12% sobre a remuneração base (ou não participar). Também entra na dedução do IR.</small>
            </div>

            <Switch title="Vale-transporte" hint={`${fmtPct(parametros.beneficios.vtPercentual * 100)} do valor do delta escolhido`} checked={p.valeTransporte} onChange={(v) => editarPerfil({ valeTransporte: v })} />
            <Switch title="Sindicato" hint={`${fmtPct(parametros.beneficios.sindicatoPercentual * 100)} da base + sobreaviso, até o teto`} checked={p.sindicato} onChange={(v) => editarPerfil({ sindicato: v })} />
          </Card>

          <Card>
            <h2>Ponto de partida</h2>
            <div style={{ maxWidth: 260 }}>
              <Field label="Saldo atual (R$)" hint="Quanto você tem hoje. Inicia o saldo acumulado.">
                <NumberInput step={100} value={p.saldoInicial} onChange={(v) => editarPerfil({ saldoInicial: v })} />
              </Field>
            </div>
          </Card>
        </div>
        <Previa />
      </div>
    </Page>
  )
}
