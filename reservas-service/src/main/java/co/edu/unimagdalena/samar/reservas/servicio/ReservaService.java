package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.config.ReservasProperties;
import co.edu.unimagdalena.samar.reservas.integracion.CuentasClient;
import co.edu.unimagdalena.samar.reservas.integracion.CuentasClient.UsuarioRemoto;
import co.edu.unimagdalena.samar.reservas.integracion.IntegracionException;
import co.edu.unimagdalena.samar.reservas.integracion.Notificador;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient.ServicioRemoto;
import co.edu.unimagdalena.samar.reservas.modelo.*;
import co.edu.unimagdalena.samar.reservas.persistencia.DisponibilidadRepository;
import co.edu.unimagdalena.samar.reservas.persistencia.PagoRepository;
import co.edu.unimagdalena.samar.reservas.persistencia.ReservaRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Solicitudes;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Casos de uso del turista: reservar (RF036), pagar (RF043/RF044), cancelar (RF040) y consultar (RF041). */
@Service
public class ReservaService {

  private final ReservaRepository reservas;
  private final DisponibilidadRepository disponibilidades;
  private final PagoRepository pagos;
  private final OfertaClient oferta;
  private final CuentasClient cuentas;
  private final Notificador notificador;
  private final Cierres cierres;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final ReservasProperties props;

  public ReservaService(ReservaRepository reservas, DisponibilidadRepository disponibilidades, PagoRepository pagos,
      OfertaClient oferta, CuentasClient cuentas, Notificador notificador, Cierres cierres,
      TransactionTemplate tx, Clock clock, ReservasProperties props) {
    this.reservas = reservas;
    this.disponibilidades = disponibilidades;
    this.pagos = pagos;
    this.oferta = oferta;
    this.cuentas = cuentas;
    this.notificador = notificador;
    this.cierres = cierres;
    this.tx = tx;
    this.clock = clock;
    this.props = props;
  }

  /**
   * RF036 con RF034, RF047, RF064, RF065 y RF066. Tres pasos:
   * 1) validaciones locales, 2) consultas a oferta y cuentas FUERA de la transacción,
   * 3) transacción corta que ocupa los cupos de forma atómica y guarda la reserva.
   */
  public Respuestas.Reserva crear(Identidad actor, Solicitudes.CrearReserva req) {
    if (!actor.esTurista()) {
      throw new AccesoDenegadoException("Solo los turistas pueden hacer reservas");
    }
    Disponibilidad disp = disponibilidades.findById(req.idDisponibilidad())
        .orElseThrow(() -> new RecursoNoEncontradoException("Esa fecha no existe"));
    if (!disp.isActivo()) {
      throw new ReglaNegocioException("Esta fecha no está disponible para reservar");
    }
    if (Fechas.yaPaso(disp, clock)) {
      throw new ReglaNegocioException("No se puede reservar una fecha u hora que ya pasó (RF066)");
    }

    ServicioRemoto servicio = consultarServicio(disp.getIdServicio());
    if (!servicio.activo()) {
      throw new ReglaNegocioException("Este servicio no está disponible en este momento");
    }
    if (!servicio.admiteReservas()) {
      throw new ReglaNegocioException("Este servicio no necesita reserva: atiende por orden de llegada");
    }
    int personas = req.personas();
    if (servicio.esHospedaje() && servicio.capacidadPersonas() != null && personas > servicio.capacidadPersonas()) {
      throw new ReglaNegocioException("Este alojamiento admite máximo " + servicio.capacidadPersonas() + " personas (RF064)");
    }
    UsuarioRemoto turista = consultarTurista(actor.usuarioId(), servicio.edadMinima() != null);
    if (servicio.edadMinima() != null) {
      exigirEdad(turista, servicio.edadMinima(), disp.getFecha());
    }
    int cantidad = servicio.esHospedaje() ? 1 : personas; // hospedaje: 1 habitación; resto: 1 cupo por persona

    Respuestas.Reserva creada = tx.execute(estado -> {
      if (disponibilidades.ocuparCupos(disp.getId(), cantidad) == 0) {
        throw new ReglaNegocioException("No hay cupos suficientes para esta fecha. Prueba otra fecha o menos personas");
      }
      Reserva r = new Reserva(actor.usuarioId(), turista.nombre(), personas, servicio.moneda(), actor.usuarioId());
      r.agregarDetalle(new DetalleReserva(disponibilidades.getReferenceById(disp.getId()), servicio.id(),
          servicio.idPrestador(), servicio.nombre(), servicio.tipo(), cantidad, servicio.precioBase()));
      return ReservaMapper.aRespuesta(reservas.saveAndFlush(r), props.plazoPago());
    });

    notificador.notificar(servicio.idPrestador(), "Nueva solicitud de reserva",
        "Reserva #" + creada.id() + " de " + servicio.nombre() + " para el " + disp.getFecha()); // RF037
    return creada;
  }

