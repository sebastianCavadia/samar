package co.edu.unimagdalena.samar.oferta.servicio.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Peticiones y respuestas de calificaciones (RF030, RF075, RF076, RF080, RF081). */
public final class Calificaciones {

  private Calificaciones() {
  }

  /**
   * ADR-11: la envía reservas-service después de comprobar que la reserva está COMPLETADA y es del
   * turista. Nunca llega desde el navegador (/internal/** no pasa por el gateway).
   */
  public record NuevaInterna(@NotNull Long idReserva, @NotNull Long idServicio, @NotNull Long idTurista,
      @Size(max = 120) String nombreTurista, @NotNull @Min(1) @Max(5) Integer puntuacion,
      @Size(max = 1000) String comentario) {
  }

  /** @param nueva false si esa reserva ya tenía calificación: se devuelve la existente (idempotente). */
  public record RegistroInterno(Long id, Long idReserva, int puntuacion, boolean nueva) {
  }

  /** RF030. */
  public record Responder(@NotBlank @Size(max = 1000) String respuesta) {
  }

  /** Reseña pública: el autor se muestra abreviado ("Ana T."), nunca con su id ni su correo. */
  public record Resena(Long id, int puntuacion, String comentario, String autor, Instant fecha,
      String respuesta, Instant fechaRespuesta) {
  }

  public record ConteoEstrellas(int estrellas, long total) {
  }

  /**
   * RF080 y RF081: promedio, cuántas reseñas hay de cada puntuación (de 5 a 1) y la página pedida,
   * filtrada por {@code estrellas} si se indicó.
   */
  public record ResumenPublico(BigDecimal promedio, int total, List<ConteoEstrellas> distribucion,
      Integer estrellas, PaginaResponse<Resena> resenas) {
  }

  public record ResenaPrestador(Long id, Long idServicio, String nombreServicio, int puntuacion, String comentario,
      String autor, Instant fecha, String respuesta, Instant fechaRespuesta) {
  }

  /** Lo que recibió el prestador en todos sus servicios, con cuántas siguen sin respuesta. */
  public record Bandeja(long sinResponder, PaginaResponse<ResenaPrestador> resenas) {
  }
}
