package co.edu.unimagdalena.samar.reservas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj en hora de Colombia: a las 8 p. m. en Santa Marta ya es "mañana" en UTC, y eso
 * rompería la validación de fechas (RF066) y el paso a COMPLETADA (RF049).
 * También habilita las tareas programadas (expiración y completado).
 */
@Configuration
@EnableScheduling
public class TiempoConfig {

  @Bean
  public Clock clock(ReservasProperties props) {
    return Clock.system(ZoneId.of(props.zonaHoraria()));
  }
}
