package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.DeltaSalarial;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeltaRepo extends JpaRepository<DeltaSalarial, Long> {
    List<DeltaSalarial> findAllByOrderByVigenciaInicioAscReferenciaAsc();
    List<DeltaSalarial> findByVigenciaInicio(LocalDate inicio);

    @Query("select d from DeltaSalarial d where d.referencia = :ref and d.vigenciaInicio = "
            + "(select max(x.vigenciaInicio) from DeltaSalarial x where x.referencia = :ref and x.vigenciaInicio <= :data)")
    Optional<DeltaSalarial> vigente(@Param("ref") int referencia, @Param("data") LocalDate data);

    @Query("select d from DeltaSalarial d where d.referencia = :ref and d.vigenciaInicio = "
            + "(select min(x.vigenciaInicio) from DeltaSalarial x where x.referencia = :ref)")
    Optional<DeltaSalarial> maisAntigo(@Param("ref") int referencia);

    @Modifying
    @Query("delete from DeltaSalarial d where d.vigenciaInicio = :inicio")
    void deleteByVigenciaInicio(@Param("inicio") LocalDate inicio);
}
