package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.PlrGasto;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlrGastoRepo extends JpaRepository<PlrGasto, Long> {
    List<PlrGasto> findAllByDeletadoEmIsNullOrderByDataDescIdDesc();

    Optional<PlrGasto> findByIdAndDeletadoEmIsNull(Long id);

    boolean existsByDestinoIdInAndDeletadoEmIsNull(Collection<Long> destinoIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlrGasto g set g.deletadoEm = :agora where g.deletadoEm is null")
    int excluirTodosAtivos(@Param("agora") LocalDateTime agora);
}
