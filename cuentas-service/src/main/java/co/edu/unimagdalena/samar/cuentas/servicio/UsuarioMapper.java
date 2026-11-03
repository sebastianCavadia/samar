package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.modelo.PrestadorTuristico;
import co.edu.unimagdalena.samar.cuentas.modelo.Usuario;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.PrestadorPendienteResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;

/** Entidad → DTO. Vive en servicio para que las entidades nunca suban a presentación. */
final class UsuarioMapper {

  private UsuarioMapper() {
  }

  static UsuarioResponse aResponse(Usuario u) {
    Boolean validado = (u instanceof PrestadorTuristico p) ? p.isValidado() : null;
    return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().name(),
        u.getEstado().name(), u.getIdioma(), u.getTelefono(), u.getUltimoAcceso(), validado);
  }

  static PrestadorPendienteResponse aPendiente(PrestadorTuristico p) {
    return new PrestadorPendienteResponse(p.getId(), p.getNombre(), p.getCorreo(),
        p.getTipoPrestador().name(), p.getNit(), p.getRnt(), p.getDireccion(), p.getCreadoEn());
  }
}
