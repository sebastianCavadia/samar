package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import java.time.Instant;

/** prestadorValidado es null cuando el usuario no es prestador. */
public record UsuarioResponse(
    Long id,
    String nombre,
    String correo,
    String rol,
    String estado,
    String idioma,
    String telefono,
    Instant ultimoAcceso,
    Boolean prestadorValidado) {
}
