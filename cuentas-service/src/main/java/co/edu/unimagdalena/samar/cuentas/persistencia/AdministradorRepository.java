package co.edu.unimagdalena.samar.cuentas.persistencia;

import co.edu.unimagdalena.samar.cuentas.modelo.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
}
