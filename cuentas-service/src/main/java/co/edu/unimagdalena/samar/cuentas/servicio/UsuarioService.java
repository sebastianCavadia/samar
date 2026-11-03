package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.integracion.CorreoClient;
import co.edu.unimagdalena.samar.cuentas.modelo.PrestadorTuristico;
import co.edu.unimagdalena.samar.cuentas.persistencia.PrestadorRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.UsuarioRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.PrestadorPendienteResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.RecursoNoEncontradoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class UsuarioService {

  private final UsuarioRepository usuarios;
  private final PrestadorRepository prestadores;
  private final CorreoClient correo;
  private final Clock clock;

  public UsuarioService(UsuarioRepository usuarios, PrestadorRepository prestadores,
      CorreoClient correo, Clock clock) {
    this.usuarios = usuarios;
    this.prestadores = prestadores;
    this.correo = correo;
    this.clock = clock;
  }

  /** RF008 (consulta): perfil del usuario autenticado. */
  @Transactional(readOnly = true)
  public UsuarioResponse perfil(Identidad actor) {
    return usuarios.findById(actor.usuarioId())
        .map(UsuarioMapper::aResponse)
        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
  }

  /** RF095: prestadores que esperan validación de documentación. */
  @Transactional(readOnly = true)
  public List<PrestadorPendienteResponse> prestadoresPendientes(Identidad actor) {
    exigirAdministrador(actor);
    return prestadores.findByValidadoFalseOrderByCreadoEnAsc().stream()
        .map(UsuarioMapper::aPendiente)
        .toList();
  }

  /** RF095: el administrador valida al prestador; desde ahí sus ofertas pueden ser públicas (RF085). */
  @Transactional
  public UsuarioResponse validarPrestador(Identidad actor, Long prestadorId) {
    exigirAdministrador(actor);
    PrestadorTuristico p = prestadores.findById(prestadorId)
        .orElseThrow(() -> new RecursoNoEncontradoException("Prestador no encontrado"));
    if (p.isValidado()) {
      throw new ReglaNegocioException("El prestador ya estaba validado");
    }
    p.validar(clock.instant());
    correo.enviar(p.getCorreo(), "Cuenta validada",
        "Tu cuenta de prestador fue validada. Ya puedes publicar tus servicios.");
    return UsuarioMapper.aResponse(p);
  }

  /** Consulta interna para otros microservicios (p. ej. oferta verifica que el prestador esté validado). */
  @Transactional(readOnly = true)
  public UsuarioResponse consultarPorId(Long id) {
    return usuarios.findById(id)
        .map(UsuarioMapper::aResponse)
        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
  }

  private static void exigirAdministrador(Identidad actor) {
    if (!actor.esAdministrador()) {
      throw new AccesoDenegadoException("Solo un administrador puede realizar esta acción");
    }
  }
}
