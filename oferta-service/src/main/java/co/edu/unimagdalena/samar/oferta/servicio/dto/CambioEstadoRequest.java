package co.edu.unimagdalena.samar.oferta.servicio.dto;

import jakarta.validation.constraints.NotNull;

/** RF032: activar o desactivar sin eliminar. */
public record CambioEstadoRequest(@NotNull Boolean activo) {
}
