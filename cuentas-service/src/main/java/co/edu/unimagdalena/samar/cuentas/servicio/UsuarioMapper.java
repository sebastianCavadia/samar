package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.modelo.PrestadorTuristico;
import co.edu.unimagdalena.samar.cuentas.modelo.Turista;
import co.edu.unimagdalena.samar.cuentas.modelo.Usuario;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.PrestadorPendienteResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;

import java.time.LocalDate;

/** Entidad → DTO. Vive en servicio para que las entidades nunca suban a presentación. */
final class UsuarioMapper {

  private UsuarioMapper() {
  }

  static UsuarioResponse aResponse(Usuario u) {
    Boolean validado = (u instanceof PrestadorTuristico p) ? p.isValidado() : null;
    LocalDate fechaNacimiento = (u instanceof Turista t) ? t.getFechaNacimiento() : null;
    return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().name(),
        u.getEstado().name(), u.getIdioma(), u.getTelefono(), u.getUltimoAcceso(), validado, fechaNacimiento);
  }

  static PrestadorPendienteResponse aPendiente(PrestadorTuristico p) {
    return new PrestadorPendienteResponse(p.getId(), p.getNombre(), p.getCorreo(),
        p.getTipoPrestador().name(), p.getNit(), p.getRnt(), p.getDireccion(), p.getCreadoEn());
  }
}
