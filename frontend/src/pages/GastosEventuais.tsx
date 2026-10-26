import { useApp } from '../store/AppContext'
import { ListaGastos } from './ListaGastos'

export function GastosEventuais() {
  const { dados, categorias, resumoEventuais, adicionarLancamento, editarLancamento, removerLancamento } = useApp()
  return (
    <ListaGastos
      grupo="EVENTUAIS"
      titulo="Gastos eventuais"
      subtitulo="Gastos de uma vez só: saem da lista sozinhos depois que o mês passa."
      rotuloNovo="+ Novo gasto eventual"
      lancamentos={dados.lancamentos}
      resumo={resumoEventuais}
      categorias={categorias}
      onAdicionar={() => void adicionarLancamento('PONTUAL')}
      onEditar={editarLancamento}
      onRemover={(id) => void removerLancamento(id)}
    />
  )
}
