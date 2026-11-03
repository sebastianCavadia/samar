package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.modelo.Calificacion;
import co.edu.unimagdalena.samar.oferta.modelo.ServicioTuristico;
import co.edu.unimagdalena.samar.oferta.persistencia.CalificacionRepository;
import co.edu.unimagdalena.samar.oferta.persistencia.ServicioTuristicoRepository;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Calificaciones;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.DatosInvalidosException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.RecursoNoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Calificaciones de los servicios. Registrar solo lo pide reservas-service (ADR-11), porque es quien
 * sabe si la reserva está COMPLETADA (RF076). Consultar es público y responder es del prestador.
 */
@Service
public class CalificacionService {

  /** Las reseñas son largas: de 5 en 5. */
  static final int TAMANO_PAGINA = 5;

  private final CalificacionRepository calificaciones;
  private final ServicioTuristicoRepository servicios;
  private final Clock clock;

  public CalificacionService(CalificacionRepository calificaciones, ServicioTuristicoRepository servicios, Clock clock) {
    this.calificaciones = calificaciones;
    this.servicios = servicios;
    this.clock = clock;
  }

  /**
   * RF075, RF076 y RF080. Idempotente por reserva: si reservas reintenta (p. ej. porque se cayó
   * después de que oferta guardó), recibe la calificación que ya existía y no se cuenta dos veces.
   * El servicio queda bloqueado hasta el commit, así dos calificaciones simultáneas no se pisan
   * el promedio y la comprobación de "ya existe" no tiene carrera.
   */
  @Transactional
  public Calificaciones.RegistroInterno registrar(Calificaciones.NuevaInterna req) {
    servicios.bloquearParaCalificar(req.idServicio())
        .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
    var existente = calificaciones.findByIdReserva(req.idReserva());
    if (existente.isPresent()) {
      return CalificacionMapper.aRegistro(existente.get(), false);
    }
    Calificacion nueva = calificaciones.save(new Calificacion(servicios.getReferenceById(req.idServicio()),
        req.idTurista(), textoOpcional(req.nombreTurista()), req.idReserva(), req.puntuacion(),
        textoOpcional(req.comentario()), clock.instant()));
    servicios.recalcularCalificacion(req.idServicio());
    return CalificacionMapper.aRegistro(nueva, true);
  }

  /** RF080 y RF081. Las reseñas de un servicio que no está publicado tampoco existen para el público. */
  @Transactional(readOnly = true)
  public Calificaciones.ResumenPublico publicas(Long idServicio, Integer estrellas, int pagina) {
    ServicioTuristico servicio = servicios.findByIdAndActivoTrueAndPrestadorSuspendidoFalse(idServicio)
        .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
    if (estrellas != null && (estrellas < 1 || estrellas > 5)) {
      throw new DatosInvalidosException("El filtro de estrellas va de 1 a 5");
    }
    var solicitud = PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA);
    Page<Calificacion> resultado = estrellas == null
        ? calificaciones.findByServicioIdOrderByFechaDescIdDesc(idServicio, solicitud)
        : calificaciones.findByServicioIdAndPuntuacionOrderByFechaDescIdDesc(idServicio, estrellas.shortValue(), solicitud);

    Map<Integer, Long> conteo = calificaciones.distribucion(idServicio).stream()
        .collect(Collectors.toMap(c -> (int) c.getPuntuacion(), CalificacionRepository.ConteoPorPuntuacion::getTotal));
    List<Calificaciones.ConteoEstrellas> distribucion = IntStream.iterate(5, e -> e >= 1, e -> e - 1)
        .mapToObj(e -> new Calificaciones.ConteoEstrellas(e, conteo.getOrDefault(e, 0L)))
        .toList();

    return new Calificaciones.ResumenPublico(servicio.getCalificacionPromedio(), servicio.getTotalCalificaciones(),
        distribucion, estrellas, CalificacionMapper.aPagina(resultado, CalificacionMapper::aResena));
  }

  /** Lo que el prestador recibió en todos sus servicios, incluidos los inactivos. */
  @Transactional(readOnly = true)
  public Calificaciones.Bandeja recibidas(Identidad actor, boolean soloSinResponder, int pagina) {
    exigirPrestador(actor);
    var solicitud = PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA);
    Page<Calificacion> resultado = soloSinResponder
        ? calificaciones.findByServicioIdPrestadorAndRespuestaPrestadorIsNullOrderByFechaDescIdDesc(actor.usuarioId(), solicitud)
        : calificaciones.findByServicioIdPrestadorOrderByFechaDescIdDesc(actor.usuarioId(), solicitud);
    return new Calificaciones.Bandeja(
        calificaciones.countByServicioIdPrestadorAndRespuestaPrestadorIsNull(actor.usuarioId()),
        CalificacionMapper.aPagina(resultado, CalificacionMapper::aResenaPrestador));
  }

  /** RF030: solo el dueño del servicio responde. La respuesta es pública y se puede corregir. */
  @Transactional
  public Calificaciones.ResenaPrestador responder(Identidad actor, Long idCalificacion, String respuesta) {
    exigirPrestador(actor);
    Calificacion calificacion = calificaciones.findById(idCalificacion)
        .orElseThrow(() -> new RecursoNoEncontradoException("Calificación no encontrada"));
    if (!calificacion.getServicio().perteneceA(actor.usuarioId())) {
      throw new AccesoDenegadoException("Solo el prestador del servicio puede responder esta reseña");
    }
    calificacion.responder(respuesta.trim(), clock.instant());
    return CalificacionMapper.aResenaPrestador(calificacion);
  }

  private static void exigirPrestador(Identidad actor) {
    if (!actor.esPrestador()) {
      throw new AccesoDenegadoException("Solo los prestadores de servicio responden reseñas");
    }
  }

  private static String textoOpcional(String valor) {
    return (valor == null || valor.isBlank()) ? null : valor.trim();
  }
}
