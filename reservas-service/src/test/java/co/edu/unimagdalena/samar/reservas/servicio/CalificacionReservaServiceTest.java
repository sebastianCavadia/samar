package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.config.ReservasProperties;
import co.edu.unimagdalena.samar.reservas.integracion.IntegracionException;
import co.edu.unimagdalena.samar.reservas.integracion.Notificador;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient.CalificacionRegistrada;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient.NuevaCalificacion;
import co.edu.unimagdalena.samar.reservas.modelo.*;
import co.edu.unimagdalena.samar.reservas.persistencia.ReservaRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Solicitudes;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CalificacionReservaServiceTest {

  private static final Identidad TURISTA = new Identidad(2L, "TURISTA");
  private static final Identidad OTRO_TURISTA = new Identidad(8L, "TURISTA");
  private static final Identidad PRESTADOR = new Identidad(3L, "PRESTADOR");
  private static final Instant AHORA = Instant.parse("2026-11-01T15:00:00Z");

  private ReservaRepository reservas;
  private OfertaClient oferta;
  private Notificador notificador;
  private CalificacionReservaService servicio;
  private Reserva reserva;

  @BeforeEach
  void setUp() {
    reservas = mock(ReservaRepository.class);
    oferta = mock(OfertaClient.class);
    notificador = mock(Notificador.class);
    servicio = new CalificacionReservaService(reservas, oferta, notificador,
        new TransactionTemplate(mock(PlatformTransactionManager.class)), Clock.fixed(AHORA, ZoneOffset.UTC),
        new ReservasProperties(15, 120, "America/Bogota"));

    var disp = new Disponibilidad(11L, LocalDate.of(2026, 10, 30), null, 10);
    reserva = new Reserva(2L, "Laura Gómez", 2, "COP", 2L);
    reserva.agregarDetalle(new DetalleReserva(disp, 11L, 3L, "Caminata", "ACTIVIDAD", 2, new BigDecimal("90000")));
    ReflectionTestUtils.setField(reserva, "id", 40L);
    when(reservas.findById(40L)).thenReturn(Optional.of(reserva));
    when(reservas.buscarParaModificar(40L)).thenReturn(Optional.of(reserva));
  }

  private void completar() {
    reserva.cambiarEstado(EstadoReserva.CONFIRMADA, 3L, "ok");
    reserva.cambiarEstado(EstadoReserva.COMPLETADA, null, "fecha pasada");
  }

  private void ofertaRegistra(int puntuacion, boolean nueva) {
    when(oferta.registrarCalificacion(any()))
        .thenReturn(Optional.of(new CalificacionRegistrada(1L, 40L, puntuacion, nueva)));
  }

  private static Solicitudes.Calificar estrellas(int puntuacion, String comentario) {
    return new Solicitudes.Calificar(puntuacion, comentario);
  }

  @Test
  void reservaCompletadaSeCalificaYQuedaLaCopiaLocal() {
    completar();
    ofertaRegistra(5, true);

    var r = servicio.calificar(TURISTA, 40L, estrellas(5, "  Guía excelente  "));

    assertThat(r.calificacion()).isEqualTo(5);
    assertThat(r.puedeCalificar()).isFalse();
    assertThat(reserva.getCalificadaEn()).isEqualTo(AHORA);
    verify(oferta).registrarCalificacion(new NuevaCalificacion(40L, 11L, 2L, "Laura Gómez", 5, "Guía excelente"));
    verify(notificador).notificar(eq(3L), eq("Nueva calificación"), contains("5 estrellas"));
  }

  @Test
  void sinCompletarNoSeCalificaNiSeLlamaAOferta_RF076() {
    reserva.cambiarEstado(EstadoReserva.CONFIRMADA, 3L, "ok");

    assertThatThrownBy(() -> servicio.calificar(TURISTA, 40L, estrellas(4, null)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("COMPLETADA");
    verifyNoInteractions(oferta);
  }

  @Test
  void unaSolaVezPorReserva_RF076() {
    completar();
    reserva.registrarCalificacion(3, AHORA);

    assertThatThrownBy(() -> servicio.calificar(TURISTA, 40L, estrellas(5, null)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Ya calificaste");
    verifyNoInteractions(oferta);
  }

  @Test
  void soloElTuristaDeLaReserva() {
    completar();

    assertThatThrownBy(() -> servicio.calificar(OTRO_TURISTA, 40L, estrellas(5, null)))
        .isInstanceOf(AccesoDenegadoException.class);
    assertThatThrownBy(() -> servicio.calificar(PRESTADOR, 40L, estrellas(5, null)))
        .isInstanceOf(AccesoDenegadoException.class);
    verifyNoInteractions(oferta);
  }

  @Test
  void ofertaCaidoResponde503SinMarcarLaReserva() {
    completar();
    when(oferta.registrarCalificacion(any())).thenThrow(new IntegracionException("caído", null));

    assertThatThrownBy(() -> servicio.calificar(TURISTA, 40L, estrellas(5, null)))
        .isInstanceOf(ServicioNoDisponibleException.class);
    assertThat(reserva.getCalificacion()).isNull();
    assertThat(reserva.puedeCalificarse()).as("puede reintentar").isTrue();
    verifyNoInteractions(notificador);
  }

  /** Oferta guardó pero reservas no se enteró (timeout o caída): el reintento repara la copia con la puntuación real. */
  @Test
  void siOfertaYaLaTeniaSeReparaLaCopiaConLaPuntuacionOriginalY409() {
    completar();
    ofertaRegistra(2, false);

    assertThatThrownBy(() -> servicio.calificar(TURISTA, 40L, estrellas(5, null)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("ya había quedado registrada con 2 estrellas");
    assertThat(reserva.getCalificacion()).isEqualTo(2);
    verifyNoInteractions(notificador);
  }

  @Test
  void servicioQueYaNoExisteEnOferta() {
    completar();
    when(oferta.registrarCalificacion(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servicio.calificar(TURISTA, 40L, estrellas(5, null)))
        .isInstanceOf(RecursoNoEncontradoException.class);
    assertThat(reserva.getCalificacion()).isNull();
  }

  @Test
  void laEntidadTampocoPermiteCalificarFueraDeRegla() {
    assertThat(reserva.puedeCalificarse()).isFalse();
    assertThatThrownBy(() -> reserva.registrarCalificacion(5, AHORA)).isInstanceOf(IllegalStateException.class);
  }
}
