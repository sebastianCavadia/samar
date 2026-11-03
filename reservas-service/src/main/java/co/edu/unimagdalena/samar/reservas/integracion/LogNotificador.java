package co.edu.unimagdalena.samar.reservas.integracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implementación de desarrollo: escribe la notificación en el log. */
@Component
class LogNotificador implements Notificador {

  private static final Logger log = LoggerFactory.getLogger(LogNotificador.class);

  @Override
  public void notificar(Long idUsuario, String asunto, String mensaje) {
    log.info("[NOTIFICACION] usuario={} asunto='{}' mensaje='{}'", idUsuario, asunto, mensaje);
  }
}
