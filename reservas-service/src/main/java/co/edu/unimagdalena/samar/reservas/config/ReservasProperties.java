package co.edu.unimagdalena.samar.reservas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param expiracionSinPagoMin RF056: minutos para pagar antes de que la reserva se cancele sola
 * @param maxDiasApertura      máximo de días que un prestador abre de una vez (evita cargas enormes)
 * @param zonaHoraria          "hoy" se calcula en hora de Colombia, no en UTC
 */
@ConfigurationProperties(prefix = "samar.reservas")
public record ReservasProperties(int expiracionSinPagoMin, int maxDiasApertura, String zonaHoraria) {

  public Duration plazoPago() {
    return Duration.ofMinutes(expiracionSinPagoMin);
  }
}
