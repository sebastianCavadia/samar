package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.config.ReservasProperties;
import co.edu.unimagdalena.samar.reservas.integracion.IntegracionException;
import co.edu.unimagdalena.samar.reservas.integracion.Notificador;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient.CalificacionRegistrada;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient.NuevaCalificacion;
import co.edu.unimagdalena.samar.reservas.modelo.DetalleReserva;
import co.edu.unimagdalena.samar.reservas.modelo.EstadoReserva;
import co.edu.unimagdalena.samar.reservas.modelo.Reserva;
import co.edu.unimagdalena.samar.reservas.persistencia.ReservaRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Solicitudes;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * RF075 y RF076 (ADR-11): el turista califica a través de reservas, que es quien sabe si la reserva
 * está COMPLETADA. La calificación se guarda en oferta; aquí queda una copia de la puntuación.
 */
@Service
public class CalificacionReservaService {

  private final ReservaRepository reservas;
  private final OfertaClient oferta;
  private final Notificador notificador;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final ReservasProperties props;

  public CalificacionReservaService(ReservaRepository reservas, OfertaClient oferta, Notificador notificador,
      TransactionTemplate tx, Clock clock, ReservasProperties props) {
    this.reservas = reservas;
    this.oferta = oferta;
    this.notificador = notificador;
    this.tx = tx;
    this.clock = clock;
    this.props = props;
  }

  private record Datos(Long idServicio, Long idPrestador, String nombreServicio, String nombreTurista) {
  }

  /**
   * Tres pasos, como al reservar (ADR-12): 1) lectura que comprueba la reserva, 2) registro en oferta
   * FUERA de la transacción, 3) transacción corta que guarda la copia local.
   * Oferta es idempotente por reserva. Si ya tenía la calificación (doble clic, un timeout tras el que
   * oferta sí la guardó, o reservas falló después del paso 2), la copia local se repara con la
   * puntuación que quedó en oferta y se responde 409.
   */
  public Respuestas.Reserva calificar(Identidad actor, Long id, Solicitudes.Calificar req) {
    if (!actor.esTurista()) {
      throw new AccesoDenegadoException("Solo los turistas califican servicios");
    }
    Datos datos = tx.execute(estado -> {
      Reserva r = reservas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
      if (!r.esDelTurista(actor.usuarioId())) {
        throw new AccesoDenegadoException("Solo quien hizo la reserva puede calificarla");
      }
      exigirCalificable(r);
      DetalleReserva d = r.detallePrincipal();
      return new Datos(d.getIdServicio(), d.getIdPrestador(), d.getNombreServicio(), r.getNombreTurista());
    });

    String comentario = (req.comentario() == null || req.comentario().isBlank()) ? null : req.comentario().trim();
    CalificacionRegistrada registrada;
    try {
      registrada = oferta.registrarCalificacion(new NuevaCalificacion(id, datos.idServicio(), actor.usuarioId(),
              datos.nombreTurista(), req.puntuacion(), comentario))
          .orElseThrow(() -> new RecursoNoEncontradoException("El servicio ya no existe"));
    } catch (IntegracionException e) {
      // Un timeout no significa que oferta no la recibió: puede guardarla después. Reintentar es seguro.
      throw new ServicioNoDisponibleException("No pudimos confirmar tu calificación. Intenta de nuevo en unos minutos: "
          + "si alcanzó a registrarse, no se contará dos veces");
    }

    Respuestas.Reserva respuesta = tx.execute(estado -> {
      Reserva r = reservas.buscarParaModificar(id)
          .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
      if (r.puedeCalificarse()) {
        r.registrarCalificacion(registrada.puntuacion(), clock.instant());
        reservas.flush();
      }
      return ReservaMapper.aRespuesta(r, props.plazoPago());
    });
    if (!registrada.nueva()) {
      throw new ReglaNegocioException("Tu calificación ya había quedado registrada con " + registrada.puntuacion()
          + (registrada.puntuacion() == 1 ? " estrella" : " estrellas") + ": no se cuenta dos veces (RF076)");
    }

    notificador.notificar(datos.idPrestador(), "Nueva calificación",
        datos.nombreServicio() + " recibió " + registrada.puntuacion() + " estrellas (reserva #" + id + ")");
    return respuesta;
  }

  private static void exigirCalificable(Reserva r) {
    if (r.getCalificacion() != null) {
      throw new ReglaNegocioException("Ya calificaste esta reserva (RF076)");
    }
    if (r.getEstado() != EstadoReserva.COMPLETADA) {
      throw new ReglaNegocioException(
          "Podrás calificar cuando el servicio se haya prestado: la reserva debe estar COMPLETADA (RF076)");
    }
  }
}
