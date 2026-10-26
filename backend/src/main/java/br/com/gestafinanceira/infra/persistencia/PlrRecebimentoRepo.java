package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.PlrRecebimento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlrRecebimentoRepo extends JpaRepository<PlrRecebimento, Long> {
    List<PlrRecebimento> findAllByDeletadoEmIsNullOrderByDataDescIdDesc();

    Optional<PlrRecebimento> findByIdAndDeletadoEmIsNull(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlrRecebimento r set r.deletadoEm = :agora where r.deletadoEm is null")
    int excluirTodosAtivos(@Param("agora") LocalDateTime agora);
}
