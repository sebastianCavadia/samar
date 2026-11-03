package co.edu.unimagdalena.samar.oferta.presentacion;

import co.edu.unimagdalena.samar.oferta.servicio.GestionOfertaService;
import co.edu.unimagdalena.samar.oferta.servicio.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestionar oferta turística (prestador). Requiere token: el gateway no la marca como pública. */
@RestController
@RequestMapping("/api/oferta/prestador/servicios")
public class PrestadorServicioController {

  private final GestionOfertaService gestion;

  public PrestadorServicioController(GestionOfertaService gestion) {
    this.gestion = gestion;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ServicioDetalleResponse registrar(Identidad actor, @Valid @RequestBody ServicioRequest req) {
    return gestion.registrar(actor, req);
  }

  @PutMapping("/{id}")
  public ServicioDetalleResponse editar(Identidad actor, @PathVariable Long id,
      @Valid @RequestBody ServicioRequest req) {
    return gestion.editar(actor, id, req);
  }

  @PatchMapping("/{id}/estado")
  public ServicioResumenResponse cambiarEstado(Identidad actor, @PathVariable Long id,
      @Valid @RequestBody CambioEstadoRequest req) {
    return gestion.cambiarEstado(actor, id, req.activo());
  }

  @GetMapping
  public List<ServicioResumenResponse> misServicios(Identidad actor) {
    return gestion.misServicios(actor);
  }

  @GetMapping("/{id}")
  public ServicioDetalleResponse detallePropio(Identidad actor, @PathVariable Long id) {
    return gestion.detallePropio(actor, id);
  }
}
