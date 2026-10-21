package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.domain.modelo.EventoAdicional;
import br.com.gestafinanceira.infra.persistencia.Entidades.DestinoAdicionalEntidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DestinoAdicionalRepo extends JpaRepository<DestinoAdicionalEntidade, Long> {
    List<DestinoAdicionalEntidade> findByEventoOrderByOrdemAscIdAsc(EventoAdicional evento);

    List<DestinoAdicionalEntidade> findAllByOrderByEventoAscOrdemAscIdAsc();

    @Modifying(flushAutomatically = true)
    @Query("delete from DestinoAdicional d where d.evento = :evento")
    void apagarDoEvento(@Param("evento") EventoAdicional evento);
}
