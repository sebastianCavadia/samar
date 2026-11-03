package co.edu.unimagdalena.samar.reservas.persistencia;

import co.edu.unimagdalena.samar.reservas.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

  boolean existsByReferencia(String referencia);
}
