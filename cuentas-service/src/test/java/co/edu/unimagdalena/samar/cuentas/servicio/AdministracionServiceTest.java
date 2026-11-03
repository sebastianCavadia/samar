package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.integracion.CorreoClient;
import co.edu.unimagdalena.samar.cuentas.modelo.*;
import co.edu.unimagdalena.samar.cuentas.persistencia.*;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Admin;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdministracionServiceTest {

  private static final Identidad ADMIN = new Identidad(1L, "ADMINISTRADOR");

  private UsuarioRepository usuarios;
  private PrestadorRepository prestadores;
  private RegistroAdministrativoRepository registros;
  private CorreoClient correo;
  private AdministracionService administracion;
  private ValidacionPrestadoresService validaciones;
  private Administrador admin;
  private Turista turista;
  private PrestadorTuristico prestador;

  @BeforeEach
  void setUp() {
    usuarios = mock(UsuarioRepository.class);
    prestadores = mock(PrestadorRepository.class);
    var administradores = mock(AdministradorRepository.class);
    registros = mock(RegistroAdministrativoRepository.class);
    correo = mock(CorreoClient.class);
    Clock clock = Clock.systemUTC();
    var bitacora = new BitacoraAdministrativa(administradores, registros, clock);
    administracion = new AdministracionService(usuarios, prestadores, administradores, registros, bitacora, correo,
        new BCryptPasswordEncoder(4));
    validaciones = new ValidacionPrestadoresService(prestadores, bitacora, correo, clock);

    admin = new Administrador();
    ReflectionTestUtils.setField(admin, "id", 1L);
    admin.setNombre("Admin");
    admin.setAreaResponsable("Soporte");
    when(administradores.findById(1L)).thenReturn(Optional.of(admin));

    turista = new Turista();
    ReflectionTestUtils.setField(turista, "id", 5L);
    turista.setNombre("Laura");
    turista.setCorreo("laura@demo.co");
    turista.setTipoDocumento(TipoDocumento.CC);
    turista.setNumeroDocumento("123");
    when(usuarios.findById(5L)).thenReturn(Optional.of(turista));

    prestador = new PrestadorTuristico();
    ReflectionTestUtils.setField(prestador, "id", 7L);
    prestador.setCorreo("hostal@demo.co");
    prestador.setNit("900");
    prestador.setRnt("R1");
    prestador.setTipoPrestador(TipoPrestador.HOTEL);
    when(prestadores.findById(7L)).thenReturn(Optional.of(prestador));
  }

  private RegistroAdministrativo ultimoRegistro() {
    var captor = ArgumentCaptor.forClass(RegistroAdministrativo.class);
    verify(registros, atLeastOnce()).save(captor.capture());
    return captor.getValue();
  }

  @Test
  void soloUnAdministradorAdministra() {
    assertThatThrownBy(() -> administracion.buscar(new Identidad(5L, "TURISTA"), null, null, null, 0))
        .isInstanceOf(AccesoDenegadoException.class);
  }

  @Test
  void unAdministradorDesactivadoNoSigueAdministrandoConUnTokenViejo() {
    admin.cambiarEstado(EstadoCuenta.INACTIVO);

    assertThatThrownBy(() -> administracion.resumen(ADMIN)).isInstanceOf(AccesoDenegadoException.class);
  }

  @Test
  void bloquearRegistraBitacoraConAreaYAvisaPorCorreo_RF097_RF099_RF100() {
    var ficha = administracion.cambiarEstado(ADMIN, 5L, new Admin.CambiarEstado("BLOQUEADO", "Reseñas falsas"));

    assertThat(ficha.estado()).isEqualTo("BLOQUEADO");
    RegistroAdministrativo r = ultimoRegistro();
    assertThat(r.getAccion()).isEqualTo(AccionAdministrativa.CAMBIAR_ESTADO);
    assertThat(r.getAreaResponsable()).isEqualTo("Soporte");
    assertThat(r.getDetalle()).contains("ACTIVO → BLOQUEADO").contains("Reseñas falsas");
    verify(correo).enviar(eq("laura@demo.co"), contains("bloqueada"), contains("Reseñas falsas"));
  }

  @Test
  void reactivarLimpiaElBloqueoTemporalPorIntentos() {
    turista.registrarIntentoFallido(1, Instant.now().plus(Duration.ofMinutes(5)));
    turista.cambiarEstado(EstadoCuenta.INACTIVO);

    administracion.cambiarEstado(ADMIN, 5L, new Admin.CambiarEstado("ACTIVO", "Revisado"));

    assertThat(turista.getEstado()).isEqualTo(EstadoCuenta.ACTIVO);
    assertThat(turista.getBloqueadoHasta()).isNull();
  }

  @Test
  void noPuedeCambiarSuPropioEstado() {
    assertThatThrownBy(() -> administracion.cambiarEstado(ADMIN, 1L, new Admin.CambiarEstado("INACTIVO", "x")))
        .isInstanceOf(ReglaNegocioException.class);
  }

  @Test
  void cambiarAlMismoEstadoNoTieneSentido() {
    assertThatThrownBy(() -> administracion.cambiarEstado(ADMIN, 5L, new Admin.CambiarEstado("ACTIVO", "x")))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("ya está activo");
  }

  @Test
  void editarGuardaQueCambio_RF094() {
    administracion.editar(ADMIN, 5L, new Admin.EditarUsuario("Laura Gómez", null));

    assertThat(turista.getNombre()).isEqualTo("Laura Gómez");
    assertThat(ultimoRegistro().getDetalle()).contains("'Laura' → 'Laura Gómez'");
    assertThatThrownBy(() -> administracion.editar(ADMIN, 5L, new Admin.EditarUsuario("Laura Gómez", null)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("No hay cambios");
  }

  @Test
  void noSeCreaUnAdministradorConUnCorreoRepetido() {
    when(usuarios.existsByCorreoIgnoreCase("laura@demo.co")).thenReturn(true);

    assertThatThrownBy(() -> administracion.crearAdministrador(ADMIN,
        new Admin.CrearAdministrador("X", "Laura@Demo.co", "ClaveSegura1", "Soporte")))
        .isInstanceOf(ReglaNegocioException.class);
  }

  // ---------- Validación de prestadores (RF095) ----------

  @Test
  void rechazarGuardaElMotivoYSoloAplicaAPendientes() {
    var p = validaciones.rechazar(ADMIN, 7L, "El RNT no corresponde al NIT");

    assertThat(p.estadoValidacion()).isEqualTo("RECHAZADO");
    assertThat(p.motivoRechazo()).isEqualTo("El RNT no corresponde al NIT");
    verify(correo).enviar(eq("hostal@demo.co"), anyString(), contains("El RNT no corresponde al NIT"));
    assertThatThrownBy(() -> validaciones.rechazar(ADMIN, 7L, "otra vez")).isInstanceOf(ReglaNegocioException.class);
  }

  @Test
  void unRechazadoSePuedeValidarSiElAdministradorReconsidera() {
    validaciones.rechazar(ADMIN, 7L, "Falta documento");

    var p = validaciones.validar(ADMIN, 7L);

    assertThat(p.estadoValidacion()).isEqualTo("VALIDADO");
    assertThat(p.motivoRechazo()).isNull();
    assertThat(ultimoRegistro().getAccion()).isEqualTo(AccionAdministrativa.VALIDAR_PRESTADOR);
    assertThatThrownBy(() -> validaciones.validar(ADMIN, 7L)).isInstanceOf(ReglaNegocioException.class);
  }
}
