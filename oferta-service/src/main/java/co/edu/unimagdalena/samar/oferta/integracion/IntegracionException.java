package co.edu.unimagdalena.samar.oferta.integracion;

/** Un servicio externo no respondió o respondió con error. La capa servicio decide qué hacer. */
public class IntegracionException extends RuntimeException {

  public IntegracionException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
