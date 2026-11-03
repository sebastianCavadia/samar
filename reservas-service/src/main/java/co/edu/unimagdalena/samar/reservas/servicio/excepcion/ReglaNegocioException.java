package co.edu.unimagdalena.samar.reservas.servicio.excepcion;

/** Se viola una regla del dominio. HTTP 409. */
public class ReglaNegocioException extends RuntimeException {

  public ReglaNegocioException(String mensaje) {
    super(mensaje);
  }
}
