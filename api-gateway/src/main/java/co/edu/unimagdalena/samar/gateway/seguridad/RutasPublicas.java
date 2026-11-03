package co.edu.unimagdalena.samar.gateway.seguridad;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/** Rutas que no exigen token: registro, login, catálogo y cupos disponibles (visitante anónimo). */
final class RutasPublicas {

  private record Ruta(HttpMethod metodo, String patron) {
  }

  private static final AntPathMatcher MATCHER = new AntPathMatcher();

  private static final List<Ruta> PUBLICAS = List.of(
      new Ruta(HttpMethod.POST, "/api/cuentas/auth/**"),
      new Ruta(HttpMethod.GET, "/api/oferta/servicios/**"),
      new Ruta(HttpMethod.GET, "/api/oferta/servicios"),
      new Ruta(HttpMethod.GET, "/api/reservas/disponibilidad"),
      new Ruta(HttpMethod.GET, "/actuator/health"));

  private RutasPublicas() {
  }

  static boolean esPublica(ServerHttpRequest request) {
    String path = request.getPath().value();
    return PUBLICAS.stream()
        .anyMatch(r -> r.metodo().equals(request.getMethod()) && MATCHER.match(r.patron(), path));
  }
}
