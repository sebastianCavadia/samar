package co.edu.unimagdalena.samar.oferta.presentacion;

import co.edu.unimagdalena.samar.oferta.servicio.CalificacionService;
import co.edu.unimagdalena.samar.oferta.servicio.CatalogoService;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Calificaciones;
import co.edu.unimagdalena.samar.oferta.servicio.dto.FiltroBusqueda;
import co.edu.unimagdalena.samar.oferta.servicio.dto.PaginaResponse;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioDetalleResponse;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioResumenResponse;
import org.springframework.web.bind.annotation.*;

/**
 * Visualizar oferta turística. Pública: el visitante anónimo puede consultar sin token.
 * Ej.: GET /api/oferta/servicios?q=tayrona&tipo=ACTIVIDAD&categoria=NATURALEZA&precioMax=200000&pagina=0
 */
@RestController
@RequestMapping("/api/oferta/servicios")
public class CatalogoController {

  private final CatalogoService catalogo;
  private final CalificacionService calificaciones;

  public CatalogoController(CatalogoService catalogo, CalificacionService calificaciones) {
    this.catalogo = catalogo;
    this.calificaciones = calificaciones;
  }

  @GetMapping
  public PaginaResponse<ServicioResumenResponse> buscar(FiltroBusqueda filtro,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "recientes") String orden) {
    return catalogo.buscar(filtro, pagina, orden);
  }

  @GetMapping("/{id}")
  public ServicioDetalleResponse detalle(@PathVariable Long id) {
    return catalogo.detalle(id);
  }

  /** RF080 y RF081: promedio, distribución por estrellas y reseñas (?estrellas=5 para filtrar). */
  @GetMapping("/{id}/calificaciones")
  public Calificaciones.ResumenPublico calificaciones(@PathVariable Long id,
      @RequestParam(required = false) Integer estrellas,
      @RequestParam(defaultValue = "0") int pagina) {
    return calificaciones.publicas(id, estrellas, pagina);
  }
}
