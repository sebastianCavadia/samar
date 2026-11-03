package co.edu.unimagdalena.samar.cuentas.persistencia;

import co.edu.unimagdalena.samar.cuentas.modelo.Turista;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TuristaRepository extends JpaRepository<Turista, Long> {

  boolean existsByNumeroDocumento(String numeroDocumento);
}
