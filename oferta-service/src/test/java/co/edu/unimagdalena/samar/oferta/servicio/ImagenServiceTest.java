package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.modelo.ImagenArchivo;
import co.edu.unimagdalena.samar.oferta.persistencia.ImagenArchivoRepository;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.DatosInvalidosException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImagenServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-01T12:00:00Z");
  private static final Identidad PRESTADOR = new Identidad(3L, "PRESTADOR");
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};

  private ImagenArchivoRepository archivos;
  private ImagenService servicio;

  @BeforeEach
  void setUp() {
    archivos = mock(ImagenArchivoRepository.class);
    when(archivos.save(any())).thenAnswer(inv -> {
      ImagenArchivo a = inv.getArgument(0);
      ReflectionTestUtils.setField(a, "id", 41L);
      return a;
    });
    servicio = new ImagenService(archivos, Clock.fixed(AHORA, ZoneOffset.UTC));
  }

  private static ImagenArchivo archivo(long id, long prestador) {
    var a = new ImagenArchivo(prestador, JPEG, "image/jpeg", AHORA);
    ReflectionTestUtils.setField(a, "id", id);
    return a;
  }

  @Test
  void elPrestadorSubeJpegYPngYRecibeLaUrlParaLaGaleria_RF019() {
    var jpeg = servicio.subir(PRESTADOR, JPEG);
    assertThat(jpeg.url()).isEqualTo("/api/oferta/imagenes/41");
    assertThat(jpeg.tipo()).isEqualTo("image/jpeg");
    assertThat(jpeg.tamano()).isEqualTo(JPEG.length);

    assertThat(servicio.subir(PRESTADOR, PNG).tipo()).isEqualTo("image/png");
  }

  @Test
  void rechazaOtrosFormatosYArchivosDeMasDe2MB_RF019() {
    byte[] pdf = {'%', 'P', 'D', 'F', '-', '1'};
    assertThatThrownBy(() -> servicio.subir(PRESTADOR, pdf))
        .isInstanceOf(DatosInvalidosException.class).hasMessageContaining("JPEG o PNG");
    assertThatThrownBy(() -> servicio.subir(PRESTADOR, Arrays.copyOf(JPEG, Imagenes.MAX_BYTES + 1)))
        .isInstanceOf(DatosInvalidosException.class).hasMessageContaining("2 MB");
    verify(archivos, never()).save(any());
  }

  @Test
  void soloLosPrestadoresSubenYConUnLimitePorCuenta() {
    assertThatThrownBy(() -> servicio.subir(new Identidad(2L, "TURISTA"), JPEG))
        .isInstanceOf(AccesoDenegadoException.class);

    when(archivos.countByIdPrestador(3L)).thenReturn((long) ImagenService.MAX_IMAGENES_POR_PRESTADOR);
    assertThatThrownBy(() -> servicio.subir(PRESTADOR, JPEG)).isInstanceOf(ReglaNegocioException.class);
  }

  @Test
  void alQuitarlaDeLaGaleriaSeEliminaElArchivoPropioQueNadieUsa_RF019() {
    var quitada = archivo(7, 3);
    when(archivos.findById(7L)).thenReturn(Optional.of(quitada));

    servicio.eliminarQuitadas(3L, List.of("/api/oferta/imagenes/7", "/api/oferta/imagenes/8"),
        List.of("/api/oferta/imagenes/8"));

    verify(archivos).delete(quitada);
    verify(archivos, never()).findById(8L); // la que sigue en la galería ni se consulta
  }

  @Test
  void noBorraArchivosAjenosNiEnUsoNiUrlsExternas() {
    var ajeno = archivo(7, 99);
    var compartido = archivo(9, 3);
    when(archivos.findById(7L)).thenReturn(Optional.of(ajeno));
    when(archivos.findById(9L)).thenReturn(Optional.of(compartido));
    when(archivos.enUso("/api/oferta/imagenes/9")).thenReturn(true);

    servicio.eliminarQuitadas(3L,
        List.of("/api/oferta/imagenes/7", "/api/oferta/imagenes/9", "https://images.unsplash.com/foto.jpg",
            "/api/oferta/imagenes/abc"),
        List.of());

    verify(archivos, never()).delete(any());
  }
}
