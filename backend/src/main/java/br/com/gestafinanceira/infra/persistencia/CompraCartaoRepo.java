package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.EntidadesCartao.CompraCartao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraCartaoRepo extends JpaRepository<CompraCartao, Long> {
    List<CompraCartao> findAllByCartaoIdAndDeletadoEmIsNullOrderByDataCompraAscIdAsc(Long cartaoId);
    Optional<CompraCartao> findByIdAndDeletadoEmIsNull(Long id);

    /** Compras ativas de cartões ativos. */
    @Query("select c from CompraCartao c where c.deletadoEm is null and c.cartaoId in "
            + "(select k.id from Cartao k where k.deletadoEm is null) order by c.id")
    List<CompraCartao> findAtivasDeCartoesAtivos();

    @Query("select count(c) from CompraCartao c where c.deletadoEm is null and c.categoria = :categoria and c.cartaoId in "
            + "(select k.id from Cartao k where k.deletadoEm is null)")
    long contarAtivasPorCategoria(@Param("categoria") String categoria);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CompraCartao c set c.deletadoEm = :agora where c.deletadoEm is null and c.cartaoId = :cartaoId")
    int excluirDoCartao(@Param("cartaoId") Long cartaoId, @Param("agora") LocalDateTime agora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CompraCartao c set c.deletadoEm = :agora where c.deletadoEm is null")
    int excluirTodasAtivas(@Param("agora") LocalDateTime agora);
}
