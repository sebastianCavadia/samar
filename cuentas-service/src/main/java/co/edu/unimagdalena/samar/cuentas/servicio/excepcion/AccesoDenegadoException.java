package co.edu.unimagdalena.samar.cuentas.servicio.excepcion;

/** Cuenta bloqueada/inactiva o rol sin permiso. HTTP 403. */
public class AccesoDenegadoException extends RuntimeException {

  public AccesoDenegadoException(String mensaje) {
    super(mensaje);
  }
}
