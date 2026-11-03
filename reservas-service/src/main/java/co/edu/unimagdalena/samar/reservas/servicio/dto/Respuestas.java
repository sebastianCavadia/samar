package co.edu.unimagdalena.samar.reservas.servicio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Respuestas de reservas-service. Las entidades nunca salen de la capa servicio. */
public final class Respuestas {

  private Respuestas() {
  }

  /** Lo que ve el turista al elegir fecha (pública). */
  public record DisponibilidadPublica(Long id, LocalDate fecha, LocalTime hora, int cuposLibres) {
  }

  /** Lo que ve el prestador al gestionar sus fechas. */
  public record DisponibilidadPrestador(Long id, LocalDate fecha, LocalTime hora, int cuposTotales,
      int cuposReservados, int cuposLibres, boolean activo) {
  }

  public record ResultadoApertura(int creadas, int actualizadas, List<String> omitidas) {
  }

  public record PagoInfo(Long id, String referencia, String medio, String estado, BigDecimal monto, Instant creadoEn) {
  }

  public record EventoBitacora(String estadoAnterior, String estadoNuevo, Long idUsuario, String detalle, Instant fecha) {
  }

  /**
   * @param cantidad  cupos ocupados (1 habitación en hospedaje, N personas en el resto)
   * @param expiraEn  hasta cuándo hay para pagar (solo PENDIENTE sin pago vigente; RF056)
   */
  public record Reserva(
      Long id,
      String estado,
      Long idServicio,
      String nombreServicio,
      String tipoServicio,
      Long idPrestador,
      Long idTurista,
      String nombreTurista,
      LocalDate fecha,
      LocalTime hora,
      int personas,
      int cantidad,
      BigDecimal precioUnitario,
      BigDecimal total,
      String moneda,
      String motivo,
      Instant creadoEn,
      Instant expiraEn,
      Integer calificacion,
      boolean puedeCalificar,
      PagoInfo pago,
      List<EventoBitacora> historial) {
  }
}
