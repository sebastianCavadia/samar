package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.UsuarioService;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuentas/perfil")
public class PerfilController {

  private final UsuarioService usuarioService;

  public PerfilController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @GetMapping
  public UsuarioResponse miPerfil(Identidad actor) {
    return usuarioService.perfil(actor);
  }
}
