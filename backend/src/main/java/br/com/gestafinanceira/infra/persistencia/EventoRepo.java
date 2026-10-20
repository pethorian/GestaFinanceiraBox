package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.EventoSalarial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepo extends JpaRepository<EventoSalarial, Long> {
    List<EventoSalarial> findAllByOrderByMesAsc();

    List<EventoSalarial> findByTipoOrderByMesAsc(String tipo);

    void deleteByTipo(String tipo);
}
