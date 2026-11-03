package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.config.ReservasProperties;
import co.edu.unimagdalena.samar.reservas.integracion.Notificador;
import co.edu.unimagdalena.samar.reservas.modelo.EstadoPago;
import co.edu.unimagdalena.samar.reservas.modelo.EstadoReserva;
import co.edu.unimagdalena.samar.reservas.modelo.Reserva;
import co.edu.unimagdalena.samar.reservas.persistencia.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Cambios que hace el sistema sin intervención de un usuario (id_usuario nulo en la bitácora).
 * Cada reserva se procesa en su propia transacción: si una falla, las demás siguen.
 * Antes de cambiar se vuelve a comprobar el estado, porque pudo cambiar desde la consulta.
 */
@Service
public class ProcesosAutomaticosService {

  private final ReservaRepository reservas;
  private final Cierres cierres;
  private final Notificador notificador;
  private final Clock clock;
  private final ReservasProperties props;

  public ProcesosAutomaticosService(ReservaRepository reservas, Cierres cierres, Notificador notificador,
      Clock clock, ReservasProperties props) {
    this.reservas = reservas;
    this.cierres = cierres;
    this.notificador = notificador;
    this.clock = clock;
    this.props = props;
  }

  @Transactional(readOnly = true)
  public List<Long> idsPorExpirar() {
    return reservas.idsPendientesVencidas(EstadoReserva.PENDIENTE, clock.instant().minus(props.plazoPago()),
        Set.of(EstadoPago.APROBADO, EstadoPago.PENDIENTE));
  }

  @Transactional(readOnly = true)
  public List<Long> idsPorCompletar() {
    return reservas.idsParaCompletar(EstadoReserva.CONFIRMADA, LocalDate.now(clock));
  }

  /** RF056: pendiente sin pago dentro del plazo → CANCELADA y cupos liberados. */
  @Transactional
  public boolean expirar(Long id) {
    Reserva r = reservas.buscarParaModificar(id).orElse(null);
    if (r == null || r.getEstado() != EstadoReserva.PENDIENTE || r.pagoVigente().isPresent()
        || clock.instant().isBefore(r.getCreadoEn().plus(props.plazoPago()))) {
      return false;
    }
    cierres.cerrar(r, EstadoReserva.CANCELADA, null, "Expirada: no se pagó dentro del plazo (RF056)");
    notificador.notificar(r.getIdTurista(), "Reserva expirada",
        "La reserva #" + r.getId() + " se canceló porque no se pagó a tiempo");
    return true;
  }

  /** RF049: confirmada cuya fecha ya pasó → COMPLETADA (habilita calificar, RF076). */
  @Transactional
  public boolean completar(Long id) {
    Reserva r = reservas.buscarParaModificar(id).orElse(null);
    if (r == null || r.getEstado() != EstadoReserva.CONFIRMADA
        || !r.detallePrincipal().getDisponibilidad().getFecha().isBefore(LocalDate.now(clock))) {
      return false;
    }
    r.cambiarEstado(EstadoReserva.COMPLETADA, null, "Completada: la fecha del servicio ya pasó");
    return true;
  }
}
