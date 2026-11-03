package co.edu.unimagdalena.samar.oferta.presentacion;

import co.edu.unimagdalena.samar.oferta.servicio.CatalogoService;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioInternoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API para reservas-service. Solo alcanzable dentro de la red interna (el gateway enruta /api/**). */
@RestController
@RequestMapping("/internal/servicios")
public class InternoController {

  private final CatalogoService catalogo;

  public InternoController(CatalogoService catalogo) {
    this.catalogo = catalogo;
  }

  @GetMapping("/{id}")
  public ServicioInternoResponse porId(@PathVariable Long id) {
    return catalogo.consultaInterna(id);
  }
}
