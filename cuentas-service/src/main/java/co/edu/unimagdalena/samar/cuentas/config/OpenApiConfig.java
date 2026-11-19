package co.edu.unimagdalena.samar.cuentas.config;

import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * RNF-034: documentación OpenAPI. Se ve en /docs/cuentas/swagger-ui/index.html a través del gateway.
 * "Try it out" llama a /api/... por el gateway, así que se autentica con el JWT del login (botón Authorize).
 */
@Configuration
public class OpenApiConfig {

  static {
    // El actor lo arma el servidor con las cabeceras del gateway: no es un parámetro que envíe el cliente.
    SpringDocUtils.getConfig().addRequestWrapperToIgnore(Identidad.class);
  }

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info().title("SAMAR · cuentas-service").version("0.1.0")
            .description("Registro, inicio de sesión (emite el JWT), perfil y administración de usuarios."))
        .servers(List.of(new Server().url("/").description("API Gateway")))
        .components(new Components().addSecuritySchemes("jwt", new SecurityScheme()
            .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("jwt"));
  }
}
