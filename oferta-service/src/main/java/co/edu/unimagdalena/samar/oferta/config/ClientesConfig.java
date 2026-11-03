package co.edu.unimagdalena.samar.oferta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Clientes HTTP hacia otros microservicios (con timeouts: si cuentas cae, oferta no se cuelga). */
@Configuration
public class ClientesConfig {

  @Bean
  public RestClient cuentasRestClient(@Value("${samar.servicios.cuentas}") String url) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(3));
    return RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }
}
