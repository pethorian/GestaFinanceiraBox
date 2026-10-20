package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.ParametroBeneficio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BeneficioRepo extends JpaRepository<ParametroBeneficio, Long> {
    List<ParametroBeneficio> findAllByOrderByVigenciaInicioAsc();
    Optional<ParametroBeneficio> findByVigenciaInicio(LocalDate inicio);

    @Query("select p from ParametroBeneficio p where p.vigenciaInicio = "
            + "(select max(x.vigenciaInicio) from ParametroBeneficio x where x.vigenciaInicio <= :data)")
    Optional<ParametroBeneficio> vigente(@Param("data") LocalDate data);

    @Query("select p from ParametroBeneficio p where p.vigenciaInicio = "
            + "(select min(x.vigenciaInicio) from ParametroBeneficio x)")
    Optional<ParametroBeneficio> maisAntigo();
}
