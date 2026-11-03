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
    return builder.routes()
        .route("cuentas", r -> r.path("/api/cuentas/**").uri(cuentas))
        .route("oferta", r -> r.path("/api/oferta/**").uri(oferta))
        .route("reservas", r -> r.path("/api/reservas/**").uri(reservas))
        .route("recomendaciones", r -> r.path("/api/recomendaciones/**").uri(recomendaciones))
        .build();
  }
}
