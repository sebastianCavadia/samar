package co.edu.unimagdalena.samar.gateway.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Secreto compartido con cuentas-service (quien firma los tokens). */
@ConfigurationProperties(prefix = "samar.jwt")
public record JwtProperties(String secret) {
}
