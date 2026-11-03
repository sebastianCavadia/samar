package co.edu.unimagdalena.samar.oferta.servicio.excepcion;

/** Un microservicio del que depende la operación no respondió. HTTP 503. */
public class ServicioNoDisponibleException extends RuntimeException {

  public ServicioNoDisponibleException(String mensaje) {
    super(mensaje);
  }
}
