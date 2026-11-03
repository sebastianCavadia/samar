package co.edu.unimagdalena.samar.gateway.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Autenticación centralizada (decisión del entregable 2): el gateway valida el JWT y
 * reenvía la identidad a los microservicios en las cabeceras X-Usuario-Id y X-Usuario-Rol.
 * Las cabeceras que vengan del cliente se eliminan siempre, para que nadie pueda suplantarlas.
 */
@Component
public class JwtFiltroGlobal implements GlobalFilter, Ordered {

  public static final String CABECERA_USUARIO_ID = "X-Usuario-Id";
  public static final String CABECERA_USUARIO_ROL = "X-Usuario-Rol";

  private final SecretKey clave;

  public JwtFiltroGlobal(JwtProperties props) {
    this.clave = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest limpia = exchange.getRequest().mutate()
        .headers(h -> {
          h.remove(CABECERA_USUARIO_ID);
          h.remove(CABECERA_USUARIO_ROL);
        })
        .build();

    String auth = limpia.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    boolean publica = RutasPublicas.esPublica(limpia);

    if (auth == null || !auth.startsWith("Bearer ")) {
      return publica
          ? chain.filter(exchange.mutate().request(limpia).build())
          : noAutorizado(exchange, "Se requiere iniciar sesión");
    }

    Claims claims;
    try {
      claims = Jwts.parser().verifyWith(clave).build()
          .parseSignedClaims(auth.substring(7))
          .getPayload();
    } catch (JwtException | IllegalArgumentException e) {
      // En rutas públicas un token vencido no debe impedir ver el catálogo: sigue como anónimo.
      return publica
          ? chain.filter(exchange.mutate().request(limpia).build())
          : noAutorizado(exchange, "Token inválido o expirado");
    }

    ServerHttpRequest conIdentidad = limpia.mutate()
        .header(CABECERA_USUARIO_ID, claims.getSubject())
        .header(CABECERA_USUARIO_ROL, claims.get("rol", String.class))
        .build();
    return chain.filter(exchange.mutate().request(conIdentidad).build());
  }

  private Mono<Void> noAutorizado(ServerWebExchange exchange, String mensaje) {
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] cuerpo = ("{\"status\":401,\"mensaje\":\"" + mensaje + "\"}").getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = response.bufferFactory().wrap(cuerpo);
    return response.writeWith(Mono.just(buffer));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
