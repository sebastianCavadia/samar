package co.edu.unimagdalena.samar.cuentas.servicio.excepcion;

/** HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

  public RecursoNoEncontradoException(String mensaje) {
    super(mensaje);
  }
}
