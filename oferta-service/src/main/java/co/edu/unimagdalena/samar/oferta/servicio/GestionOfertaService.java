package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.integracion.CuentasClient;
import co.edu.unimagdalena.samar.oferta.integracion.IntegracionException;
import co.edu.unimagdalena.samar.oferta.modelo.*;
import co.edu.unimagdalena.samar.oferta.persistencia.ServicioTuristicoRepository;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioDetalleResponse;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioRequest;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioResumenResponse;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** Casos de uso del prestador: RF016, RF017, RF023-RF026, RF032 y RF085. */
@Service
public class GestionOfertaService {

  private final ServicioTuristicoRepository servicios;
  private final CuentasClient cuentas;
  private final ImagenService imagenes;
  private final Clock clock;

  public GestionOfertaService(ServicioTuristicoRepository servicios, CuentasClient cuentas, ImagenService imagenes,
      Clock clock) {
    this.servicios = servicios;
    this.cuentas = cuentas;
    this.imagenes = imagenes;
    this.clock = clock;
  }

  /** RF016. Solo prestadores validados por el administrador pueden publicar (RF085 / ADR-10). */
  @Transactional
  public ServicioDetalleResponse registrar(Identidad actor, ServicioRequest req) {
    exigirPrestador(actor);
    exigirPrestadorValidado(actor.usuarioId());

    ServicioTuristico servicio = switch (Enums.opcional(TipoServicio.class, req.tipo(), "tipo")) {
      case HOSPEDAJE -> new Hospedaje();
      case ACTIVIDAD -> new ActividadTuristica();
      case GASTRONOMICO -> new ServicioGastronomico();
    };
    servicio.setIdPrestador(actor.usuarioId());
    aplicarDatos(servicio, req);
    return ServicioMapper.aDetalle(servicios.save(servicio), clock.instant());
  }

  /** RF017: solo el dueño edita, y el tipo no se puede cambiar. */
  @Transactional
  public ServicioDetalleResponse editar(Identidad actor, Long id, ServicioRequest req) {
    ServicioTuristico servicio = buscarPropio(actor, id);
    if (!servicio.getTipo().name().equals(req.tipo())) {
      throw new ReglaNegocioException("No se puede cambiar el tipo de un servicio ya registrado");
    }
    List<String> antes = urlsDe(servicio);
    aplicarDatos(servicio, req);
    imagenes.eliminarQuitadas(actor.usuarioId(), antes, urlsDe(servicio)); // RF019: quitar de la galería elimina el archivo
    return ServicioMapper.aDetalle(servicio, clock.instant());
  }

  /** RF032: un servicio inactivo deja de aparecer en búsquedas y recomendaciones. */
  @Transactional
  public ServicioResumenResponse cambiarEstado(Identidad actor, Long id, boolean activo) {
    ServicioTuristico servicio = buscarPropio(actor, id);
    servicio.setActivo(activo);
    return ServicioMapper.aResumen(servicio, clock.instant());
  }

  /** Detalle de un servicio propio, aunque esté inactivo (para editarlo o revisarlo). */
  @Transactional(readOnly = true)
  public ServicioDetalleResponse detallePropio(Identidad actor, Long id) {
    return ServicioMapper.aDetalle(buscarPropio(actor, id), clock.instant());
  }

  /** Incluye los inactivos: el prestador debe poder verlos para reactivarlos. */
  @Transactional(readOnly = true)
  public List<ServicioResumenResponse> misServicios(Identidad actor) {
    exigirPrestador(actor);
    var ahora = clock.instant();
    return servicios.findByIdPrestadorOrderByCreadoEnDesc(actor.usuarioId()).stream()
        .map(s -> ServicioMapper.aResumen(s, ahora))
        .toList();
  }

