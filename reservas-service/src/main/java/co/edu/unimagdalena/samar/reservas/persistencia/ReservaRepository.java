package co.edu.unimagdalena.samar.reservas.persistencia;

import co.edu.unimagdalena.samar.reservas.modelo.EstadoPago;
import co.edu.unimagdalena.samar.reservas.modelo.EstadoReserva;
import co.edu.unimagdalena.samar.reservas.modelo.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

  /**
   * Carga para modificar: incrementa la versión al confirmar la transacción aunque solo se agregue
   * un pago. Así dos operaciones simultáneas sobre la misma reserva nunca se pisan (una falla con 409).
   */
  @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
  @Query("select r from Reserva r where r.id = :id")
  Optional<Reserva> buscarParaModificar(@Param("id") Long id);

  List<Reserva> findByIdTuristaOrderByCreadoEnDesc(Long idTurista);

  @Query("select distinct r from Reserva r join r.detalles d where d.idPrestador = :prestador order by r.creadoEn desc")
  List<Reserva> recibidasPor(@Param("prestador") Long prestador);

  @Query("""
      select distinct r from Reserva r join r.detalles d
       where d.idPrestador = :prestador and r.estado = :estado order by r.creadoEn desc""")
  List<Reserva> recibidasPorEstado(@Param("prestador") Long prestador, @Param("estado") EstadoReserva estado);

  /** RF056: pendientes creadas antes del límite y sin pago aprobado ni comprobante en revisión. */
  @Query("""
      select r.id from Reserva r
       where r.estado = :pendiente and r.creadoEn < :limite
         and not exists (select p from Pago p where p.reserva = r and p.estado in :vigentes)""")
  List<Long> idsPendientesVencidas(@Param("pendiente") EstadoReserva pendiente, @Param("limite") Instant limite,
      @Param("vigentes") Collection<EstadoPago> vigentes);

  /** RF049: confirmadas cuya fecha de servicio ya pasó. */
  @Query("""
      select r.id from Reserva r join r.detalles d join d.disponibilidad disp
       where r.estado = :confirmada group by r.id having max(disp.fecha) < :hoy""")
  List<Long> idsParaCompletar(@Param("confirmada") EstadoReserva confirmada, @Param("hoy") LocalDate hoy);
}
