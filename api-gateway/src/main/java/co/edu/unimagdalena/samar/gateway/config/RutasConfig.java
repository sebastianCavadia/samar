package co.edu.unimagdalena.samar.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Punto único de entrada: cada prefijo /api/{servicio}/** se redirige a su microservicio.
 * Los clientes (SPA) nunca conocen la ubicación real de los servicios.
 */
@Configuration
public class RutasConfig {

  @Bean
  public RouteLocator rutas(RouteLocatorBuilder builder,
      @Value("${samar.servicios.cuentas}") String cuentas,
      @Value("${samar.servicios.oferta}") String oferta,
      @Value("${samar.servicios.reservas}") String reservas,
      @Value("${samar.servicios.recomendaciones}") String recomendaciones) {
    var rutas = builder.routes()
        .route("cuentas", r -> r.path("/api/cuentas/**").uri(cuentas))
        .route("oferta", r -> r.path("/api/oferta/**").uri(oferta))
        .route("reservas", r -> r.path("/api/reservas/**").uri(reservas))
        .route("recomendaciones", r -> r.path("/api/recomendaciones/**").uri(recomendaciones));

    // RNF-034: /docs/{servicio}/... muestra su Swagger. Solo se enrutan las rutas de la documentación,
    // así /internal/** sigue sin poder alcanzarse desde afuera. StripPrefix(2) quita "/docs/{servicio}";
    // cada servicio declara ese prefijo en springdoc.swagger-ui para que la página pida bien sus datos.
    for (String[] servicio : new String[][] {{"cuentas", cuentas}, {"oferta", oferta}, {"reservas", reservas}}) {
      String base = "/docs/" + servicio[0];
      rutas = rutas.route("docs-" + servicio[0], r -> r
          .path(base + "/v3/api-docs/**", base + "/swagger-ui/**", base + "/swagger-ui.html")
          .filters(f -> f.stripPrefix(2))
          .uri(servicio[1]));
    }
    return rutas.build();
  }
}
