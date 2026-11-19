package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF008 y RF009: lo que el usuario cambia de su propia cuenta. */
public final class Perfil {

  private Perfil() {
  }

  /** direccion y zona solo aplican a prestadores ("ubicación si es empresarial"). */
  public record Editar(@NotBlank @Size(max = 120) String nombre, @Size(max = 40) String telefono,
      @Size(max = 200) String direccion, @Size(max = 80) String zona) {
  }

  /** RF009: la nueva sigue las mismas reglas que al registrarse. */
  public record CambiarPassword(@NotBlank String actual, @NotBlank @Size(min = 8, max = 72) String nueva) {
  }

  public record Foto(byte[] contenido, String tipo) {
  }
}
