package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.modelo.EstadoCuenta;
import co.edu.unimagdalena.samar.cuentas.persistencia.FotoPerfilRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.PrestadorRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.UsuarioRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Perfil propio y consultas internas que hacen otros microservicios. */
@Service
public class UsuarioService {

  private final UsuarioRepository usuarios;
  private final PrestadorRepository prestadores;
  private final FotoPerfilRepository fotos;

  public UsuarioService(UsuarioRepository usuarios, PrestadorRepository prestadores, FotoPerfilRepository fotos) {
    this.usuarios = usuarios;
    this.prestadores = prestadores;
    this.fotos = fotos;
  }

  /** RF008 (consulta): perfil del usuario autenticado. */
  @Transactional(readOnly = true)
  public UsuarioResponse perfil(Identidad actor) {
    return usuarios.findById(actor.usuarioId())
        .map(u -> UsuarioMapper.aResponse(u, fotos.existsById(u.getId())))
        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
  }

  /** Para oferta y reservas: datos de un usuario (validación del prestador, fecha de nacimiento...). */
  @Transactional(readOnly = true)
  public UsuarioResponse consultarPorId(Long id) {
    return usuarios.findById(id)
        .map(UsuarioMapper::aResponse)
        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
  }

  /**
   * Para oferta-service (ADR-13): prestadores cuyas ofertas deben ocultarse porque su cuenta
   * fue desactivada o bloqueada. Oferta lo consulta periódicamente.
   */
  @Transactional(readOnly = true)
  public List<Long> prestadoresSuspendidos() {
    return prestadores.idsNoActivos(EstadoCuenta.ACTIVO);
  }
}
