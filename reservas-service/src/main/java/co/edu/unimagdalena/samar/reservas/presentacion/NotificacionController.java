package co.edu.unimagdalena.samar.reservas.presentacion;

import co.edu.unimagdalena.samar.reservas.servicio.NotificacionService;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF037 y RF039: las alertas internas del usuario que inició sesión (la campana del frontend). */
@RestController
@RequestMapping("/api/reservas/notificaciones")
public class NotificacionController {

  private final NotificacionService notificaciones;

  public NotificacionController(NotificacionService notificaciones) {
    this.notificaciones = notificaciones;
  }

  @GetMapping
  public Respuestas.Notificaciones mias(Identidad actor) {
    return notificaciones.mias(actor);
  }

  @PostMapping("/leidas")
  public Respuestas.Notificaciones marcarTodasLeidas(Identidad actor) {
    return notificaciones.marcarTodasLeidas(actor);
  }

  @PostMapping("/{id}/leida")
  public Respuestas.Notificaciones marcarLeida(Identidad actor, @PathVariable Long id) {
    return notificaciones.marcarLeida(actor, id);
  }
}
