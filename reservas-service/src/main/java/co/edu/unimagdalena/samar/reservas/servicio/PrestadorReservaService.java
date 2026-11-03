package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.config.ReservasProperties;
import co.edu.unimagdalena.samar.reservas.integracion.Notificador;
import co.edu.unimagdalena.samar.reservas.modelo.*;
import co.edu.unimagdalena.samar.reservas.persistencia.ReservaRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Casos de uso del prestador sobre las reservas que recibe: RF038, RF044 y consulta. */
@Service
public class PrestadorReservaService {

  private final ReservaRepository reservas;
  private final Notificador notificador;
  private final Cierres cierres;
  private final ReservasProperties props;

  public PrestadorReservaService(ReservaRepository reservas, Notificador notificador, Cierres cierres,
      ReservasProperties props) {
    this.reservas = reservas;
    this.notificador = notificador;
    this.cierres = cierres;
    this.props = props;
  }

  @Transactional(readOnly = true)
  public List<Respuestas.Reserva> recibidas(Identidad actor, String estado) {
    exigirPrestador(actor);
    List<Reserva> lista;
    if (estado == null || estado.isBlank()) {
      lista = reservas.recibidasPor(actor.usuarioId());
    } else {
      EstadoReserva filtro;
      try {
        filtro = EstadoReserva.valueOf(estado.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new DatosInvalidosException("Estado no válido: " + estado);
      }
      lista = reservas.recibidasPorEstado(actor.usuarioId(), filtro);
    }
    return lista.stream().map(r -> ReservaMapper.aRespuesta(r, props.plazoPago())).toList();
  }

  /** RF038 + RF044: solo se confirma una reserva con el pago aprobado. */
  @Transactional
  public Respuestas.Reserva confirmar(Identidad actor, Long id) {
    Reserva r = cargarPropia(actor, id);
    Cierres.exigirTransicion(r, EstadoReserva.CONFIRMADA, "confirmar");
    if (!r.tienePagoAprobado()) {
      throw new ReglaNegocioException("Solo puedes confirmar una reserva con el pago aprobado (RF044)");
    }
    r.cambiarEstado(EstadoReserva.CONFIRMADA, actor.usuarioId(), "Confirmada por el prestador");
    reservas.flush();
    notificador.notificar(r.getIdTurista(), "Reserva confirmada",
        "Tu reserva #" + r.getId() + " de " + r.detallePrincipal().getNombreServicio() + " está confirmada"); // RF039
    return ReservaMapper.aRespuesta(r, props.plazoPago());
  }

  /** RF038: rechazar libera los cupos y devuelve el pago si lo hubo. */
  @Transactional
  public Respuestas.Reserva rechazar(Identidad actor, Long id, String motivo) {
    Reserva r = cargarPropia(actor, id);
    Cierres.exigirTransicion(r, EstadoReserva.RECHAZADA, "rechazar");
    String texto = motivo == null || motivo.isBlank() ? "Rechazada por el prestador" : motivo.trim();
    cierres.cerrar(r, EstadoReserva.RECHAZADA, actor.usuarioId(), texto);
    reservas.flush();
    notificador.notificar(r.getIdTurista(), "Reserva rechazada", "Tu reserva #" + r.getId() + " fue rechazada: " + texto);
    return ReservaMapper.aRespuesta(r, props.plazoPago());
  }

  /** RF044: el prestador revisa el comprobante. Aprobarlo no confirma la reserva: eso es un paso aparte. */
  @Transactional
  public Respuestas.Reserva validarPago(Identidad actor, Long id, boolean aprobado) {
    Reserva r = cargarPropia(actor, id);
    if (r.getEstado() != EstadoReserva.PENDIENTE) {
      throw new ReglaNegocioException("Solo se revisan pagos de reservas pendientes");
    }
    Pago pago = r.getPagos().stream()
        .filter(p -> p.getEstado() == EstadoPago.PENDIENTE)
        .findFirst()
        .orElseThrow(() -> new ReglaNegocioException("Esta reserva no tiene un comprobante por revisar"));
    pago.setEstado(aprobado ? EstadoPago.APROBADO : EstadoPago.RECHAZADO);
    reservas.flush();
    notificador.notificar(r.getIdTurista(), aprobado ? "Pago aprobado" : "Comprobante rechazado",
        "Reserva #" + r.getId() + (aprobado ? ": el prestador aprobó tu pago" : ": revisa el comprobante y envíalo de nuevo"));
    return ReservaMapper.aRespuesta(r, props.plazoPago());
  }

  private Reserva cargarPropia(Identidad actor, Long id) {
    exigirPrestador(actor);
    Reserva r = reservas.buscarParaModificar(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
    if (!r.esAtendidaPor(actor.usuarioId())) {
      throw new AccesoDenegadoException("Esta reserva es de otro prestador");
    }
    return r;
  }

  private static void exigirPrestador(Identidad actor) {
    if (!actor.esPrestador()) {
      throw new AccesoDenegadoException("Solo los prestadores gestionan reservas recibidas");
    }
  }
}
