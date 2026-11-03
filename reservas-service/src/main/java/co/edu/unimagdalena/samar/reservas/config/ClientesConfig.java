package co.edu.unimagdalena.samar.reservas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

/**
 * Clientes HTTP hacia otros microservicios, con timeouts para que una caída no cuelgue a reservas.
 * También expone un TransactionTemplate: las llamadas remotas se hacen FUERA de la transacción,
 * para no retener una conexión de la base de datos mientras se espera la red.
 */
@Configuration
public class ClientesConfig {

  @Bean
  public RestClient ofertaRestClient(@Value("${samar.servicios.oferta}") String url) {
    return cliente(url);
  }

  @Bean
  public RestClient cuentasRestClient(@Value("${samar.servicios.cuentas}") String url) {
    return cliente(url);
  }

  @Bean
  public TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
    return new TransactionTemplate(tm);
  }

  private static RestClient cliente(String url) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(3));
    return RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }
}
