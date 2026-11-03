package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import java.time.Instant;

public record SesionResponse(String token, Instant expiraEn, UsuarioResponse usuario) {
}
