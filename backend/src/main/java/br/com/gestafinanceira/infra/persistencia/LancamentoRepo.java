package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.Lancamento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoRepo extends JpaRepository<Lancamento, Long> {
    List<Lancamento> findAllByDeletadoEmIsNullOrderByIdAsc();
    Optional<Lancamento> findByIdAndDeletadoEmIsNull(Long id);
    long countByCategoriaAndDeletadoEmIsNull(String categoria);

    /** Soft delete em lote de todos os lançamentos ativos. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Lancamento l set l.deletadoEm = :agora, l.motivoExclusao = :motivo where l.deletadoEm is null")
    int excluirTodosAtivos(@Param("agora") LocalDateTime agora, @Param("motivo") String motivo);

    /** Expira (soft delete) os pontuais ativos com mês inicial anterior a {@code limite} (1º dia do mês corrente). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Lancamento l set l.deletadoEm = :agora, l.motivoExclusao = 'EXPIRADO' "
            + "where l.deletadoEm is null and l.tipo = br.com.gestafinanceira.domain.modelo.TipoLancamento.PONTUAL "
            + "and l.mesInicial < :limite")
    int expirarPontuaisAnteriores(@Param("limite") LocalDate limite, @Param("agora") LocalDateTime agora);
}
