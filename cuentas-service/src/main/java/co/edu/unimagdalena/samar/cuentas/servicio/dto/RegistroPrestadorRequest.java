package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

/** RF001 + RF003. idiomasGuia solo aplica a agencias turísticas. */
public record RegistroPrestadorRequest(
    @NotBlank @Size(max = 120) String nombre,
    @NotBlank @Email String correo,
    @NotBlank @Size(min = 8, max = 72) String password,
    String telefono,
    @NotBlank @Pattern(regexp = "HOTEL|RESTAURANTE|AGENCIA_TURISTICA") String tipoPrestador,
    @NotBlank @Size(max = 40) String nit,
    @NotBlank @Size(max = 40) String rnt,
    @NotBlank @Size(max = 200) String direccion,
    String zona,
    Double latitud,
    Double longitud,
    Set<String> idiomasGuia) {
}
