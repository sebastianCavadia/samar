package co.edu.unimagdalena.samar.reservas.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.LongPredicate;

/** Ejecuta cada minuto (configurable) la expiración (RF056) y el completado (RF049). */
@Component
class TareasProgramadas {

  private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

  private final ProcesosAutomaticosService procesos;

  TareasProgramadas(ProcesosAutomaticosService procesos) {
    this.procesos = procesos;
  }

  @Scheduled(fixedDelayString = "${samar.reservas.tarea-ms:60000}", initialDelayString = "${samar.reservas.tarea-ms:60000}")
  void ejecutar() {
    procesar(procesos.idsPorExpirar(), procesos::expirar, "expiradas por falta de pago");
    procesar(procesos.idsPorCompletar(), procesos::completar, "completadas");
  }

  private static void procesar(List<Long> ids, LongPredicate accion, String resultado) {
    int procesadas = 0;
    for (Long id : ids) {
      try {
        if (accion.test(id)) {
          procesadas++;
        }
      } catch (ObjectOptimisticLockingFailureException e) {
        // Alguien la modificó al mismo tiempo (p. ej. el turista pagó): se revisa en la próxima pasada.
        log.debug("La reserva {} cambió mientras se procesaba", id);
      } catch (RuntimeException e) {
        log.warn("No se pudo procesar la reserva {}: {}", id, e.getMessage());
      }
    }
    if (procesadas > 0) {
      log.info("Tarea automática: {} reserva(s) {}", procesadas, resultado);
    }
  }
}
