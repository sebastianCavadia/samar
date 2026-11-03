package co.edu.unimagdalena.samar.cuentas.servicio.excepcion;

/** La petición está mal formada (p. ej. un filtro con un valor que no existe). HTTP 400. */
public class DatosInvalidosException extends RuntimeException {

  public DatosInvalidosException(String mensaje) {
    super(mensaje);
  }
}
