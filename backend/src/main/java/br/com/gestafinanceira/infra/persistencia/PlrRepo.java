package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.PlrFaixa;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Tabela de IR exclusivo do PLR, com vigência. */
public interface PlrRepo extends JpaRepository<PlrFaixa, Long> {
    @Query("select f from PlrFaixa f where f.vigenciaInicio = "
            + "(select max(x.vigenciaInicio) from PlrFaixa x where x.vigenciaInicio <= :data) order by f.id")
    List<PlrFaixa> vigentes(@Param("data") LocalDate data);

    @Query("select f from PlrFaixa f where f.vigenciaInicio = "
            + "(select min(x.vigenciaInicio) from PlrFaixa x) order by f.id")
    List<PlrFaixa> maisAntigas();
}
