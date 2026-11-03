package co.edu.unimagdalena.samar.reservas.servicio.dto;

import jakarta.validation.constraints.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** Cuerpos de las peticiones de reservas-service. */
public final class Solicitudes {

  private Solicitudes() {
  }

  /** RF036. */
  public record CrearReserva(@NotNull Long idDisponibilidad, @NotNull @Min(1) @Max(50) Integer personas) {
  }

  /** RF043 (PASARELA, simulada) o RF044 (COMPROBANTE con número de transferencia). */
  public record Pagar(@NotBlank @Pattern(regexp = "PASARELA|COMPROBANTE") String medio,
      @Size(max = 80) String referencia) {
  }

  /** RF040 cancelar y RF038 rechazar. */
  public record Motivo(@NotBlank @Size(max = 200) String motivo) {
  }

  /** RF044: el prestador revisa el comprobante. */
  public record ValidarPago(@NotNull Boolean aprobado) {
  }

  /**
   * RF018: abre (o actualiza) cupos para un rango de fechas.
   * @param hora        opcional: actividades con horario fijo; vacío = todo el día
   * @param diasSemana  opcional: solo esos días (p. ej. fines de semana); vacío = todos
   */
  public record AbrirDisponibilidad(@NotNull LocalDate desde, @NotNull LocalDate hasta, LocalTime hora,
      @NotNull @Min(0) @Max(10000) Integer cupos, Set<DayOfWeek> diasSemana) {
  }

  /** Bloquear o desbloquear una fecha (p. ej. cierre por clima). */
  public record CambiarDisponibilidad(@NotNull Boolean activo) {
  }
}
