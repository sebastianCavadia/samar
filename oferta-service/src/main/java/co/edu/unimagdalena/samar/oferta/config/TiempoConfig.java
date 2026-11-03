package co.edu.unimagdalena.samar.oferta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Reloj inyectable (RF082 "Nuevo" depende de la fecha actual; en pruebas se fija). */
@Configuration
public class TiempoConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
