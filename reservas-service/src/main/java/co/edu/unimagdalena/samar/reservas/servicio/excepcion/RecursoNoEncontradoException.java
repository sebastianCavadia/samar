package co.edu.unimagdalena.samar.reservas.servicio.excepcion;

/** HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

  public RecursoNoEncontradoException(String mensaje) {
    super(mensaje);
  }
}
