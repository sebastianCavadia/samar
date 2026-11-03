package co.edu.unimagdalena.samar.cuentas.integracion;

/**
 * Salida hacia el servicio de correo (nodo SMTP del diagrama de despliegue).
 * La capa servicio depende de esta interfaz, no de la tecnología concreta.
 */
public interface CorreoClient {

  void enviar(String destinatario, String asunto, String cuerpo);
}
