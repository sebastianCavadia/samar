package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.integracion.Notificador;
import co.edu.unimagdalena.samar.reservas.modelo.Notificacion;
import co.edu.unimagdalena.samar.reservas.persistencia.NotificacionRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * RF037 y RF039: alertas internas. Implementa Notificador, así que las reglas de negocio que avisan
 * (crear, pagar, confirmar, cancelar, expirar, completar) no cambiaron al pasar del log a la base.
 */
@Service
public class NotificacionService implements Notificador {

  private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

  private final NotificacionRepository notificaciones;
  private final Clock clock;

  public NotificacionService(NotificacionRepository notificaciones, Clock clock) {
    this.notificaciones = notificaciones;
    this.clock = clock;
  }

  /**
   * Se une a la transacción de quien avisa: si la operación se deshace, el aviso también.
   * Sin transacción abierta (por ejemplo, después de crear una reserva) abre la suya.
   */
  @Override
  @Transactional
  public void notificar(Long idUsuario, String asunto, String mensaje) {
    if (idUsuario == null) {
      return;
    }
    notificaciones.save(new Notificacion(idUsuario, recortar(asunto, 120), recortar(mensaje, 500), clock.instant()));
    log.info("[NOTIFICACION] usuario={} asunto='{}'", idUsuario, asunto);
  }

  @Transactional(readOnly = true)
  public Respuestas.Notificaciones mias(Identidad actor) {
    var recientes = notificaciones.findTop20ByIdUsuarioOrderByCreadoEnDescIdDesc(actor.usuarioId()).stream()
        .map(n -> new Respuestas.NotificacionInfo(n.getId(), n.getAsunto(), n.getMensaje(), n.isLeida(), n.getCreadoEn()))
        .toList();
    return new Respuestas.Notificaciones(notificaciones.countByIdUsuarioAndLeidaFalse(actor.usuarioId()), recientes);
  }

  @Transactional
  public Respuestas.Notificaciones marcarTodasLeidas(Identidad actor) {
    notificaciones.marcarTodasLeidas(actor.usuarioId());
    return mias(actor);
  }

  /** Una notificación ajena responde 404, igual que una inexistente: no revela que existe. */
  @Transactional
  public Respuestas.Notificaciones marcarLeida(Identidad actor, Long id) {
    notificaciones.findById(id)
        .filter(n -> n.esDe(actor.usuarioId()))
        .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada"))
        .marcarLeida();
    return mias(actor);
  }

  private static String recortar(String texto, int max) {
    String limpio = texto == null ? "" : texto.trim();
    return limpio.length() <= max ? limpio : limpio.substring(0, max - 1) + "…";
  }
}
