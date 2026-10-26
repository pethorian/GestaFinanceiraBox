import { useApp } from '../store/AppContext'
import { ListaGastos } from './ListaGastos'

export function GastosFixos() {
  const { dados, categorias, resumoFixos, adicionarLancamento, editarLancamento, removerLancamento } = useApp()
  return (
    <ListaGastos
      grupo="FIXOS"
      titulo="Gastos fixos"
      subtitulo="Contas que se repetem todo mês e compras parceladas."
      rotuloNovo="+ Novo gasto fixo"
      lancamentos={dados.lancamentos}
      resumo={resumoFixos}
      categorias={categorias}
      onAdicionar={() => void adicionarLancamento('RECORRENTE')}
      onEditar={editarLancamento}
      onRemover={(id) => void removerLancamento(id)}
    />
  )
}
