import { HashRouter, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { AppProvider } from './store/AppContext'
import { Decimo } from './pages/Decimo'
import { Gastos } from './pages/Gastos'
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
            <Route path="gastos" element={<Gastos />} />
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
