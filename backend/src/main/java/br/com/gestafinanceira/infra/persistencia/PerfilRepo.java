package br.com.gestafinanceira.infra.persistencia;

import br.com.gestafinanceira.infra.persistencia.Entidades.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepo extends JpaRepository<Perfil, Short> {
}
