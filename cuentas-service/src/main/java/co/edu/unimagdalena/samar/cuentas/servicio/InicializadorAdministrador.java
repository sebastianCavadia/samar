package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.config.CuentasProperties;
import co.edu.unimagdalena.samar.cuentas.modelo.Administrador;
import co.edu.unimagdalena.samar.cuentas.modelo.Rol;
import co.edu.unimagdalena.samar.cuentas.persistencia.AdministradorRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Crea el primer administrador si no existe ninguno (los demás los crea un admin). */
@Component
class InicializadorAdministrador implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(InicializadorAdministrador.class);

  private final UsuarioRepository usuarios;
  private final AdministradorRepository administradores;
  private final PasswordEncoder passwordEncoder;
  private final CuentasProperties props;

  InicializadorAdministrador(UsuarioRepository usuarios, AdministradorRepository administradores,
      PasswordEncoder passwordEncoder, CuentasProperties props) {
    this.usuarios = usuarios;
    this.administradores = administradores;
    this.passwordEncoder = passwordEncoder;
    this.props = props;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (usuarios.existsByRol(Rol.ADMINISTRADOR)) {
      return;
    }
    Administrador a = new Administrador();
    a.setNombre("Administrador SAMAR");
    a.setCorreo(RegistroService.normalizarCorreo(props.adminCorreo()));
    a.setPasswordHash(passwordEncoder.encode(props.adminPassword()));
    a.setAreaResponsable("General");
    administradores.save(a);
    log.info("Administrador inicial creado: {}", a.getCorreo());
  }
}
