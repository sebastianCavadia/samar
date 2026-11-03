package co.edu.unimagdalena.samar.oferta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Reloj inyectable (RF082 "Nuevo" depende de la fecha actual; en pruebas se fija).
 * También habilita la sincronización periódica con cuentas (ADR-13).
 */
@Configuration
@EnableScheduling
public class TiempoConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
