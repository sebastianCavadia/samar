package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.config.CuentasProperties;
import co.edu.unimagdalena.samar.cuentas.modelo.EstadoCuenta;
import co.edu.unimagdalena.samar.cuentas.modelo.Usuario;
import co.edu.unimagdalena.samar.cuentas.persistencia.UsuarioRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.LoginRequest;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.SesionResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.CredencialesInvalidasException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** RF004-RF006 y RF013: inicio de sesión con bloqueo temporal por intentos fallidos. */
@Service
public class AutenticacionService {

  private final UsuarioRepository usuarios;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokens;
  private final CuentasProperties props;
  private final Clock clock;

  public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
      TokenService tokens, CuentasProperties props, Clock clock) {
    this.usuarios = usuarios;
    this.passwordEncoder = passwordEncoder;
    this.tokens = tokens;
    this.props = props;
    this.clock = clock;
  }

  /**
   * noRollbackFor: el intento fallido debe quedar guardado aunque se lance la excepción;
   * sin esto el contador nunca llegaría a 5 y RF005 no funcionaría.
   */
  @Transactional(noRollbackFor = CredencialesInvalidasException.class)
  public SesionResponse iniciarSesion(LoginRequest req) {
    Instant ahora = clock.instant();
    Usuario u = usuarios.findByCorreoIgnoreCase(RegistroService.normalizarCorreo(req.correo()))
        .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));

    if (u.getEstado() != EstadoCuenta.ACTIVO) {
      throw new AccesoDenegadoException("La cuenta está " + u.getEstado()
          + ". Contacta a la administración");
    }
    if (u.estaBloqueadoTemporalmente(ahora)) {
      throw new AccesoDenegadoException(
          "Cuenta bloqueada temporalmente por intentos fallidos. Intenta más tarde");
    }

    if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
      u.registrarIntentoFallido(props.maxIntentosFallidos(),
          ahora.plus(Duration.ofMinutes(props.bloqueoMin())));
      throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
    }

    u.registrarAccesoExitoso(ahora);
    TokenService.TokenEmitido token = tokens.emitir(u);
    return new SesionResponse(token.token(), token.expiraEn(), UsuarioMapper.aResponse(u));
  }
}
