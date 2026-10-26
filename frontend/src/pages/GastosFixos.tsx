import { api } from '../api/api'
import { useApp } from '../store/AppContext'
import { CartoesCard } from './CartoesCard'
import { ListaGastos } from './ListaGastos'

export function GastosFixos() {
  const { dados, categorias, resumoFixos, cartoes, adicionarLancamento, editarLancamento, removerLancamento, alterarCartoes } = useApp()
  return (
    <ListaGastos
      grupo="FIXOS"
      titulo="Gastos fixos"
      subtitulo="Contas que se repetem todo mês, compras parceladas e a fatura dos cartões de crédito."
      rotuloNovo="+ Novo gasto fixo"
      lancamentos={dados.lancamentos}
      resumo={resumoFixos}
      categorias={categorias}
      onAdicionar={() => void adicionarLancamento('RECORRENTE')}
      onEditar={editarLancamento}
      onRemover={(id) => void removerLancamento(id)}
    >
      <CartoesCard cartoes={cartoes} onCadastrar={(c) => alterarCartoes(() => api.criarCartao(c))} />
    </ListaGastos>
  )
}
