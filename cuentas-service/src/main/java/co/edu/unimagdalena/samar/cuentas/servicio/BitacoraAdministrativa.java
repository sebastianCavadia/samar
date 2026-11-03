package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.modelo.AccionAdministrativa;
import co.edu.unimagdalena.samar.cuentas.modelo.Administrador;
import co.edu.unimagdalena.samar.cuentas.modelo.EstadoCuenta;
import co.edu.unimagdalena.samar.cuentas.modelo.RegistroAdministrativo;
import co.edu.unimagdalena.samar.cuentas.modelo.Usuario;
import co.edu.unimagdalena.samar.cuentas.persistencia.AdministradorRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.RegistroAdministrativoRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.AccesoDenegadoException;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Permisos de administrador y registro de sus acciones (RF097, RF099).
 * El rol viene del JWT, pero además se comprueba que el administrador exista y esté activo:
 * un token emitido antes de desactivarlo no debe permitirle seguir administrando.
 */
@Component
class BitacoraAdministrativa {

  private final AdministradorRepository administradores;
  private final RegistroAdministrativoRepository registros;
  private final Clock clock;

  BitacoraAdministrativa(AdministradorRepository administradores, RegistroAdministrativoRepository registros, Clock clock) {
    this.administradores = administradores;
    this.registros = registros;
    this.clock = clock;
  }

  Administrador exigirAdministrador(Identidad actor) {
    if (!actor.esAdministrador()) {
      throw new AccesoDenegadoException("Solo un administrador puede realizar esta acción");
    }
    return administradores.findById(actor.usuarioId())
        .filter(a -> a.getEstado() == EstadoCuenta.ACTIVO)
        .orElseThrow(() -> new AccesoDenegadoException("Tu cuenta de administrador no está activa"));
  }

  void registrar(Administrador administrador, Usuario usuario, AccionAdministrativa accion, String detalle) {
    registros.save(new RegistroAdministrativo(administrador, usuario, accion, detalle, clock.instant()));
  }
}
