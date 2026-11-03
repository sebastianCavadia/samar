package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.integracion.CuentasClient;
import co.edu.unimagdalena.samar.oferta.integracion.IntegracionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ADR-13: cada minuto (configurable) pregunta a cuentas qué prestadores están desactivados.
 * La dependencia va de oferta hacia cuentas (ADR-08): cuentas nunca le avisa a oferta.
 * Si cuentas no responde se conserva el último estado conocido y se reintenta en la siguiente pasada.
 */
@Component
class SincronizacionPrestadores {

  private static final Logger log = LoggerFactory.getLogger(SincronizacionPrestadores.class);

  private final CuentasClient cuentas;
  private final SuspensionPrestadoresService suspension;

  SincronizacionPrestadores(CuentasClient cuentas, SuspensionPrestadoresService suspension) {
    this.cuentas = cuentas;
    this.suspension = suspension;
  }

  @Scheduled(fixedDelayString = "${samar.oferta.sincronizacion-ms:60000}", initialDelayString = "${samar.oferta.sincronizacion-ms:60000}")
  void sincronizar() {
    try {
      var r = suspension.aplicar(cuentas.prestadoresSuspendidos());
      if (r.suspendidos() > 0 || r.reactivados() > 0) {
        log.info("Sincronización con cuentas: {} servicio(s) ocultos, {} visibles de nuevo", r.suspendidos(), r.reactivados());
      }
    } catch (IntegracionException e) {
      log.warn("No se pudo sincronizar con cuentas-service ({}); se reintenta en la próxima pasada", e.getMessage());
    }
  }
}