  /** RF041: historial completo; el frontend lo agrupa en activas, completadas, rechazadas y canceladas. */
  @Transactional(readOnly = true)
  public List<Respuestas.Reserva> misReservas(Identidad actor) {
    if (!actor.esTurista()) {
      throw new AccesoDenegadoException("Solo los turistas tienen reservas propias");
    }
    return reservas.findByIdTuristaOrderByCreadoEnDesc(actor.usuarioId()).stream()
        .map(r -> ReservaMapper.aRespuesta(r, props.plazoPago()))
        .toList();
  }

  /** La ve el turista que reservó, el prestador que la atiende o un administrador. */
  @Transactional(readOnly = true)
  public Respuestas.Reserva detalle(Identidad actor, Long id) {
    Reserva r = reservas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
    if (!r.esDelTurista(actor.usuarioId()) && !r.esAtendidaPor(actor.usuarioId()) && !actor.esAdministrador()) {
      throw new AccesoDenegadoException("No tienes acceso a esta reserva");
    }
    return ReservaMapper.aRespuesta(r, props.plazoPago());
  }

  /**
   * RF043: pago en línea (simulado en esta versión: se aprueba al instante, sin datos de tarjeta).
   * RF044: comprobante de transferencia que el prestador revisa después.
   */
  @Transactional
  public Respuestas.Reserva pagar(Identidad actor, Long id, Solicitudes.Pagar req) {
    Reserva r = cargarParaModificar(id);
    if (!r.esDelTurista(actor.usuarioId())) {
      throw new AccesoDenegadoException("Solo quien hizo la reserva puede pagarla");
    }
    if (r.getEstado() != EstadoReserva.PENDIENTE) {
      throw new ReglaNegocioException("Solo se puede pagar una reserva pendiente");
    }
    r.pagoVigente().ifPresent(p -> {
      throw new ReglaNegocioException(p.getEstado() == EstadoPago.APROBADO
          ? "Esta reserva ya está pagada"
          : "Ya enviaste un comprobante: espera a que el prestador lo revise");
    });
    if (clock.instant().isAfter(r.getCreadoEn().plus(props.plazoPago()))) {
      throw new ReglaNegocioException("El plazo para pagar venció: la reserva se cancelará y los cupos se liberarán (RF056)");
    }

    MedioPago medio = MedioPago.valueOf(req.medio());
    Pago pago;
    if (medio == MedioPago.PASARELA) {
      String referencia = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
      pago = new Pago(referencia, medio, EstadoPago.APROBADO, r.getTotal());
    } else {
      if (req.referencia() == null || req.referencia().isBlank()) {
        throw new DatosInvalidosException("Escribe el número del comprobante o de la transferencia");
      }
      String referencia = req.referencia().trim();
      if (pagos.existsByReferencia(referencia)) {
        throw new ReglaNegocioException("Ese número de comprobante ya fue registrado");
      }
      pago = new Pago(referencia, medio, EstadoPago.PENDIENTE, r.getTotal());
    }
    r.registrarPago(pago);
    reservas.flush();

    Long prestador = r.detallePrincipal().getIdPrestador();
    notificador.notificar(prestador, medio == MedioPago.PASARELA ? "Reserva pagada" : "Comprobante por revisar",
        "Reserva #" + r.getId() + (medio == MedioPago.PASARELA ? ": ya puedes confirmarla" : ": revisa el comprobante " + pago.getReferencia()));
    return ReservaMapper.aRespuesta(r, props.plazoPago());
  }

