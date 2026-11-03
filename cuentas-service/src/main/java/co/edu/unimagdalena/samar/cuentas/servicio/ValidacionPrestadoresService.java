package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.integracion.CorreoClient;
import co.edu.unimagdalena.samar.cuentas.modelo.AccionAdministrativa;
import co.edu.unimagdalena.samar.cuentas.modelo.Administrador;
import co.edu.unimagdalena.samar.cuentas.modelo.EstadoValidacion;
import co.edu.unimagdalena.samar.cuentas.modelo.PrestadorTuristico;
import co.edu.unimagdalena.samar.cuentas.persistencia.PrestadorRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Admin;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.DatosInvalidosException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.RecursoNoEncontradoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** RF095: el administrador revisa NIT y RNT y decide si el prestador puede publicar. */
@Service
public class ValidacionPrestadoresService {

  private final PrestadorRepository prestadores;
  private final BitacoraAdministrativa bitacora;
  private final CorreoClient correo;
  private final Clock clock;

  public ValidacionPrestadoresService(PrestadorRepository prestadores, BitacoraAdministrativa bitacora,
      CorreoClient correo, Clock clock) {
    this.prestadores = prestadores;
    this.bitacora = bitacora;
    this.correo = correo;
    this.clock = clock;
  }

  /** @param validacion PENDIENTE (por defecto), VALIDADO o RECHAZADO */
  @Transactional(readOnly = true)
  public List<Admin.Prestador> listar(Identidad actor, String validacion) {
    bitacora.exigirAdministrador(actor);
    EstadoValidacion estado;
    try {
      estado = validacion == null || validacion.isBlank()
          ? EstadoValidacion.PENDIENTE
          : EstadoValidacion.valueOf(validacion.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new DatosInvalidosException("Estado de validación no válido: " + validacion);
    }
    return prestadores.findByEstadoValidacionOrderByCreadoEnAsc(estado).stream()
        .map(UsuarioMapper::aPrestador)
        .toList();
  }

  /** Desde PENDIENTE, o desde RECHAZADO si el administrador reconsidera. Desde ahí puede publicar (RF085). */
  @Transactional
  public Admin.Prestador validar(Identidad actor, Long prestadorId) {
    Administrador admin = bitacora.exigirAdministrador(actor);
    PrestadorTuristico p = buscar(prestadorId);
    if (p.isValidado()) {
      throw new ReglaNegocioException("El prestador ya estaba validado");
    }
    p.validar(clock.instant());
    bitacora.registrar(admin, p, AccionAdministrativa.VALIDAR_PRESTADOR, "NIT " + p.getNit() + " y RNT " + p.getRnt() + " verificados");
    correo.enviar(p.getCorreo(), "Cuenta validada",
        "Tu cuenta de prestador fue validada. Ya puedes publicar tus servicios en SAMAR.");
    return UsuarioMapper.aPrestador(p);
  }

  @Transactional
  public Admin.Prestador rechazar(Identidad actor, Long prestadorId, String motivo) {
    Administrador admin = bitacora.exigirAdministrador(actor);
    PrestadorTuristico p = buscar(prestadorId);
    if (p.getEstadoValidacion() != EstadoValidacion.PENDIENTE) {
      throw new ReglaNegocioException("Solo se rechazan solicitudes pendientes. Para retirar a un prestador "
          + "validado, desactiva su cuenta");
    }
    p.rechazar(motivo.trim());
    bitacora.registrar(admin, p, AccionAdministrativa.RECHAZAR_PRESTADOR, motivo.trim());
    correo.enviar(p.getCorreo(), "Solicitud de prestador rechazada",
        "Revisamos tu documentación y no pudimos validarla. Motivo: " + motivo.trim());
    return UsuarioMapper.aPrestador(p);
  }

  private PrestadorTuristico buscar(Long id) {
    return prestadores.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Prestador no encontrado"));
  }
}
