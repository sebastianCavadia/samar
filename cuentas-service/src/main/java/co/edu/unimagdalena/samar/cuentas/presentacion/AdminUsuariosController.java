package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.AdministracionService;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Admin;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.PaginaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Administrar usuarios (RF091-RF094, RF097, RF099, RF100). */
@RestController
@RequestMapping("/api/cuentas/admin")
public class AdminUsuariosController {

  private final AdministracionService administracion;

  public AdminUsuariosController(AdministracionService administracion) {
    this.administracion = administracion;
  }

  @GetMapping("/resumen")
  public Admin.Resumen resumen(Identidad actor) {
    return administracion.resumen(actor);
  }

  /** ?q=texto en nombre o correo &rol=TURISTA|PRESTADOR|ADMINISTRADOR &estado=ACTIVO|INACTIVO|BLOQUEADO &pagina=0 */
  @GetMapping("/usuarios")
  public PaginaResponse<Admin.UsuarioFila> buscar(Identidad actor, @RequestParam(required = false) String q,
      @RequestParam(required = false) String rol, @RequestParam(required = false) String estado,
      @RequestParam(defaultValue = "0") int pagina) {
    return administracion.buscar(actor, q, rol, estado, pagina);
  }

  @GetMapping("/usuarios/{id}")
  public Admin.UsuarioFicha ficha(Identidad actor, @PathVariable Long id) {
    return administracion.ficha(actor, id);
  }

  @PostMapping("/usuarios/{id}/estado")
  public Admin.UsuarioFicha cambiarEstado(Identidad actor, @PathVariable Long id,
      @Valid @RequestBody Admin.CambiarEstado req) {
    return administracion.cambiarEstado(actor, id, req);
  }

  @PutMapping("/usuarios/{id}")
  public Admin.UsuarioFicha editar(Identidad actor, @PathVariable Long id, @Valid @RequestBody Admin.EditarUsuario req) {
    return administracion.editar(actor, id, req);
  }

  @PostMapping("/administradores")
  @ResponseStatus(HttpStatus.CREATED)
  public Admin.UsuarioFicha crearAdministrador(Identidad actor, @Valid @RequestBody Admin.CrearAdministrador req) {
    return administracion.crearAdministrador(actor, req);
  }

  @GetMapping("/bitacora")
  public PaginaResponse<Admin.Evento> bitacora(Identidad actor, @RequestParam(defaultValue = "0") int pagina) {
    return administracion.bitacora(actor, pagina);
  }
}
