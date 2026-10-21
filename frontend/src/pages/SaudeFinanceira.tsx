import { Icon, type IconName } from '../components/Icon'
import { DivisaoRendaChart, MetaAtualChart, StatusPill } from '../components/saudeCharts'
import { Card, Page, Segmented, Switch, cls } from '../components/ui'
import { fmt } from '../domain/format'
import { FAIXAS, GRUPOS, fmtPctFaixa } from '../domain/saude'
import type { GrupoGasto, SaudeFinanceira as Saude } from '../domain/types'
import { useApp } from '../store/AppContext'

const TITULO_SWITCH = 'Quero seguir o método 50/30/20'

function Kpi({ icon, cor, titulo, valor, nota, classe = '' }: {
  icon: IconName; cor: string; titulo: string; valor: string; nota: string; classe?: string
}) {
  return (
    <Card className="kpi">
      <div className="ic" style={{ background: `color-mix(in srgb, ${cor} 14%, transparent)`, color: cor }}><Icon name={icon} /></div>
      <div><small>{titulo}</small><strong className={`num ${classe}`}>{valor}</strong><small className="nt">{nota}</small></div>
    </Card>
  )
}

/** Explicação curta do método + adesão. */
function Metodo({ saude, onAlternar }: { saude: Saude; onAlternar: (v: boolean) => void }) {
  return (
    <Card className="sf-intro">
      <div className="sf-intro-txt">
        <h2><Icon name="pulse" size={16} />Método 50/30/20</h2>
        {saude.participa ? (
          <p className="sf-chips">
            {FAIXAS.map((f) => <span key={f.faixa}><i className="sf-sw" style={{ background: f.cor }} /><b>{f.meta}%</b> {f.nome.toLowerCase()}</span>)}
          </p>
        ) : (
          <p>
            Divida a renda líquida do mês em <b>até 50%</b> para necessidades, <b>até 30%</b> para desejos e
            <b> pelo menos 20%</b> para poupança.
          </p>
        )}
      </div>
      <Switch
        checked={saude.participa} onChange={onAlternar} title={TITULO_SWITCH}
        hint={saude.participa ? 'Você acompanha suas faixas aqui.' : 'Veja como ficaria com seus números.'}
      />
    </Card>
  )
}

/** Sem adesão: o que é cada faixa, uma prévia com os números do usuário e o convite. */
function Convite({ saude, onAlternar }: { saude: Saude; onAlternar: (v: boolean) => void }) {
  return (
    <div className="sf-convite">
      <Card className="col sf-faixas">
        <h2>Como funciona</h2>
        <ul>
          {FAIXAS.map((f) => (
            <li key={f.faixa}>
              <i className="sf-sw lg" style={{ background: f.cor }} />
              <div><b>{f.meta}% · {f.nome}</b><small>{f.descricao}</small></div>
            </li>
          ))}
        </ul>
        <p className="dim sf-nota">A renda de referência é o seu líquido de um mês comum (sem 13º, férias ou PLR).</p>
      </Card>
      <Card className="col sf-previa">
        <h2>Prévia com os seus números <small>renda de referência {fmt(saude.rendaMensal)}</small></h2>
        <DivisaoRendaChart saude={saude} compacto />
        <div className="sf-mini">
          {FAIXAS.map((f) => (
            <div key={f.faixa}>
              <small><i className="sf-sw" style={{ background: f.cor }} />{f.nome}</small>
              <b className="num">{fmtPctFaixa(saude.percentuais?.[f.faixa])}</b>
              <StatusPill faixa={f.faixa} saude={saude} />
            </div>
          ))}
        </div>
        <div className="sf-cta">
          <p>Ao aderir, você vê o diagnóstico completo, o aporte sugerido e classifica suas categorias.</p>
          <button className="btn pri" onClick={() => onAlternar(true)}>{TITULO_SWITCH}</button>
        </div>
      </Card>
    </div>
  )
}

