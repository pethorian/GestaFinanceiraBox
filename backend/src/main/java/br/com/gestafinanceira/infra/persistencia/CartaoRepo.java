package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.EntidadesCartao.Cartao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartaoRepo extends JpaRepository<Cartao, Long> {
    List<Cartao> findAllByDeletadoEmIsNullOrderByIdAsc();
    Optional<Cartao> findByIdAndDeletadoEmIsNull(Long id);

    /** Soft delete em lote de todos os cartões ativos. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Cartao c set c.deletadoEm = :agora where c.deletadoEm is null")
    int excluirTodosAtivos(@Param("agora") LocalDateTime agora);
}
