package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.IrpfFaixa;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IrpfRepo extends JpaRepository<IrpfFaixa, Long> {
    List<IrpfFaixa> findAllByOrderByVigenciaInicioAscIdAsc();

    @Query("select f from IrpfFaixa f where f.vigenciaInicio = "
            + "(select max(x.vigenciaInicio) from IrpfFaixa x where x.vigenciaInicio <= :data) order by f.id")
    List<IrpfFaixa> vigentes(@Param("data") LocalDate data);

    @Query("select f from IrpfFaixa f where f.vigenciaInicio = "
            + "(select min(x.vigenciaInicio) from IrpfFaixa x) order by f.id")
    List<IrpfFaixa> maisAntigas();

    @Modifying
    @Query("delete from IrpfFaixa f where f.vigenciaInicio = :inicio")
    void deleteByVigenciaInicio(@Param("inicio") LocalDate inicio);
}
