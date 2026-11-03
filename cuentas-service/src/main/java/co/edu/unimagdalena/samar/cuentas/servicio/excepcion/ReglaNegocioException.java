package co.edu.unimagdalena.samar.cuentas.servicio.excepcion;

/** Se viola una regla del dominio (correo repetido, documento repetido...). HTTP 409. */
public class ReglaNegocioException extends RuntimeException {

  public ReglaNegocioException(String mensaje) {
    super(mensaje);
  }
}
