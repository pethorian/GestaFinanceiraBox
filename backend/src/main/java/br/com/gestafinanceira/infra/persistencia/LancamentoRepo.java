package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.Lancamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRepo extends JpaRepository<Lancamento, Long> {
    List<Lancamento> findAllByOrderByIdAsc();
    long countByCategoria(String categoria);
}
