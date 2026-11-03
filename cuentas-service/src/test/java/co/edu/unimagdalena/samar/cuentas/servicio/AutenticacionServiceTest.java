package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.config.CuentasProperties;
import co.edu.unimagdalena.samar.cuentas.modelo.EstadoCuenta;
import co.edu.unimagdalena.samar.cuentas.modelo.Turista;
import co.edu.unimagdalena.samar.cuentas.persistencia.UsuarioRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.LoginRequest;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.SesionResponse;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.CredencialesInvalidasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutenticacionServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-01T12:00:00Z");
  private static final CuentasProperties PROPS = new CuentasProperties(
      "secreto-de-prueba-con-al-menos-32-bytes!!", 60, 5, 5, "admin@samar.co", "x");

  private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
  private UsuarioRepository usuarios;
  private Turista turista;

  @BeforeEach
  void setUp() {
    usuarios = mock(UsuarioRepository.class);
    turista = new Turista();
    turista.setNombre("Laura");
    turista.setCorreo("laura@demo.co");
    turista.setPasswordHash(encoder.encode("ClaveSegura1"));
    when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(turista));
  }

  private AutenticacionService servicioEn(Instant instante) {
    Clock clock = Clock.fixed(instante, ZoneOffset.UTC);
    return new AutenticacionService(usuarios, encoder, new TokenService(PROPS, clock), PROPS, clock);
  }

  @Test
  void loginCorrectoEmiteTokenYRegistraUltimoAcceso() {
    SesionResponse sesion = servicioEn(AHORA).iniciarSesion(new LoginRequest("Laura@Demo.co", "ClaveSegura1"));

    assertThat(sesion.token()).isNotBlank();
    assertThat(sesion.usuario().rol()).isEqualTo("TURISTA");
    assertThat(turista.getUltimoAcceso()).isEqualTo(AHORA);
  }

  @Test
  void cincoIntentosFallidosBloqueanCincoMinutos_RF005() {
    AutenticacionService servicio = servicioEn(AHORA);
    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest("laura@demo.co", "mala")))
          .isInstanceOf(CredencialesInvalidasException.class);
    }

    // Aun con la contraseña correcta, sigue bloqueada dentro de la ventana.
    assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest("laura@demo.co", "ClaveSegura1")))
        .isInstanceOf(AccesoDenegadoException.class);

    // Pasados los 5 minutos, puede entrar.
    SesionResponse sesion = servicioEn(AHORA.plus(Duration.ofMinutes(6)))
        .iniciarSesion(new LoginRequest("laura@demo.co", "ClaveSegura1"));
    assertThat(sesion.token()).isNotBlank();
  }

  @Test
  void cuentaInactivaNoPuedeIniciarSesion_RF006() {
    turista.setEstado(EstadoCuenta.INACTIVO);

    assertThatThrownBy(() -> servicioEn(AHORA).iniciarSesion(new LoginRequest("laura@demo.co", "ClaveSegura1")))
        .isInstanceOf(AccesoDenegadoException.class)
        .hasMessageContaining("administración");
  }
}
