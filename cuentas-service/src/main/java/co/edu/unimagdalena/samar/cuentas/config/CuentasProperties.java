package co.edu.unimagdalena.samar.cuentas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros configurables del servicio (application.yml / variables de entorno).
 *
 * @param jwtSecret          secreto HMAC compartido con el gateway (mínimo 32 bytes)
 * @param jwtExpiracionMin   vigencia del token de acceso
 * @param maxIntentosFallidos RF005: intentos antes de bloquear
 * @param bloqueoMin         RF005: duración del bloqueo temporal
 * @param adminCorreo        administrador inicial que se crea si no existe ninguno
 * @param adminPassword      contraseña del administrador inicial
 */
@ConfigurationProperties(prefix = "samar.cuentas")
public record CuentasProperties(
    String jwtSecret,
    int jwtExpiracionMin,
    int maxIntentosFallidos,
    int bloqueoMin,
    String adminCorreo,
    String adminPassword) {
}
