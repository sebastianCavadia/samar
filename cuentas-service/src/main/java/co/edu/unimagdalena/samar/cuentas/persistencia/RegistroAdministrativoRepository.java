package co.edu.unimagdalena.samar.cuentas.persistencia;

import co.edu.unimagdalena.samar.cuentas.modelo.RegistroAdministrativo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAdministrativoRepository extends JpaRepository<RegistroAdministrativo, Long> {

  List<RegistroAdministrativo> findByUsuarioIdOrderByFechaDesc(Long idUsuario);

  Page<RegistroAdministrativo> findAllByOrderByFechaDesc(Pageable pagina);
}
