import { useCallback, useState } from 'react'
import { PlanoUsoResumo } from '../components/adicionais'
import { PlanoUsoDrawer } from '../components/PlanoUsoDrawer'
import { Card, Field, NumberInput, Page, Switch } from '../components/ui'
import { MES_LONGO } from '../domain/format'
import { useApp } from '../store/AppContext'

// meses 1..12, como no back end
const OPCOES_MES = MES_LONGO.map((n, i) => <option key={n} value={i + 1}>{n}</option>)

export function Decimo() {
  const {
    dados, parametros, alternarDecimo, editarFerias,
    editarBonus,
  } = useApp()
  const { decimo, ferias, bonus } = dados
  const [tentouBonus, setTentouBonus] = useState(false)
  const [editando, setEditando] = useState<'decimo' | null>(null)
  const fecharPlano = useCallback(() => setEditando(null), [])
  // o plano só é editável com o 13º ativo; desligar o 13º fecha o painel
  const planoAberto = decimo.ativo ? editando : null
  const cargo = parametros.cargos.find((c) => c.id === dados.perfil.cargoId)
  const bonusIndisponivel = cargo != null && cargo.jornada !== 8
  const maxSalariosBonus = parametros.beneficios.bonusCaixaMaxSalarios

  const alternarBonus = (v: boolean) => {
    // a regra de quem recebe é do back end; aqui só se avisa antes de tentar
    if (v && bonusIndisponivel) { setTentouBonus(true); return }
    setTentouBonus(false)
    editarBonus({ ativo: v })
  }
  return (
    <Page title="13º, férias e bônus" subtitle="Escolha o que entra na projeção. INSS e IR do 13º são tratados à parte. O PLR tem aba própria.">
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

      </div>
      <PlanoUsoDrawer evento={planoAberto} onClose={fecharPlano} />
    </Page>
  )
}
