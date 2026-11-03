package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.config.ReservasProperties;
import co.edu.unimagdalena.samar.reservas.integracion.IntegracionException;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient.ServicioRemoto;
import co.edu.unimagdalena.samar.reservas.modelo.Disponibilidad;
import co.edu.unimagdalena.samar.reservas.persistencia.DisponibilidadRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Solicitudes;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** RF018, RF020, RF034: cupos por fecha. El prestador los abre; el turista los consulta. */
@Service
public class DisponibilidadService {

  private static final int DIAS_CONSULTA_POR_DEFECTO = 60;

  private final DisponibilidadRepository disponibilidades;
  private final OfertaClient oferta;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final ReservasProperties props;

  public DisponibilidadService(DisponibilidadRepository disponibilidades, OfertaClient oferta,
      TransactionTemplate tx, Clock clock, ReservasProperties props) {
    this.disponibilidades = disponibilidades;
    this.oferta = oferta;
    this.tx = tx;
    this.clock = clock;
    this.props = props;
  }

  /** Pública (visitante anónimo): solo fechas activas que aún no pasan. */
  @Transactional(readOnly = true)
  public List<Respuestas.DisponibilidadPublica> consultarPublica(Long idServicio, LocalDate desde, LocalDate hasta) {
    LocalDate hoy = LocalDate.now(clock);
    LocalDate inicio = desde == null || desde.isBefore(hoy) ? hoy : desde;
    LocalDate fin = hasta == null ? inicio.plusDays(DIAS_CONSULTA_POR_DEFECTO) : hasta;
    exigirRango(inicio, fin);
    return disponibilidades.findByIdServicioAndActivoTrueAndFechaBetweenOrderByFechaAscHoraAsc(idServicio, inicio, fin)
        .stream()
        .filter(d -> !Fechas.yaPaso(d, clock))
        .map(ReservaMapper::aPublica)
        .toList();
  }

  /** Vista del prestador: incluye fechas bloqueadas y los cupos ya reservados. */
  public List<Respuestas.DisponibilidadPrestador> consultarPrestador(Identidad actor, Long idServicio,
      LocalDate desde, LocalDate hasta) {
    exigirDueno(actor, idServicio);
    LocalDate inicio = desde == null ? LocalDate.now(clock) : desde;
    LocalDate fin = hasta == null ? inicio.plusDays(DIAS_CONSULTA_POR_DEFECTO) : hasta;
    exigirRango(inicio, fin);
    return tx.execute(estado -> disponibilidades
        .findByIdServicioAndFechaBetweenOrderByFechaAscHoraAsc(idServicio, inicio, fin)
        .stream().map(ReservaMapper::aPrestador).toList());
  }

  /**
   * RF018: crea las fechas que no existen y actualiza los cupos de las que sí.
   * Nunca deja menos cupos totales que los ya reservados (se omite esa fecha y se informa).
   */
  public Respuestas.ResultadoApertura abrir(Identidad actor, Long idServicio, Solicitudes.AbrirDisponibilidad req) {
    ServicioRemoto servicio = exigirDueno(actor, idServicio); // llamada remota: fuera de la transacción
    if (!servicio.admiteReservas()) {
      throw new ReglaNegocioException("Este servicio es de atención directa (sin reserva): no maneja cupos");
    }
    LocalDate hoy = LocalDate.now(clock);
    if (req.desde().isBefore(hoy)) {
      throw new DatosInvalidosException("No se pueden abrir fechas pasadas (RF066)");
    }
    exigirRango(req.desde(), req.hasta());

    return tx.execute(estado -> {
      int creadas = 0;
      int actualizadas = 0;
      List<String> omitidas = new ArrayList<>();
      for (LocalDate fecha = req.desde(); !fecha.isAfter(req.hasta()); fecha = fecha.plusDays(1)) {
        if (req.diasSemana() != null && !req.diasSemana().isEmpty() && !req.diasSemana().contains(fecha.getDayOfWeek())) {
          continue;
        }
        var existente = disponibilidades.findByIdServicioAndFechaAndHora(idServicio, fecha, req.hora());
        if (existente.isEmpty()) {
          disponibilidades.save(new Disponibilidad(idServicio, fecha, req.hora(), req.cupos()));
          creadas++;
        } else if (req.cupos() < existente.get().getCuposReservados()) {
          omitidas.add(fecha + ": ya tiene " + existente.get().getCuposReservados() + " cupos reservados");
        } else {
          existente.get().setCuposTotales(req.cupos());
          existente.get().setActivo(true);
          actualizadas++;
        }
      }
      return new Respuestas.ResultadoApertura(creadas, actualizadas, omitidas);
    });
  }

  /** Bloquear una fecha (cierre por clima, mantenimiento...). Las reservas ya hechas se conservan. */
  public Respuestas.DisponibilidadPrestador cambiarEstado(Identidad actor, Long idDisponibilidad, boolean activo) {
    Long idServicio = tx.execute(e -> disponibilidades.findById(idDisponibilidad)
        .map(Disponibilidad::getIdServicio)
        .orElseThrow(() -> new RecursoNoEncontradoException("Fecha no encontrada")));
    exigirDueno(actor, idServicio);
    return tx.execute(e -> {
      Disponibilidad d = disponibilidades.findById(idDisponibilidad).orElseThrow();
      d.setActivo(activo);
      return ReservaMapper.aPrestador(d);
    });
  }

  private void exigirRango(LocalDate desde, LocalDate hasta) {
    if (hasta.isBefore(desde)) {
      throw new DatosInvalidosException("La fecha final no puede ser anterior a la inicial");
    }
    if (ChronoUnit.DAYS.between(desde, hasta) > props.maxDiasApertura()) {
      throw new DatosInvalidosException("El rango no puede superar " + props.maxDiasApertura() + " días");
    }
  }

  private ServicioRemoto exigirDueno(Identidad actor, Long idServicio) {
    if (!actor.esPrestador()) {
      throw new AccesoDenegadoException("Solo el prestador gestiona la disponibilidad de sus servicios");
    }
    ServicioRemoto servicio;
    try {
      servicio = oferta.buscarServicio(idServicio)
          .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
    } catch (IntegracionException e) {
      throw new ServicioNoDisponibleException("No pudimos consultar el servicio. Intenta de nuevo en unos minutos");
    }
    if (!servicio.idPrestador().equals(actor.usuarioId())) {
      throw new AccesoDenegadoException("Este servicio pertenece a otro prestador");
    }
    return servicio;
  }
}
