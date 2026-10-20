package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.InssFaixa;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InssRepo extends JpaRepository<InssFaixa, Long> {
    List<InssFaixa> findAllByOrderByVigenciaInicioAscAteAsc();

    @Query("select f from InssFaixa f where f.vigenciaInicio = "
            + "(select max(x.vigenciaInicio) from InssFaixa x where x.vigenciaInicio <= :data) order by f.ate")
    List<InssFaixa> vigentes(@Param("data") LocalDate data);

    @Query("select f from InssFaixa f where f.vigenciaInicio = "
            + "(select min(x.vigenciaInicio) from InssFaixa x) order by f.ate")
    List<InssFaixa> maisAntigas();

    @Modifying
    @Query("delete from InssFaixa f where f.vigenciaInicio = :inicio")
    void deleteByVigenciaInicio(@Param("inicio") LocalDate inicio);
}
