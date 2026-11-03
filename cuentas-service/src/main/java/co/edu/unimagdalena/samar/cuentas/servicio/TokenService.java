package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.config.CuentasProperties;
import co.edu.unimagdalena.samar.cuentas.modelo.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Emite el JWT (HS256). Cuentas es el único servicio que firma tokens;
 * el gateway los verifica con el mismo secreto.
 * Claims: sub = id del usuario, rol, correo.
 */
@Service
public class TokenService {

  public record TokenEmitido(String token, Instant expiraEn) {
  }

  private final SecretKey clave;
  private final Duration vigencia;
  private final Clock clock;

  public TokenService(CuentasProperties props, Clock clock) {
    this.clave = Keys.hmacShaKeyFor(props.jwtSecret().getBytes(StandardCharsets.UTF_8));
    this.vigencia = Duration.ofMinutes(props.jwtExpiracionMin());
    this.clock = clock;
  }

  public TokenEmitido emitir(Usuario usuario) {
    Instant ahora = clock.instant();
    Instant expira = ahora.plus(vigencia);
    String token = Jwts.builder()
        .subject(String.valueOf(usuario.getId()))
        .claim("rol", usuario.getRol().name())
        .claim("correo", usuario.getCorreo())
        .issuedAt(Date.from(ahora))
        .expiration(Date.from(expira))
        .signWith(clave)
        .compact();
    return new TokenEmitido(token, expira);
  }
}
