package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.modelo.*;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Admin;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;

import java.util.List;
import java.util.TreeSet;

/** Entidad → DTO. Vive en servicio para que las entidades nunca suban a presentación. */
final class UsuarioMapper {

  private UsuarioMapper() {
  }

  static UsuarioResponse aResponse(Usuario u) {
    return aResponse(u, null);
  }

  static UsuarioResponse aResponse(Usuario u, Boolean tieneFoto) {
    PrestadorTuristico p = (u instanceof PrestadorTuristico x) ? x : null;
    return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().name(),
        u.getEstado().name(), u.getIdioma(), u.getTelefono(), u.getUltimoAcceso(),
        p == null ? null : p.isValidado(),
        (u instanceof Turista t) ? t.getFechaNacimiento() : null,
        p == null ? null : p.getEstadoValidacion().name(),
        p == null ? null : p.getMotivoRechazo(),
        (u instanceof Administrador a) ? a.getAreaResponsable() : null,
        p == null ? null : p.getDireccion(),
        p == null ? null : p.getZona(),
        tieneFoto);
  }

  static Admin.UsuarioFila aFila(Usuario u) {
    String validacion = (u instanceof PrestadorTuristico p) ? p.getEstadoValidacion().name() : null;
    return new Admin.UsuarioFila(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().name(), u.getEstado().name(),
        u.getUltimoAcceso(), validacion);
  }

  static Admin.Prestador aPrestador(PrestadorTuristico p) {
    return new Admin.Prestador(p.getId(), p.getNombre(), p.getCorreo(), p.getTelefono(), p.getTipoPrestador().name(),
        p.getNit(), p.getRnt(), p.getDireccion(), p.getZona(), new TreeSet<>(p.getIdiomasGuia()), p.getEstado().name(),
        p.getEstadoValidacion().name(), p.getMotivoRechazo(), p.getCreadoEn(), p.getValidadoEn());
  }

  static Admin.Evento aEvento(RegistroAdministrativo r) {
    return new Admin.Evento(r.getId(), r.getFecha(), r.getAdministrador().getId(), r.getAdministrador().getNombre(),
        r.getAreaResponsable(), r.getUsuario().getId(), r.getUsuario().getNombre(), r.getAccion().name(), r.getDetalle());
  }

  static Admin.UsuarioFicha aFicha(Usuario u, List<RegistroAdministrativo> historial) {
    Admin.DatosTurista turista = (u instanceof Turista t)
        ? new Admin.DatosTurista(t.getTipoDocumento().name(), t.getNumeroDocumento(), t.getNacionalidad(),
            t.getPaisResidencia(), t.getFechaNacimiento(), t.isDiscapacidad())
        : null;
    Admin.Prestador prestador = (u instanceof PrestadorTuristico p) ? aPrestador(p) : null;
    String area = (u instanceof Administrador a) ? a.getAreaResponsable() : null;
    return new Admin.UsuarioFicha(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getRol().name(),
        u.getEstado().name(), u.getIdioma(), u.getCreadoEn(), u.getUltimoAcceso(), u.getBloqueadoHasta(),
        turista, prestador, area, historial.stream().map(UsuarioMapper::aEvento).toList());
  }
}
