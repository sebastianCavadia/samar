package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.modelo.Calificacion;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Calificaciones;
import co.edu.unimagdalena.samar.oferta.servicio.dto.PaginaResponse;
import org.springframework.data.domain.Page;

import java.util.Locale;
import java.util.function.Function;

/** Entidad → DTO. Vive en servicio para que las entidades nunca suban a presentación. */
final class CalificacionMapper {

  private CalificacionMapper() {
  }

  static Calificaciones.Resena aResena(Calificacion c) {
    return new Calificaciones.Resena(c.getId(), c.getPuntuacion(), c.getComentario(), autor(c.getNombreTurista()),
        c.getFecha(), c.getRespuestaPrestador(), c.getFechaRespuesta());
  }

  static Calificaciones.ResenaPrestador aResenaPrestador(Calificacion c) {
    return new Calificaciones.ResenaPrestador(c.getId(), c.getServicio().getId(), c.getServicio().getNombre(),
        c.getPuntuacion(), c.getComentario(), autor(c.getNombreTurista()), c.getFecha(), c.getRespuestaPrestador(),
        c.getFechaRespuesta());
  }

  static Calificaciones.RegistroInterno aRegistro(Calificacion c, boolean nueva) {
    return new Calificaciones.RegistroInterno(c.getId(), c.getIdReserva(), c.getPuntuacion(), nueva);
  }

  static <E, D> PaginaResponse<D> aPagina(Page<E> pagina, Function<E, D> mapeo) {
    return new PaginaResponse<>(pagina.getContent().stream().map(mapeo).toList(), pagina.getNumber(),
        pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
  }

  /** "Ana María Turista" → "Ana M.": la reseña es pública, el nombre completo no tiene por qué serlo. */
  static String autor(String nombreCompleto) {
    if (nombreCompleto == null || nombreCompleto.isBlank()) {
      return "Turista";
    }
    String[] partes = nombreCompleto.trim().split("\\s+");
    if (partes.length == 1) {
      return partes[0];
    }
    return partes[0] + " " + partes[1].substring(0, 1).toUpperCase(Locale.ROOT) + ".";
  }
}
