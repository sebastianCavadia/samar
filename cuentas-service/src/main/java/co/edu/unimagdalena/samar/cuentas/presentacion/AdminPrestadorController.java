package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.UsuarioService;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.PrestadorPendienteResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF095: validación de prestadores por el administrador. */
@RestController
@RequestMapping("/api/cuentas/admin/prestadores")
public class AdminPrestadorController {

  private final UsuarioService usuarioService;

  public AdminPrestadorController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @GetMapping("/pendientes")
  public List<PrestadorPendienteResponse> pendientes(Identidad actor) {
    return usuarioService.prestadoresPendientes(actor);
  }

  @PostMapping("/{id}/validar")
  public UsuarioResponse validar(Identidad actor, @PathVariable Long id) {
    return usuarioService.validarPrestador(actor, id);
  }
}
