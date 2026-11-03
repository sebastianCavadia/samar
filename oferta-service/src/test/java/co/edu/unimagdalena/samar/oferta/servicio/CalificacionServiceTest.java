package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.modelo.Calificacion;
import co.edu.unimagdalena.samar.oferta.modelo.Hospedaje;
import co.edu.unimagdalena.samar.oferta.modelo.ServicioTuristico;
import co.edu.unimagdalena.samar.oferta.persistencia.CalificacionRepository;
import co.edu.unimagdalena.samar.oferta.persistencia.CalificacionRepository.ConteoPorPuntuacion;
import co.edu.unimagdalena.samar.oferta.persistencia.ServicioTuristicoRepository;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Calificaciones;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.DatosInvalidosException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CalificacionServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-01T12:00:00Z");
  private static final Identidad PRESTADOR = new Identidad(3L, "PRESTADOR");
  private static final Identidad OTRO_PRESTADOR = new Identidad(9L, "PRESTADOR");
  private static final Identidad TURISTA = new Identidad(2L, "TURISTA");

  private CalificacionRepository calificaciones;
  private ServicioTuristicoRepository servicios;
  private CalificacionService servicio;
  private ServicioTuristico hotel;

  @BeforeEach
  void setUp() {
    calificaciones = mock(CalificacionRepository.class);
    servicios = mock(ServicioTuristicoRepository.class);
    servicio = new CalificacionService(calificaciones, servicios, Clock.fixed(AHORA, ZoneOffset.UTC));
    hotel = new Hospedaje();
    hotel.setIdPrestador(3L);
    hotel.setNombre("Hotel Rodadero Mar");
    when(calificaciones.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(servicios.getReferenceById(10L)).thenReturn(hotel);
  }

  private static Calificaciones.NuevaInterna nueva(int puntuacion, String comentario) {
    return new Calificaciones.NuevaInterna(50L, 10L, 2L, "Ana María Turista", puntuacion, comentario);
  }

  private Calificacion guardada(int puntuacion) {
    return new Calificacion(hotel, 2L, "Ana María Turista", 50L, puntuacion, "Muy bueno", AHORA);
  }

  @Test
  void registrarGuardaYRecalculaElPromedio_RF080() {
    when(servicios.bloquearParaCalificar(10L)).thenReturn(Optional.of(10L));
    when(calificaciones.findByIdReserva(50L)).thenReturn(Optional.empty());

    var r = servicio.registrar(nueva(4, "  Excelente guía  "));

    assertThat(r.nueva()).isTrue();
    assertThat(r.puntuacion()).isEqualTo(4);
    verify(calificaciones).save(argThat(c -> "Excelente guía".equals(c.getComentario()) && c.getFecha().equals(AHORA)));
    verify(servicios).recalcularCalificacion(10L);
  }

  /** El bloqueo va primero: así la comprobación de duplicado y el recálculo ven las calificaciones simultáneas. */
  @Test
  void bloqueaElServicioAntesDeComprobarYDeRecalcular() {
    when(servicios.bloquearParaCalificar(10L)).thenReturn(Optional.of(10L));
    when(calificaciones.findByIdReserva(50L)).thenReturn(Optional.empty());

    servicio.registrar(nueva(5, null));

    InOrder orden = inOrder(servicios, calificaciones);
    orden.verify(servicios).bloquearParaCalificar(10L);
    orden.verify(calificaciones).findByIdReserva(50L);
    orden.verify(calificaciones).save(any());
    orden.verify(servicios).recalcularCalificacion(10L);
  }

  @Test
  void comentarioVacioSeGuardaComoNulo() {
    when(servicios.bloquearParaCalificar(10L)).thenReturn(Optional.of(10L));
    when(calificaciones.findByIdReserva(50L)).thenReturn(Optional.empty());

    servicio.registrar(nueva(3, "   "));

    verify(calificaciones).save(argThat(c -> c.getComentario() == null));
  }

  /** RF076 + reintentos de reservas: la misma reserva nunca cuenta dos veces. */
  @Test
  void mismaReservaDevuelveLaCalificacionExistenteSinContarlaOtraVez() {
    when(servicios.bloquearParaCalificar(10L)).thenReturn(Optional.of(10L));
    when(calificaciones.findByIdReserva(50L)).thenReturn(Optional.of(guardada(2)));

    var r = servicio.registrar(nueva(5, "otra"));

    assertThat(r.nueva()).isFalse();
    assertThat(r.puntuacion()).isEqualTo(2);
    verify(calificaciones, never()).save(any());
    verify(servicios, never()).recalcularCalificacion(anyLong());
  }

  @Test
  void servicioInexistente() {
    when(servicios.bloquearParaCalificar(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servicio.registrar(nueva(5, null))).isInstanceOf(RecursoNoEncontradoException.class);
    verify(calificaciones, never()).save(any());
  }

  @Test
  void publicasIncluyeDistribucionDeCincoAUnaConCeros_RF081() {
    when(servicios.findByIdAndActivoTrueAndPrestadorSuspendidoFalse(10L)).thenReturn(Optional.of(hotel));
    when(calificaciones.findByServicioIdOrderByFechaDescIdDesc(eq(10L), any()))
        .thenReturn(new PageImpl<>(List.of(guardada(5)), PageRequest.of(0, 5), 1));
    when(calificaciones.distribucion(10L)).thenReturn(List.of(conteo(5, 3), conteo(2, 1)));

    var r = servicio.publicas(10L, null, 0);

    assertThat(r.distribucion()).extracting(Calificaciones.ConteoEstrellas::estrellas).containsExactly(5, 4, 3, 2, 1);
    assertThat(r.distribucion()).extracting(Calificaciones.ConteoEstrellas::total).containsExactly(3L, 0L, 0L, 1L, 0L);
    assertThat(r.resenas().contenido().get(0).autor()).isEqualTo("Ana M.");
    assertThat(r.resenas().tamano()).isEqualTo(5);
  }

  @Test
  void filtrarPorEstrellasUsaSoloEsaPuntuacion_RF081() {
    when(servicios.findByIdAndActivoTrueAndPrestadorSuspendidoFalse(10L)).thenReturn(Optional.of(hotel));
    when(calificaciones.findByServicioIdAndPuntuacionOrderByFechaDescIdDesc(eq(10L), eq((short) 5), any()))
        .thenReturn(new PageImpl<>(List.of(guardada(5))));

    var r = servicio.publicas(10L, 5, 0);

    assertThat(r.estrellas()).isEqualTo(5);
    verify(calificaciones, never()).findByServicioIdOrderByFechaDescIdDesc(anyLong(), any());
  }

  @Test
  void filtroDeEstrellasFueraDeRango() {
    when(servicios.findByIdAndActivoTrueAndPrestadorSuspendidoFalse(10L)).thenReturn(Optional.of(hotel));

    assertThatThrownBy(() -> servicio.publicas(10L, 6, 0)).isInstanceOf(DatosInvalidosException.class);
    assertThatThrownBy(() -> servicio.publicas(10L, 0, 0)).isInstanceOf(DatosInvalidosException.class);
  }

  @Test
  void lasResenasDeUnServicioNoPublicadoNoSonPublicas() {
    when(servicios.findByIdAndActivoTrueAndPrestadorSuspendidoFalse(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servicio.publicas(10L, null, 0)).isInstanceOf(RecursoNoEncontradoException.class);
  }

  @Test
  void elDuenoRespondeYQuedaLaFecha_RF030() {
    when(calificaciones.findById(7L)).thenReturn(Optional.of(guardada(4)));

    var r = servicio.responder(PRESTADOR, 7L, "  ¡Gracias por visitarnos!  ");

    assertThat(r.respuesta()).isEqualTo("¡Gracias por visitarnos!");
    assertThat(r.fechaRespuesta()).isEqualTo(AHORA);
    assertThat(r.nombreServicio()).isEqualTo("Hotel Rodadero Mar");
  }

  @Test
  void otroPrestadorNoResponde() {
    when(calificaciones.findById(7L)).thenReturn(Optional.of(guardada(4)));

    assertThatThrownBy(() -> servicio.responder(OTRO_PRESTADOR, 7L, "hola")).isInstanceOf(AccesoDenegadoException.class);
  }

  @Test
  void unTuristaNoResponde() {
    assertThatThrownBy(() -> servicio.responder(TURISTA, 7L, "hola")).isInstanceOf(AccesoDenegadoException.class);
    assertThatThrownBy(() -> servicio.recibidas(TURISTA, false, 0)).isInstanceOf(AccesoDenegadoException.class);
  }

  @Test
  void elAutorSeMuestraAbreviado() {
    assertThat(CalificacionMapper.autor("Ana María Turista")).isEqualTo("Ana M.");
    assertThat(CalificacionMapper.autor("carlos ruiz")).isEqualTo("carlos R.");
    assertThat(CalificacionMapper.autor("Madonna")).isEqualTo("Madonna");
    assertThat(CalificacionMapper.autor(null)).isEqualTo("Turista");
  }

  private static ConteoPorPuntuacion conteo(int puntuacion, long total) {
    return new ConteoPorPuntuacion() {
      @Override public short getPuntuacion() { return (short) puntuacion; }
      @Override public long getTotal() { return total; }
    };
  }
}
