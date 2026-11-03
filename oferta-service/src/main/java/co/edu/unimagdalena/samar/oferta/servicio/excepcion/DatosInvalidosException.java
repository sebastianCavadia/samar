package co.edu.unimagdalena.samar.oferta.servicio.excepcion;

/** La petición está mal formada o incompleta para el caso pedido. HTTP 400. */
public class DatosInvalidosException extends RuntimeException {

  public DatosInvalidosException(String mensaje) {
    super(mensaje);
  }
}
