package co.edu.unimagdalena.samar.cuentas.servicio.excepcion;

/** HTTP 401. */
public class CredencialesInvalidasException extends RuntimeException {

  public CredencialesInvalidasException(String mensaje) {
    super(mensaje);
  }
}
