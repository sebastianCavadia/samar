package co.edu.unimagdalena.samar.reservas.servicio.dto;

/**
 * Quién hace la petición, según el JWT que validó el gateway.
 * La capa presentación lo construye y la capa servicio decide los permisos.
 */
public record Identidad(Long usuarioId, String rol) {

  public boolean esTurista() {
    return "TURISTA".equals(rol);
  }

  public boolean esPrestador() {
    return "PRESTADOR".equals(rol);
  }

  public boolean esAdministrador() {
    return "ADMINISTRADOR".equals(rol);
  }
}
