package co.edu.unimagdalena.samar.cuentas.servicio.dto;

/**
 * Quién hace la petición, según el JWT que validó el gateway.
 * La capa presentación lo construye y la capa servicio decide los permisos.
 */
public record Identidad(Long usuarioId, String rol) {

  public boolean esAdministrador() {
    return "ADMINISTRADOR".equals(rol);
  }
}
