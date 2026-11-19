package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.modelo.FotoPerfil;
import co.edu.unimagdalena.samar.cuentas.modelo.PrestadorTuristico;
import co.edu.unimagdalena.samar.cuentas.modelo.Usuario;
import co.edu.unimagdalena.samar.cuentas.persistencia.FotoPerfilRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.UsuarioRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Perfil;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.DatosInvalidosException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.RecursoNoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** RF008 y RF009: el usuario edita su propio perfil, su foto y su contraseña. */
@Service
public class PerfilService {

  private final UsuarioRepository usuarios;
  private final FotoPerfilRepository fotos;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public PerfilService(UsuarioRepository usuarios, FotoPerfilRepository fotos, PasswordEncoder passwordEncoder,
      Clock clock) {
    this.usuarios = usuarios;
    this.fotos = fotos;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  /** RF008: nombre y teléfono para todos; dirección y zona solo para prestadores. El correo no cambia. */
  @Transactional
  public UsuarioResponse editar(Identidad actor, Perfil.Editar req) {
    Usuario u = cargar(actor);
    u.setNombre(req.nombre().trim());
    u.setTelefono(vacioANulo(req.telefono()));
    if (u instanceof PrestadorTuristico p) {
      String direccion = vacioANulo(req.direccion());
      if (direccion == null) {
        throw new DatosInvalidosException("La dirección del establecimiento es obligatoria");
      }
      p.setDireccion(direccion);
      p.setZona(vacioANulo(req.zona()));
    }
    return UsuarioMapper.aResponse(u, fotos.existsById(u.getId()));
  }

  /** RF009: exige la contraseña actual y guarda la nueva con BCrypt. */
  @Transactional
  public void cambiarPassword(Identidad actor, Perfil.CambiarPassword req) {
    Usuario u = cargar(actor);
    if (!passwordEncoder.matches(req.actual(), u.getPasswordHash())) {
      throw new DatosInvalidosException("La contraseña actual no es correcta");
    }
    if (req.actual().equals(req.nueva())) {
      throw new DatosInvalidosException("La nueva contraseña debe ser distinta de la actual");
    }
    u.setPasswordHash(passwordEncoder.encode(req.nueva()));
  }

  /** RF008: JPEG o PNG de hasta 2 MB; reemplaza la anterior si había una. */
  @Transactional
  public void guardarFoto(Identidad actor, byte[] contenido) {
    Usuario u = cargar(actor);
    String tipo = Imagenes.validar(contenido);
    fotos.findById(u.getId()).ifPresentOrElse(
        f -> f.reemplazar(contenido, tipo, clock.instant()),
        () -> fotos.save(new FotoPerfil(u.getId(), contenido, tipo, clock.instant())));
  }

  @Transactional(readOnly = true)
  public Perfil.Foto foto(Identidad actor) {
    return fotos.findById(actor.usuarioId())
        .map(f -> new Perfil.Foto(f.getContenido(), f.getTipo()))
        .orElseThrow(() -> new RecursoNoEncontradoException("No tienes foto de perfil"));
  }

  @Transactional
  public void eliminarFoto(Identidad actor) {
    fotos.deleteById(actor.usuarioId());
  }

  private Usuario cargar(Identidad actor) {
    return usuarios.findById(actor.usuarioId())
        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
  }

  private static String vacioANulo(String texto) {
    return texto == null || texto.isBlank() ? null : texto.trim();
  }
}
