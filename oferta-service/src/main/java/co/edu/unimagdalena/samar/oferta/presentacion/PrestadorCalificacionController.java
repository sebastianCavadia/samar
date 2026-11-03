package co.edu.unimagdalena.samar.oferta.presentacion;

import co.edu.unimagdalena.samar.oferta.servicio.CalificacionService;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Calificaciones;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Reseñas recibidas por el prestador y su respuesta pública (RF030). Requiere token. */
@RestController
@RequestMapping("/api/oferta/prestador/calificaciones")
public class PrestadorCalificacionController {

  private final CalificacionService calificaciones;

  public PrestadorCalificacionController(CalificacionService calificaciones) {
    this.calificaciones = calificaciones;
  }

  @GetMapping
  public Calificaciones.Bandeja recibidas(Identidad actor,
      @RequestParam(defaultValue = "false") boolean sinResponder,
      @RequestParam(defaultValue = "0") int pagina) {
    return calificaciones.recibidas(actor, sinResponder, pagina);
  }

  @PutMapping("/{id}/respuesta")
  public Calificaciones.ResenaPrestador responder(Identidad actor, @PathVariable Long id,
      @Valid @RequestBody Calificaciones.Responder req) {
    return calificaciones.responder(actor, id, req.respuesta());
  }
}
