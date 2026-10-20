package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.Cargo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoRepo extends JpaRepository<Cargo, Long> {
    List<Cargo> findAllByOrderByOrdemAscIdAsc();
    boolean existsByNomeIgnoreCase(String nome);
}
