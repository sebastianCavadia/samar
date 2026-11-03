package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.modelo.*;
import co.edu.unimagdalena.samar.reservas.persistencia.DisponibilidadRepository;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Component;

/**
 * Lo que tienen en común cancelar (RF040), rechazar (RF038) y expirar (RF056): pasar la
 * reserva a su estado final, devolver los cupos (RF035) y resolver el pago (RF045).
 */
@Component
class Cierres {

  private final DisponibilidadRepository disponibilidades;

  Cierres(DisponibilidadRepository disponibilidades) {
    this.disponibilidades = disponibilidades;
  }

  /** Valida antes de cambiar, para responder 409 con un mensaje claro en vez de un error interno. */
  static void exigirTransicion(Reserva r, EstadoReserva nuevo, String accion) {
    if (!r.getEstado().puedePasarA(nuevo)) {
      throw new ReglaNegocioException("No se puede " + accion + " una reserva " + r.getEstado().name().toLowerCase());
    }
  }

  void cerrar(Reserva r, EstadoReserva estadoFinal, Long idUsuario, String motivo) {
    r.cambiarEstado(estadoFinal, idUsuario, motivo);
    r.setMotivo(motivo);
    for (DetalleReserva d : r.getDetalles()) {
      disponibilidades.liberarCupos(d.getDisponibilidad().getId(), d.getCantidad());
    }
    for (Pago p : r.getPagos()) {
      if (p.getEstado() == EstadoPago.APROBADO) {
        p.setEstado(EstadoPago.REEMBOLSADO); // RF045: la política de penalización (RF042) queda pendiente
      } else if (p.getEstado() == EstadoPago.PENDIENTE) {
        p.setEstado(EstadoPago.RECHAZADO);
      }
    }
  }
}
