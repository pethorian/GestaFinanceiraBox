package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.Meta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaRepo extends JpaRepository<Meta, Long> {
    List<Meta> findAllByOrderByIdAsc();
}
