package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.modelo.*;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;

import java.time.Duration;

/** Entidad → DTO. Vive en servicio para que las entidades nunca suban a presentación. */
final class ReservaMapper {

  private ReservaMapper() {
  }

  static Respuestas.Reserva aRespuesta(Reserva r, Duration plazoPago) {
    DetalleReserva d = r.detallePrincipal();
    Disponibilidad disp = d.getDisponibilidad();
    boolean esperandoPago = r.getEstado() == EstadoReserva.PENDIENTE && r.pagoVigente().isEmpty();
    Pago pago = r.getPagos().isEmpty() ? null : r.getPagos().get(0);
    return new Respuestas.Reserva(r.getId(), r.getEstado().name(), d.getIdServicio(), d.getNombreServicio(),
        d.getTipoServicio(), d.getIdPrestador(), r.getIdTurista(), r.getNombreTurista(), disp.getFecha(),
        disp.getHora(), r.getPersonas(), d.getCantidad(), d.getPrecioUnitario(), r.getTotal(), r.getMoneda(),
        r.getMotivo(), r.getCreadoEn(), esperandoPago ? r.getCreadoEn().plus(plazoPago) : null,
        r.getCalificacion(), r.puedeCalificarse(), pago == null ? null : aPago(pago),
        r.getHistorial().stream().map(ReservaMapper::aEvento).toList());
  }

  static Respuestas.DisponibilidadPublica aPublica(Disponibilidad d) {
    return new Respuestas.DisponibilidadPublica(d.getId(), d.getFecha(), d.getHora(), d.cuposLibres());
  }

  static Respuestas.DisponibilidadPrestador aPrestador(Disponibilidad d) {
    return new Respuestas.DisponibilidadPrestador(d.getId(), d.getFecha(), d.getHora(), d.getCuposTotales(),
        d.getCuposReservados(), d.cuposLibres(), d.isActivo());
  }

  private static Respuestas.PagoInfo aPago(Pago p) {
    return new Respuestas.PagoInfo(p.getId(), p.getReferencia(), p.getMedio().name(), p.getEstado().name(),
        p.getMonto(), p.getCreadoEn());
  }

  private static Respuestas.EventoBitacora aEvento(RegistroBitacora b) {
    return new Respuestas.EventoBitacora(b.getEstadoAnterior() == null ? null : b.getEstadoAnterior().name(),
        b.getEstadoNuevo().name(), b.getIdUsuario(), b.getDetalle(), b.getFecha());
  }
}