  /** RF040: cancela el turista o el prestador; libera cupos y reembolsa si estaba pagada (RF045). */
  @Transactional
  public Respuestas.Reserva cancelar(Identidad actor, Long id, String motivo) {
    Reserva r = cargarParaModificar(id);
    boolean esTurista = r.esDelTurista(actor.usuarioId());
    if (!esTurista && !r.esAtendidaPor(actor.usuarioId())) {
      throw new AccesoDenegadoException("No puedes cancelar esta reserva");
    }
    Cierres.exigirTransicion(r, EstadoReserva.CANCELADA, "cancelar");
    if (r.detallePrincipal().getDisponibilidad().getFecha().isBefore(LocalDate.now(clock))) {
      throw new ReglaNegocioException("La fecha del servicio ya pasó");
    }
    cierres.cerrar(r, EstadoReserva.CANCELADA, actor.usuarioId(), motivo.trim());
    reservas.flush();

    Long otraParte = esTurista ? r.detallePrincipal().getIdPrestador() : r.getIdTurista();
    notificador.notificar(otraParte, "Reserva cancelada", "La reserva #" + r.getId() + " fue cancelada: " + motivo.trim());
    return ReservaMapper.aRespuesta(r, props.plazoPago());
  }

  private Reserva cargarParaModificar(Long id) {
    return reservas.buscarParaModificar(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
  }

  private ServicioRemoto consultarServicio(Long idServicio) {
    try {
      return oferta.buscarServicio(idServicio)
          .orElseThrow(() -> new RecursoNoEncontradoException("El servicio ya no existe"));
    } catch (IntegracionException e) {
      throw new ServicioNoDisponibleException("No pudimos consultar el servicio. Intenta de nuevo en unos minutos");
    }
  }

  /**
   * Si cuentas no responde, se sigue reservando (degradación controlada) salvo que haga falta la
   * fecha de nacimiento para validar la edad mínima: ahí no hay forma segura de continuar.
   */
  private UsuarioRemoto consultarTurista(Long id, boolean necesitaFechaNacimiento) {
    UsuarioRemoto turista;
    try {
      turista = cuentas.buscarUsuario(id).orElseThrow(() -> new AccesoDenegadoException("Tu cuenta no existe"));
    } catch (IntegracionException e) {
      if (necesitaFechaNacimiento) {
        throw new ServicioNoDisponibleException("No pudimos verificar tu edad en este momento. Intenta en unos minutos");
      }
      return new UsuarioRemoto(id, null, "TURISTA", "ACTIVO", null);
    }
    if (!"ACTIVO".equals(turista.estado())) {
      throw new AccesoDenegadoException("Tu cuenta no está activa");
    }
    return turista;
  }

  private static void exigirEdad(UsuarioRemoto turista, int edadMinima, LocalDate fechaServicio) {
    if (turista.fechaNacimiento() == null) {
      throw new ReglaNegocioException("Esta actividad exige " + edadMinima
          + " años o más y tu perfil no tiene fecha de nacimiento");
    }
    if (Period.between(turista.fechaNacimiento(), fechaServicio).getYears() < edadMinima) {
      throw new ReglaNegocioException("Esta actividad es para personas de " + edadMinima + " años o más (RF065)");
    }
  }
}
