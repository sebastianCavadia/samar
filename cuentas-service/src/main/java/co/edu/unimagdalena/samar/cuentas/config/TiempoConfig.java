package co.edu.unimagdalena.samar.cuentas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Reloj inyectable: en las pruebas se reemplaza por uno fijo (p. ej. para el bloqueo RF005). */
@Configuration
public class TiempoConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