function Completa({ saude, onGrupo }: { saude: Saude; onGrupo: (nome: string, grupo: GrupoGasto) => void }) {
  const falta = saude.aporteSugerido > 0
  return (
    <>
      <div className="sf-kpis">
        <Kpi icon="wallet" cor="var(--cyan)" titulo="Renda de referência" valor={fmt(saude.rendaMensal)} nota="líquido de um mês comum" />
        <Kpi
          icon="card" cor={saude.sobraLivre < 0 ? 'var(--red)' : 'var(--green)'} titulo="Sobra livre"
          valor={fmt(saude.sobraLivre)} classe={cls(saude.sobraLivre)}
          nota={saude.sobraLivre < 0 ? 'gastos passam da renda' : 'sem destino no mês'}
        />
        <Kpi
          icon="piggy" cor="var(--purple)" titulo="Aporte sugerido" valor={`${fmt(saude.aporteSugerido)}/mês`}
          nota={falta ? 'para chegar aos 20%' : 'já guarda 20% ou mais'}
        />
      </div>

      <div className="sf-main">
        <Card className="col sf-graf">
          <h2>Recomendado × atual <small>% da renda em cada faixa</small></h2>
          <MetaAtualChart saude={saude} />
          <h2 className="sf-h2b">Como sua renda se divide <small>passe o mouse ou toque para ver os valores</small></h2>
          <DivisaoRendaChart saude={saude} />
        </Card>

        <div className="sf-mid">
          <Card className="col sf-diag">
            <h2>Diagnóstico</h2>
            <ul>
              {saude.diagnostico.map((d, i) => <li key={i}><Icon name="info" size={15} />{d}</li>)}
            </ul>
          </Card>
          <Card className="col sf-poup">
            <h2><i className="sf-sw" style={{ background: 'var(--f-pou)' }} />Poupança</h2>
            <p>
              {falta
                ? <>Use a aba <b>Metas e reserva</b> para guardar <b className="num">{fmt(saude.aporteSugerido)}</b> por mês e chegar aos 20%.</>
                : <>Você já guarda pelo menos 20% da renda. Ajuste quando quiser em <b>Metas e reserva</b>.</>}
            </p>
            {saude.poupancaDetalhe.funcef > 0 && (
              <p className="sf-split">
                <span>Metas <b className="num">{fmt(saude.poupancaDetalhe.metas)}</b></span>
                <span>+ FUNCEF <b className="num">{fmt(saude.poupancaDetalhe.funcef)}</b></span>
                <span>= <b className="num">{fmt(saude.atual.poupanca)}</b>/mês</span>
              </p>
            )}
            <a className="btn pri sm" href="#/metas">Criar ou ajustar reserva<Icon name="arrow" size={14} /></a>
          </Card>
        </div>

        <Card className="col sf-cats">
          <h2>Classifique suas categorias</h2>
          <div className="sf-catlist">
            {saude.categorias.map((c) => (
              <div key={c.nome} className="sf-cat">
                <div>
                  <b>{c.nome}</b>
                  <small className="num">{c.mediaMensal > 0 ? fmt(c.mediaMensal) : 'sem gastos'}</small>
                </div>
                <Segmented
                  name={`grupo-${c.nome}`} value={c.grupo} options={GRUPOS} label={`Classificação de ${c.nome}`}
                  onChange={(g) => onGrupo(c.nome, g)}
                />
              </div>
            ))}
          </div>
          <p className="dim sf-nota">Média mensal. Obrigatório → necessidades; lazer → desejos.</p>
        </Card>
      </div>
    </>
  )
}

/** Tela sem estado próprio: tudo chega por props (facilita testar). */
export function SaudeFinanceiraView({ saude, onAlternar, onGrupo }: {
  saude: Saude; onAlternar: (v: boolean) => void; onGrupo: (nome: string, grupo: GrupoGasto) => void
}) {
  return (
    <Page fit title="Saúde financeira" subtitle="Seu mês comparado ao método 50/30/20: necessidades, desejos e poupança.">
      <div className={`sf${saude.participa ? ' on' : ''}`}>
        <Metodo saude={saude} onAlternar={onAlternar} />
        {saude.participa ? <Completa saude={saude} onGrupo={onGrupo} /> : <Convite saude={saude} onAlternar={onAlternar} />}
      </div>
    </Page>
  )
}

export function SaudeFinanceira() {
  const { dados, alternarMetodo, definirGrupoCategoria } = useApp()
  return (
    <SaudeFinanceiraView
      saude={dados.saudeFinanceira}
      onAlternar={(v) => void alternarMetodo(v)}
      onGrupo={(nome, g) => void definirGrupoCategoria(nome, g)}
    />
  )
}
