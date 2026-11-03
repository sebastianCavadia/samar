package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.integracion.CorreoClient;
import co.edu.unimagdalena.samar.cuentas.modelo.*;
import co.edu.unimagdalena.samar.cuentas.persistencia.*;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Admin;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.PaginaResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.DatosInvalidosException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.RecursoNoEncontradoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.ReglaNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

/**
 * Administrar usuarios: RF091 buscar, RF092 ficha, RF093 estado y nuevos administradores,
 * RF094 editar datos, RF097 bitácora, RF100 aviso por correo.
 */
@Service
public class AdministracionService {

  static final int TAMANO_PAGINA = 10;

  private final UsuarioRepository usuarios;
  private final PrestadorRepository prestadores;
  private final AdministradorRepository administradores;
  private final RegistroAdministrativoRepository registros;
  private final BitacoraAdministrativa bitacora;
  private final CorreoClient correo;
  private final PasswordEncoder passwordEncoder;

  public AdministracionService(UsuarioRepository usuarios, PrestadorRepository prestadores,
      AdministradorRepository administradores, RegistroAdministrativoRepository registros,
      BitacoraAdministrativa bitacora, CorreoClient correo, PasswordEncoder passwordEncoder) {
    this.usuarios = usuarios;
    this.prestadores = prestadores;
    this.administradores = administradores;
    this.registros = registros;
    this.bitacora = bitacora;
    this.correo = correo;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public Admin.Resumen resumen(Identidad actor) {
    bitacora.exigirAdministrador(actor);
    return new Admin.Resumen(prestadores.countByEstadoValidacion(EstadoValidacion.PENDIENTE), usuarios.count(),
        usuarios.countByRol(Rol.TURISTA), usuarios.countByRol(Rol.PRESTADOR), usuarios.countByRol(Rol.ADMINISTRADOR),
        usuarios.countByEstado(EstadoCuenta.INACTIVO), usuarios.countByEstado(EstadoCuenta.BLOQUEADO));
  }

  /** RF091: lista paginada de 10 en 10, los más recientes primero. */
  @Transactional(readOnly = true)
  public PaginaResponse<Admin.UsuarioFila> buscar(Identidad actor, String texto, String rol, String estado, int pagina) {
    bitacora.exigirAdministrador(actor);
    Page<Usuario> resultado = usuarios.findAll(
        UsuarioEspecificaciones.con(texto == null || texto.isBlank() ? null : texto.trim(),
            enumOpcional(Rol.class, rol, "rol"), enumOpcional(EstadoCuenta.class, estado, "estado")),
        PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA, Sort.by(Sort.Order.desc("creadoEn"))));
    return new PaginaResponse<>(resultado.getContent().stream().map(UsuarioMapper::aFila).toList(),
        resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
  }

  /** RF092: ficha completa con su historial administrativo (antes de editar o desactivar). */
  @Transactional(readOnly = true)
  public Admin.UsuarioFicha ficha(Identidad actor, Long id) {
    bitacora.exigirAdministrador(actor);
    Usuario u = buscarUsuario(id);
    return UsuarioMapper.aFicha(u, registros.findByUsuarioIdOrderByFechaDesc(id));
  }

  /** RF093 + RF100: activar, desactivar o bloquear, siempre con motivo y aviso al usuario. */
  @Transactional
  public Admin.UsuarioFicha cambiarEstado(Identidad actor, Long id, Admin.CambiarEstado req) {
    Administrador admin = bitacora.exigirAdministrador(actor);
    if (admin.getId().equals(id)) {
      throw new ReglaNegocioException("No puedes cambiar el estado de tu propia cuenta");
    }
    Usuario u = buscarUsuario(id);
    EstadoCuenta nuevo = EstadoCuenta.valueOf(req.estado());
    if (u.getEstado() == nuevo) {
      throw new ReglaNegocioException("La cuenta ya está " + nuevo.name().toLowerCase(Locale.ROOT));
    }
    EstadoCuenta anterior = u.getEstado();
    u.cambiarEstado(nuevo);
    String motivo = req.motivo().trim();
    bitacora.registrar(admin, u, AccionAdministrativa.CAMBIAR_ESTADO, anterior + " → " + nuevo + ": " + motivo);
    correo.enviar(u.getCorreo(), nuevo == EstadoCuenta.ACTIVO ? "Tu cuenta fue reactivada" : "Tu cuenta fue " +
        (nuevo == EstadoCuenta.BLOQUEADO ? "bloqueada" : "desactivada"),
        "Motivo: " + motivo + ". Si tienes dudas, contacta a la administración de SAMAR.");
    return UsuarioMapper.aFicha(u, registros.findByUsuarioIdOrderByFechaDesc(id));
  }

  /** RF094: corregir datos básicos. La bitácora guarda qué cambió. */
  @Transactional
  public Admin.UsuarioFicha editar(Identidad actor, Long id, Admin.EditarUsuario req) {
    Administrador admin = bitacora.exigirAdministrador(actor);
    Usuario u = buscarUsuario(id);
    StringBuilder cambios = new StringBuilder();
    String nombre = req.nombre().trim();
    String telefono = req.telefono() == null || req.telefono().isBlank() ? null : req.telefono().trim();
    if (!nombre.equals(u.getNombre())) {
      cambios.append("nombre: '").append(u.getNombre()).append("' → '").append(nombre).append("'. ");
      u.setNombre(nombre);
    }
    if (!Objects.equals(telefono, u.getTelefono())) {
      cambios.append("teléfono actualizado. ");
      u.setTelefono(telefono);
    }
    if (cambios.isEmpty()) {
      throw new ReglaNegocioException("No hay cambios para guardar");
    }
    bitacora.registrar(admin, u, AccionAdministrativa.EDITAR_DATOS, cambios.toString().trim());
    return UsuarioMapper.aFicha(u, registros.findByUsuarioIdOrderByFechaDesc(id));
  }

  /** RF093 + RF099: los administradores no se registran solos; los crea otro administrador. */
  @Transactional
  public Admin.UsuarioFicha crearAdministrador(Identidad actor, Admin.CrearAdministrador req) {
    Administrador admin = bitacora.exigirAdministrador(actor);
    String correoNormalizado = RegistroService.normalizarCorreo(req.correo());
    if (usuarios.existsByCorreoIgnoreCase(correoNormalizado)) {
      throw new ReglaNegocioException("El correo ya está registrado");
    }
    Administrador nuevo = new Administrador();
    nuevo.setNombre(req.nombre().trim());
    nuevo.setCorreo(correoNormalizado);
    nuevo.setPasswordHash(passwordEncoder.encode(req.password()));
    nuevo.setAreaResponsable(req.areaResponsable().trim());
    nuevo = administradores.save(nuevo);
    bitacora.registrar(admin, nuevo, AccionAdministrativa.CREAR_ADMINISTRADOR, "Área: " + nuevo.getAreaResponsable());
    return UsuarioMapper.aFicha(nuevo, registros.findByUsuarioIdOrderByFechaDesc(nuevo.getId()));
  }

  /** RF097: todas las acciones administrativas, las más recientes primero. */
  @Transactional(readOnly = true)
  public PaginaResponse<Admin.Evento> bitacora(Identidad actor, int pagina) {
    bitacora.exigirAdministrador(actor);
    var resultado = registros.findAllByOrderByFechaDesc(PageRequest.of(Math.max(pagina, 0), 20));
    return new PaginaResponse<>(resultado.getContent().stream().map(UsuarioMapper::aEvento).toList(),
        resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
  }

  private Usuario buscarUsuario(Long id) {
    return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
  }

  private static <E extends Enum<E>> E enumOpcional(Class<E> tipo, String valor, String campo) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new DatosInvalidosException("Valor no válido para " + campo + ": " + valor);
    }
  }
}
