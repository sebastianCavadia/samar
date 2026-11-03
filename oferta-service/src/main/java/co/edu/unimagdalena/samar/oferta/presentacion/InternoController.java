package co.edu.unimagdalena.samar.oferta.presentacion;

import co.edu.unimagdalena.samar.oferta.servicio.CalificacionService;
import co.edu.unimagdalena.samar.oferta.servicio.CatalogoService;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Calificaciones;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioInternoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** API para reservas-service. Solo alcanzable dentro de la red interna (el gateway enruta /api/**). */
@RestController
@RequestMapping("/internal")
public class InternoController {

  private final CatalogoService catalogo;
  private final CalificacionService calificaciones;

  public InternoController(CatalogoService catalogo, CalificacionService calificaciones) {
    this.catalogo = catalogo;
    this.calificaciones = calificaciones;
  }

  @GetMapping("/servicios/{id}")
  public ServicioInternoResponse porId(@PathVariable Long id) {
    return catalogo.consultaInterna(id);
  }

  /** ADR-11: 201 si la registró, 200 si esa reserva ya estaba calificada (devuelve la existente). */
  @PostMapping("/calificaciones")
  public ResponseEntity<Calificaciones.RegistroInterno> calificar(@Valid @RequestBody Calificaciones.NuevaInterna req) {
    var registro = calificaciones.registrar(req);
    return ResponseEntity.status(registro.nueva() ? HttpStatus.CREATED : HttpStatus.OK).body(registro);
  }
}
