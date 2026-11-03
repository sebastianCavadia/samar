package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.UsuarioService;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API para los otros microservicios (comunicación REST interna).
 * El gateway solo enruta /api/**, así que /internal/** no es alcanzable desde internet:
 * únicamente desde la red interna de Docker.
 */
@RestController
@RequestMapping("/internal/usuarios")
public class InternoController {

  private final UsuarioService usuarioService;

  public InternoController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @GetMapping("/{id}")
  public UsuarioResponse porId(@PathVariable Long id) {
    return usuarioService.consultarPorId(id);
  }
}
