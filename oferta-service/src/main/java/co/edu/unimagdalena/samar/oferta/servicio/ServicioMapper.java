package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.modelo.*;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioDetalleResponse;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioInternoResponse;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioRequest.ActividadDatos;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioRequest.GastronomicoDatos;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioRequest.HospedajeDatos;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioRequest.ImagenDatos;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioResumenResponse;

import java.time.Instant;
import java.util.List;

/** Entidad → DTO. Vive en servicio para que las entidades nunca suban a presentación. */
final class ServicioMapper {

  private ServicioMapper() {
  }

  static ServicioResumenResponse aResumen(ServicioTuristico s, Instant ahora) {
    ImagenDatos principal = s.getImagenes().isEmpty() ? null : aImagen(s.getImagenes().get(0));
    return new ServicioResumenResponse(s.getId(), s.getTipo().name(), s.getNombre(), s.getZona(),
        s.getPrecioBase(), s.getMoneda(), s.isAccesible(), s.isActivo(), s.getCalificacionPromedio(),
        s.getTotalCalificaciones(), s.esNuevo(ahora), Enums.nombres(s.getCategorias()), principal);
  }

  static ServicioDetalleResponse aDetalle(ServicioTuristico s, Instant ahora) {
    HospedajeDatos hospedaje = null;
    ActividadDatos actividad = null;
    GastronomicoDatos gastronomico = null;
    switch (s) {
      case Hospedaje h -> hospedaje = new HospedajeDatos(h.getTipoHabitacion(),
          h.getCapacidadPersonas(), h.getHoraCheckIn(), h.getHoraCheckOut());
      case ActividadTuristica a -> actividad = new ActividadDatos(a.getDuracionHoras(),
          a.getHoraInicio(), a.getDificultad() == null ? null : a.getDificultad().name(), a.getEdadMinima());
      case ServicioGastronomico g -> gastronomico = new GastronomicoDatos(g.getTipoComida(),
          g.getAforo(), g.getHoraApertura(), g.getHoraCierre(), Enums.nombres(g.getDietas()));
      default -> throw new IllegalStateException("Tipo de servicio sin mapear: " + s.getClass());
    }
    List<ImagenDatos> imagenes = s.getImagenes().stream().map(ServicioMapper::aImagen).toList();
    return new ServicioDetalleResponse(s.getId(), s.getIdPrestador(), s.getTipo().name(),
        s.getNombre(), s.getDescripcion(), s.getModalidad().name(), s.getPrecioBase(), s.getMoneda(),
        s.getDireccion(), s.getZona(), s.getLatitud(), s.getLongitud(), s.isAccesible(), s.isActivo(),
        s.getCalificacionPromedio(), s.getTotalCalificaciones(), s.esNuevo(ahora),
        Enums.nombres(s.getCategorias()), imagenes, hospedaje, actividad, gastronomico, s.getCreadoEn());
  }

  static ServicioInternoResponse aInterno(ServicioTuristico s) {
    Integer capacidad = (s instanceof Hospedaje h) ? h.getCapacidadPersonas() : null;
    Integer edadMinima = (s instanceof ActividadTuristica a) ? a.getEdadMinima() : null;
    return new ServicioInternoResponse(s.getId(), s.getIdPrestador(), s.getTipo().name(),
        s.getNombre(), s.getModalidad().name(), s.getPrecioBase(), s.getMoneda(), s.isActivo(),
        capacidad, edadMinima);
  }

  private static ImagenDatos aImagen(ImagenServicio i) {
    return new ImagenDatos(i.getUrl(), i.getDescripcion());
  }
}
