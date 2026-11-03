package co.edu.unimagdalena.samar.reservas.integracion;

/**
 * Avisos al turista o al prestador (RF037, RF039). La capa servicio depende de esta
 * interfaz; la implementación real (correo SMTP o alertas internas) se cambia sin tocarla.
 */
public interface Notificador {

  void notificar(Long idUsuario, String asunto, String mensaje);
}
