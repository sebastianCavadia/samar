package co.edu.unimagdalena.samar.reservas.integracion;

/**
 * Avisos al turista o al prestador (RF037, RF039). La capa servicio depende de esta
 * interfaz; hoy la implementa NotificacionService con alertas internas, y un envío por correo
 * se agregaría como otra implementación sin tocar las reglas de negocio.
 */
public interface Notificador {

  void notificar(Long idUsuario, String asunto, String mensaje);
}
