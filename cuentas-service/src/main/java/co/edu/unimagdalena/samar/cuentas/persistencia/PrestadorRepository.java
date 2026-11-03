package co.edu.unimagdalena.samar.cuentas.persistencia;

import co.edu.unimagdalena.samar.cuentas.modelo.EstadoCuenta;
import co.edu.unimagdalena.samar.cuentas.modelo.EstadoValidacion;
import co.edu.unimagdalena.samar.cuentas.modelo.PrestadorTuristico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrestadorRepository extends JpaRepository<PrestadorTuristico, Long> {

  boolean existsByNit(String nit);

  boolean existsByRnt(String rnt);

  List<PrestadorTuristico> findByEstadoValidacionOrderByCreadoEnAsc(EstadoValidacion estado);

  long countByEstadoValidacion(EstadoValidacion estado);

  /** Para oferta-service: prestadores cuyas ofertas no deben verse (cuenta inactiva o bloqueada). */
  @Query("select p.id from PrestadorTuristico p where p.estado <> :activo")
  List<Long> idsNoActivos(@Param("activo") EstadoCuenta activo);
}
