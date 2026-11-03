package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * prestadorValidado es null cuando el usuario no es prestador.
 * fechaNacimiento solo aplica a turistas (reservas la usa para la edad mínima, RF065).
 */
public record UsuarioResponse(
    Long id,
    String nombre,
    String correo,
    String rol,
    String estado,
    String idioma,
    String telefono,
    Instant ultimoAcceso,
    Boolean prestadorValidado,
    LocalDate fechaNacimiento) {
}