  private ServicioTuristico buscarPropio(Identidad actor, Long id) {
    exigirPrestador(actor);
    ServicioTuristico servicio = servicios.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
    if (!servicio.perteneceA(actor.usuarioId())) {
      throw new AccesoDenegadoException("Este servicio pertenece a otro prestador");
    }
    return servicio;
  }

  private static List<String> urlsDe(ServicioTuristico s) {
    return s.getImagenes().stream().map(ImagenServicio::getUrl).toList();
  }

  private void aplicarDatos(ServicioTuristico s, ServicioRequest req) {
    s.setNombre(req.nombre().trim());
    s.setDescripcion(req.descripcion());
    s.setModalidad(Enums.opcional(Modalidad.class, req.modalidad(), "modalidad"));
    s.setPrecioBase(req.precioBase());
    s.setMoneda(req.moneda() == null ? "COP" : req.moneda());
    s.setDireccion(req.direccion().trim());
    s.setZona(req.zona().trim());
    s.setLatitud(req.latitud());
    s.setLongitud(req.longitud());
    s.setAccesible(req.accesible());
    s.reemplazarCategorias(Enums.conjunto(Categoria.class, req.categorias(), "categoria"));
    s.reemplazarImagenes(req.imagenes() == null ? List.of() : req.imagenes().stream()
        .map(i -> new ImagenServicio(i.url().trim(), i.descripcion().trim()))
        .toList());

    switch (s) {
      case Hospedaje h -> {
        var d = exigirBloque(req.hospedaje(), "hospedaje");
        h.setTipoHabitacion(d.tipoHabitacion());
        h.setCapacidadPersonas(d.capacidadPersonas());
        h.setHoraCheckIn(d.horaCheckIn());
        h.setHoraCheckOut(d.horaCheckOut());
      }
      case ActividadTuristica a -> {
        var d = exigirBloque(req.actividad(), "actividad");
        a.setDuracionHoras(d.duracionHoras());
        a.setHoraInicio(d.horaInicio());
        a.setDificultad(Enums.opcional(Dificultad.class, d.dificultad(), "dificultad"));
        a.setEdadMinima(d.edadMinima());
      }
      case ServicioGastronomico g -> {
        var d = exigirBloque(req.gastronomico(), "gastronomico");
        g.setTipoComida(d.tipoComida());
        g.setAforo(d.aforo());
        g.setHoraApertura(d.horaApertura());
        g.setHoraCierre(d.horaCierre());
        g.reemplazarDietas(Enums.conjunto(TipoDieta.class, d.dietas(), "dieta"));
      }
      default -> throw new IllegalStateException("Tipo de servicio sin manejar: " + s.getClass());
    }
  }

  private static <T> T exigirBloque(T bloque, String nombre) {
    if (bloque == null) {
      throw new DatosInvalidosException("Para este tipo de servicio debes enviar el bloque '" + nombre + "' (RF023)");
    }
    return bloque;
  }

  private static void exigirPrestador(Identidad actor) {
    if (!actor.esPrestador()) {
      throw new AccesoDenegadoException("Solo los prestadores de servicio gestionan ofertas");
    }
  }

  /**
   * Consulta síncrona a cuentas. Si cuentas no responde se rechaza la operación (falla cerrada):
   * es preferible no publicar a publicar la oferta de un prestador sin validar.
   */
  private void exigirPrestadorValidado(Long prestadorId) {
    CuentasClient.UsuarioRemoto usuario;
    try {
      usuario = cuentas.buscarUsuario(prestadorId)
          .orElseThrow(() -> new AccesoDenegadoException("Tu cuenta no existe"));
    } catch (IntegracionException e) {
      throw new ServicioNoDisponibleException(
          "No pudimos verificar tu cuenta en este momento. Intenta de nuevo en unos minutos");
    }
    if (!Boolean.TRUE.equals(usuario.prestadorValidado())) {
      throw new AccesoDenegadoException(
          "Tu cuenta de prestador aún no ha sido validada por un administrador (RF095)");
    }
  }
}
