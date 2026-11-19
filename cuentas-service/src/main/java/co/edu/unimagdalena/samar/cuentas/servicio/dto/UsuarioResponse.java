package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Perfil propio y consulta interna de otros servicios.
 * prestadorValidado, estadoValidacion y motivoRechazo solo aplican a prestadores (RF095);
 * fechaNacimiento solo a turistas (reservas la usa para la edad mínima, RF065);
 * areaResponsable solo a administradores (RF099); direccion y zona solo a prestadores (RF008).
 * tieneFoto solo se calcula en el perfil propio (en las demás respuestas es nulo).
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
    LocalDate fechaNacimiento,
    String estadoValidacion,
    String motivoRechazo,
    String areaResponsable,
    String direccion,
    String zona,
    Boolean tieneFoto) {
}
