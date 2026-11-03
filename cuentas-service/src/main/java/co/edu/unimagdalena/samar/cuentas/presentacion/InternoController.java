package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.UsuarioService;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API para los otros microservicios (comunicación REST interna).
 * El gateway solo enruta /api/**, así que /internal/** no es alcanzable desde internet:
 * únicamente desde la red interna de Docker.
 */
@RestController
@RequestMapping("/internal")
public class InternoController {

  private final UsuarioService usuarioService;

  public InternoController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @GetMapping("/usuarios/{id}")
  public UsuarioResponse porId(@PathVariable Long id) {
    return usuarioService.consultarPorId(id);
  }

  /** oferta-service lo consulta cada minuto para ocultar las ofertas de cuentas desactivadas (ADR-13). */
  @GetMapping("/prestadores/suspendidos")
  public List<Long> prestadoresSuspendidos() {
    return usuarioService.prestadoresSuspendidos();
  }
}
