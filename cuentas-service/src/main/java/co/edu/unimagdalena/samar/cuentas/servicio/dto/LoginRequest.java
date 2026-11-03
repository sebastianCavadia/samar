package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String correo, @NotBlank String password) {
}
