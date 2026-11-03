package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.config.CuentasProperties;
import co.edu.unimagdalena.samar.cuentas.integracion.CorreoClient;
import co.edu.unimagdalena.samar.cuentas.modelo.*;
import co.edu.unimagdalena.samar.cuentas.persistencia.PrestadorRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.TuristaRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.UsuarioRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.RegistroPrestadorRequest;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.RegistroTuristaRequest;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.ReglaNegocioException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;

/** RF001-RF003: registro por tipo de cuenta. */
@Service
public class RegistroService {

  private final UsuarioRepository usuarios;
  private final TuristaRepository turistas;
  private final PrestadorRepository prestadores;
  private final PasswordEncoder passwordEncoder;
  private final CorreoClient correo;
  private final CuentasProperties props;

  public RegistroService(UsuarioRepository usuarios, TuristaRepository turistas,
      PrestadorRepository prestadores, PasswordEncoder passwordEncoder, CorreoClient correo,
      CuentasProperties props) {
    this.usuarios = usuarios;
    this.turistas = turistas;
    this.prestadores = prestadores;
    this.passwordEncoder = passwordEncoder;
    this.correo = correo;
    this.props = props;
  }

  @Transactional
  public UsuarioResponse registrarTurista(RegistroTuristaRequest req) {
    String correoNormalizado = normalizarCorreo(req.correo());
    exigirCorreoLibre(correoNormalizado);
    String documento = req.numeroDocumento().trim();
    if (turistas.existsByNumeroDocumento(documento)) {
      throw new ReglaNegocioException("El número de documento ya está registrado");
    }

    Turista t = new Turista();
    t.setNombre(req.nombre().trim());
    t.setCorreo(correoNormalizado);
    t.setPasswordHash(passwordEncoder.encode(req.password()));
    t.setTelefono(req.telefono());
    t.setTipoDocumento(TipoDocumento.valueOf(req.tipoDocumento()));
    t.setNumeroDocumento(documento);
    t.setNacionalidad(req.nacionalidad());
    t.setPaisResidencia(req.paisResidencia());
    t.setFechaNacimiento(req.fechaNacimiento());
    t.setDiscapacidad(req.discapacidad());
    return UsuarioMapper.aResponse(turistas.save(t));
  }

  @Transactional
  public UsuarioResponse registrarPrestador(RegistroPrestadorRequest req) {
    String correoNormalizado = normalizarCorreo(req.correo());
    exigirCorreoLibre(correoNormalizado);
    if (prestadores.existsByNit(req.nit().trim())) {
      throw new ReglaNegocioException("El NIT ya está registrado");
    }
    if (prestadores.existsByRnt(req.rnt().trim())) {
      throw new ReglaNegocioException("El RNT ya está registrado");
    }

    TipoPrestador tipo = TipoPrestador.valueOf(req.tipoPrestador());
    PrestadorTuristico p = new PrestadorTuristico();
    p.setNombre(req.nombre().trim());
    p.setCorreo(correoNormalizado);
    p.setPasswordHash(passwordEncoder.encode(req.password()));
    p.setTelefono(req.telefono());
    p.setTipoPrestador(tipo);
    p.setNit(req.nit().trim());
    p.setRnt(req.rnt().trim());
    p.setDireccion(req.direccion().trim());
    p.setZona(req.zona());
    p.setLatitud(req.latitud());
    p.setLongitud(req.longitud());
    if (tipo == TipoPrestador.AGENCIA_TURISTICA && req.idiomasGuia() != null) {
      p.setIdiomasGuia(new HashSet<>(req.idiomasGuia()));
    }
    p = prestadores.save(p);

    // RF096: avisar al administrador que hay una cuenta pendiente de validación.
    correo.enviar(props.adminCorreo(), "Prestador pendiente de validación",
        "El prestador " + p.getNombre() + " (NIT " + p.getNit() + ") espera validación.");
    return UsuarioMapper.aResponse(p);
  }

  private void exigirCorreoLibre(String correoNormalizado) {
    if (usuarios.existsByCorreoIgnoreCase(correoNormalizado)) {
      throw new ReglaNegocioException("El correo ya está registrado");
    }
  }

  static String normalizarCorreo(String correo) {
    return correo.trim().toLowerCase(Locale.ROOT);
  }
}
