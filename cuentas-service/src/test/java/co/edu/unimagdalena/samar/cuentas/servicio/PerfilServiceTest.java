package co.edu.unimagdalena.samar.cuentas.servicio;

import co.edu.unimagdalena.samar.cuentas.modelo.FotoPerfil;
import co.edu.unimagdalena.samar.cuentas.modelo.PrestadorTuristico;
import co.edu.unimagdalena.samar.cuentas.modelo.Turista;
import co.edu.unimagdalena.samar.cuentas.persistencia.FotoPerfilRepository;
import co.edu.unimagdalena.samar.cuentas.persistencia.UsuarioRepository;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Perfil;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.DatosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PerfilServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-01T12:00:00Z");
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};

  private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
  private UsuarioRepository usuarios;
  private FotoPerfilRepository fotos;
  private PerfilService servicio;
  private Turista turista;
  private PrestadorTuristico prestador;

  @BeforeEach
  void setUp() {
    usuarios = mock(UsuarioRepository.class);
    fotos = mock(FotoPerfilRepository.class);
    servicio = new PerfilService(usuarios, fotos, encoder, Clock.fixed(AHORA, ZoneOffset.UTC));

    turista = new Turista();
    turista.setNombre("Laura");
    turista.setPasswordHash(encoder.encode("ClaveSegura1"));
    ReflectionTestUtils.setField(turista, "id", 2L);
    when(usuarios.findById(2L)).thenReturn(Optional.of(turista));

    prestador = new PrestadorTuristico();
    prestador.setNombre("Tours Caribe");
    prestador.setDireccion("Centro");
    ReflectionTestUtils.setField(prestador, "id", 3L);
    when(usuarios.findById(3L)).thenReturn(Optional.of(prestador));
  }

  private static Identidad turistaId() {
    return new Identidad(2L, "TURISTA");
  }

  @Test
  void elTuristaEditaNombreYTelefono_RF008() {
    var r = servicio.editar(turistaId(), new Perfil.Editar("  Laura Gómez ", " 3001234567 ", "ignorada", "ignorada"));

    assertThat(r.nombre()).isEqualTo("Laura Gómez");
    assertThat(r.telefono()).isEqualTo("3001234567");
    assertThat(r.direccion()).isNull();
  }

  @Test
  void elPrestadorEditaSuUbicacionYNoPuedeDejarlaVacia_RF008() {
    var r = servicio.editar(new Identidad(3L, "PRESTADOR"), new Perfil.Editar("Tours Caribe", "", "Calle 10 # 2-30", "Rodadero"));
    assertThat(r.direccion()).isEqualTo("Calle 10 # 2-30");
    assertThat(r.zona()).isEqualTo("Rodadero");
    assertThat(r.telefono()).isNull();

    assertThatThrownBy(() -> servicio.editar(new Identidad(3L, "PRESTADOR"), new Perfil.Editar("Tours", null, "  ", null)))
        .isInstanceOf(DatosInvalidosException.class).hasMessageContaining("dirección");
  }

  @Test
  void cambiaLaContrasenaConLaActualCorrecta_RF009() {
    servicio.cambiarPassword(turistaId(), new Perfil.CambiarPassword("ClaveSegura1", "OtraClave99"));

    assertThat(encoder.matches("OtraClave99", turista.getPasswordHash())).isTrue();
    assertThat(turista.getPasswordHash()).startsWith("$2"); // BCrypt
  }

  @Test
  void noCambiaSiLaActualEsIncorrectaOSiEsLaMisma_RF009() {
    String antes = turista.getPasswordHash();

    assertThatThrownBy(() -> servicio.cambiarPassword(turistaId(), new Perfil.CambiarPassword("Equivocada1", "OtraClave99")))
        .isInstanceOf(DatosInvalidosException.class).hasMessageContaining("actual");
    assertThatThrownBy(() -> servicio.cambiarPassword(turistaId(), new Perfil.CambiarPassword("ClaveSegura1", "ClaveSegura1")))
        .isInstanceOf(DatosInvalidosException.class).hasMessageContaining("distinta");
    assertThat(turista.getPasswordHash()).isEqualTo(antes);
  }

  @Test
  void guardaUnaFotoJpegOPngReconocidaPorSusBytes() {
    servicio.guardarFoto(turistaId(), JPEG);

    var foto = ArgumentCaptor.forClass(FotoPerfil.class);
    verify(fotos).save(foto.capture());
    assertThat(foto.getValue().getTipo()).isEqualTo("image/jpeg");
    assertThat(foto.getValue().getIdUsuario()).isEqualTo(2L);

    var existente = new FotoPerfil(2L, JPEG, "image/jpeg", AHORA);
    when(fotos.findById(2L)).thenReturn(Optional.of(existente));
    servicio.guardarFoto(turistaId(), PNG);
    assertThat(existente.getTipo()).isEqualTo("image/png");
  }

  @Test
  void rechazaArchivosQueNoSonImagenOSonDemasiadoGrandes() {
    byte[] gif = {'G', 'I', 'F', '8', '9', 'a'};
    byte[] grande = Arrays.copyOf(JPEG, Imagenes.MAX_BYTES + 1);

    assertThatThrownBy(() -> servicio.guardarFoto(turistaId(), gif))
        .isInstanceOf(DatosInvalidosException.class).hasMessageContaining("JPEG o PNG");
    assertThatThrownBy(() -> servicio.guardarFoto(turistaId(), grande))
        .isInstanceOf(DatosInvalidosException.class).hasMessageContaining("2 MB");
    assertThatThrownBy(() -> servicio.guardarFoto(turistaId(), new byte[0]))
        .isInstanceOf(DatosInvalidosException.class);
    verify(fotos, never()).save(any());
  }
}
