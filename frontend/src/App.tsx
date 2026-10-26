import { HashRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { AppProvider } from './store/AppContext'
import { Cartao } from './pages/Cartao'
import { Decimo } from './pages/Decimo'
import { GastosEventuais } from './pages/GastosEventuais'
import { GastosFixos } from './pages/GastosFixos'
import { Metas } from './pages/Metas'
import { Parametros } from './pages/Parametros'
import { Perfil } from './pages/Perfil'
import { Projecao } from './pages/Projecao'
import { SaudeFinanceira } from './pages/SaudeFinanceira'

export default function App() {
  return (
    <AppProvider>
      <HashRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Projecao />} />
            <Route path="saude" element={<SaudeFinanceira />} />
            <Route path="perfil" element={<Perfil />} />
            <Route path="gastos" element={<Navigate to="/gastos-fixos" replace />} />
            <Route path="gastos-fixos" element={<GastosFixos />} />
            <Route path="gastos-eventuais" element={<GastosEventuais />} />
            <Route path="cartao" element={<Cartao />} />
            <Route path="decimo" element={<Decimo />} />
            <Route path="metas" element={<Metas />} />
            <Route path="parametros" element={<Parametros />} />
            <Route path="*" element={<Projecao />} />
          </Route>
        </Routes>
      </HashRouter>
    </AppProvider>
  )
}
