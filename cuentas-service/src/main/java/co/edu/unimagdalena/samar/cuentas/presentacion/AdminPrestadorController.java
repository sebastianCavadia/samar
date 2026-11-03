package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.ValidacionPrestadoresService;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Admin;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF095: validación de prestadores por el administrador. */
@RestController
@RequestMapping("/api/cuentas/admin/prestadores")
public class AdminPrestadorController {

  private final ValidacionPrestadoresService validaciones;

  public AdminPrestadorController(ValidacionPrestadoresService validaciones) {
    this.validaciones = validaciones;
  }

  /** ?validacion=PENDIENTE (por defecto) | VALIDADO | RECHAZADO */
  @GetMapping
  public List<Admin.Prestador> listar(Identidad actor, @RequestParam(required = false) String validacion) {
    return validaciones.listar(actor, validacion);
  }

  @GetMapping("/pendientes")
  public List<Admin.Prestador> pendientes(Identidad actor) {
    return validaciones.listar(actor, "PENDIENTE");
  }

  @PostMapping("/{id}/validar")
  public Admin.Prestador validar(Identidad actor, @PathVariable Long id) {
    return validaciones.validar(actor, id);
  }

  @PostMapping("/{id}/rechazar")
  public Admin.Prestador rechazar(Identidad actor, @PathVariable Long id, @Valid @RequestBody Admin.Rechazar req) {
    return validaciones.rechazar(actor, id, req.motivo());
  }
}
