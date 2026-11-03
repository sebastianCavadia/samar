package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** RF001 + RF002. */
public record RegistroTuristaRequest(
    @NotBlank @Size(max = 120) String nombre,
    @NotBlank @Email String correo,
    @NotBlank @Size(min = 8, max = 72) String password,
    String telefono,
    @NotBlank @Pattern(regexp = "CC|CE|PASAPORTE") String tipoDocumento,
    @NotBlank @Size(max = 40) String numeroDocumento,
    String nacionalidad,
    String paisResidencia,
    @Past LocalDate fechaNacimiento,
    boolean discapacidad) {
}
