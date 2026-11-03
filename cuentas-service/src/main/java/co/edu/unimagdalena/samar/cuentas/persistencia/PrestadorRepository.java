package co.edu.unimagdalena.samar.cuentas.persistencia;

import co.edu.unimagdalena.samar.cuentas.modelo.PrestadorTuristico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrestadorRepository extends JpaRepository<PrestadorTuristico, Long> {

  boolean existsByNit(String nit);

  boolean existsByRnt(String rnt);

  List<PrestadorTuristico> findByValidadoFalseOrderByCreadoEnAsc();
}
