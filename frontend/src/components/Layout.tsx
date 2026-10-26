import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useApp } from '../store/AppContext'
import { Icon, type IconName } from './Icon'

interface Rota { to: string; label: string; icon: IconName }

export const ROTAS: Rota[] = [
  { to: '/', label: 'Projeção', icon: 'chart' },
  { to: '/saude', label: 'Saúde financeira', icon: 'pulse' },
  { to: '/perfil', label: 'Meu perfil', icon: 'user' },
  { to: '/gastos-fixos', label: 'Gastos fixos', icon: 'receipt' },
  { to: '/gastos-eventuais', label: 'Gastos eventuais', icon: 'wallet' },
  { to: '/cartao', label: 'Cartão de crédito', icon: 'card' },
  { to: '/decimo', label: '13º e férias', icon: 'gift' },
  { to: '/plr', label: 'PLR', icon: 'star' },
  { to: '/metas', label: 'Metas e reserva', icon: 'target' },
  { to: '/parametros', label: 'Parâmetros', icon: 'cog' },
]

const link = (r: Rota) => (
  <NavLink key={r.to} to={r.to} end={r.to === '/'} className={({ isActive }) => (isActive ? 'active' : undefined)}>
    <Icon name={r.icon} />{r.label}
  </NavLink>
)

function Sidebar() {
  return (
    <nav className="side" aria-label="Navegação principal">
      <div className="brand">
        <div className="mk"><Icon name="bars" strokeWidth={2.4} /></div>
        <div><b>Gestão Financeira</b><small>Box</small></div>
      </div>
      <div className="nl">Visão geral</div>
      {ROTAS.slice(0, 2).map(link)}
      <div className="nl">Meus dados</div>
      {ROTAS.slice(2, 9).map(link)}
      <div className="sp" />
      {link(ROTAS[9])}
      <div className="user">
        <div className="av">VC</div>
        <div><b>Você</b><small>Instalação local</small></div>
      </div>
    </nav>
  )
}

function Topbar() {
  const { pathname } = useLocation()
  const { projecao, salvando, aviso, limparAviso } = useApp()
  const atual = ROTAS.find((r) => (r.to === '/' ? pathname === '/' : pathname.startsWith(r.to)))
  const meses = projecao.meses
  return (
    <>
      <header className="top">
        <div className="crumb">Gestão Financeira Box / <b>{atual?.label ?? ''}</b></div>
        <span className="chip">{meses[0].rotulo} → {meses[meses.length - 1].rotulo}</span>
        <span className="chip"><i className="dot" />{salvando ? 'Salvando…' : 'Salvo no banco'}</span>
      </header>
      {aviso && (
        <div className="warn" role="alert" style={{ margin: '8px 24px 0', display: 'flex', justifyContent: 'space-between', gap: 12 }}>
          <span>⚠️ {aviso}</span>
          <button className="btn sm" onClick={limparAviso}>Fechar</button>
        </div>
      )}
    </>
  )
}

export function Layout() {
  return (
    <>
      <Sidebar />
      <main>
        <Topbar />
        <div className="wrap"><Outlet /></div>
      </main>
    </>
  )
}
