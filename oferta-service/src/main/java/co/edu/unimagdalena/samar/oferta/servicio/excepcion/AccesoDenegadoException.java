package co.edu.unimagdalena.samar.oferta.servicio.excepcion;

/** Rol sin permiso o recurso de otro usuario. HTTP 403. */
public class AccesoDenegadoException extends RuntimeException {

  public AccesoDenegadoException(String mensaje) {
    super(mensaje);
  }
}
