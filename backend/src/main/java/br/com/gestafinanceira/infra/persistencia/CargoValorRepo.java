package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.CargoValor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CargoValorRepo extends JpaRepository<CargoValor, Long> {
    List<CargoValor> findByCargoIdOrderByVigenciaInicioDesc(Long cargoId);
    Optional<CargoValor> findByCargoIdAndVigenciaInicio(Long cargoId, LocalDate inicio);
    List<CargoValor> findByCargoIdOrderByVigenciaInicioAsc(Long cargoId);

    @Query("select v from CargoValor v where v.cargoId = :cargoId and v.vigenciaInicio = "
            + "(select max(x.vigenciaInicio) from CargoValor x where x.cargoId = :cargoId and x.vigenciaInicio <= :data)")
    Optional<CargoValor> vigente(@Param("cargoId") Long cargoId, @Param("data") LocalDate data);

    @Query("select v from CargoValor v where v.cargoId = :cargoId and v.vigenciaInicio = "
            + "(select min(x.vigenciaInicio) from CargoValor x where x.cargoId = :cargoId)")
    Optional<CargoValor> maisAntigo(@Param("cargoId") Long cargoId);

    void deleteByCargoId(Long cargoId);
}
