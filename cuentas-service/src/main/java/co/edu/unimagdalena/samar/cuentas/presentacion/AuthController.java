package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.AutenticacionService;
import co.edu.unimagdalena.samar.cuentas.servicio.RegistroService;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.LoginRequest;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.RegistroPrestadorRequest;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.RegistroTuristaRequest;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.SesionResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Rutas públicas (el gateway no exige token en /api/cuentas/auth/**). */
@RestController
@RequestMapping("/api/cuentas/auth")
public class AuthController {

  private final RegistroService registroService;
  private final AutenticacionService autenticacionService;

  public AuthController(RegistroService registroService, AutenticacionService autenticacionService) {
    this.registroService = registroService;
    this.autenticacionService = autenticacionService;
  }

  @PostMapping("/registro/turista")
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse registrarTurista(@Valid @RequestBody RegistroTuristaRequest req) {
    return registroService.registrarTurista(req);
  }

  @PostMapping("/registro/prestador")
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse registrarPrestador(@Valid @RequestBody RegistroPrestadorRequest req) {
    return registroService.registrarPrestador(req);
  }

  @PostMapping("/login")
  public SesionResponse login(@Valid @RequestBody LoginRequest req) {
    return autenticacionService.iniciarSesion(req);
  }
}
