package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.config.ReservasProperties;
import co.edu.unimagdalena.samar.reservas.integracion.Notificador;
import co.edu.unimagdalena.samar.reservas.modelo.*;
import co.edu.unimagdalena.samar.reservas.persistencia.DisponibilidadRepository;
import co.edu.unimagdalena.samar.reservas.persistencia.ReservaRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PrestadorYProcesosTest {

  private static final Identidad PRESTADOR = new Identidad(3L, "PRESTADOR");
  private static final ReservasProperties PROPS = new ReservasProperties(15, 120, "America/Bogota");
  private static final Clock RELOJ = Clock.system(ZoneId.of("America/Bogota"));

  private ReservaRepository reservas;
  private DisponibilidadRepository disponibilidades;
  private PrestadorReservaService prestador;
  private Disponibilidad disp;
  private Reserva reserva;

  @BeforeEach
  void setUp() {
    reservas = mock(ReservaRepository.class);
    disponibilidades = mock(DisponibilidadRepository.class);
    prestador = new PrestadorReservaService(reservas, mock(Notificador.class), new Cierres(disponibilidades), PROPS);

    disp = new Disponibilidad(11L, LocalDate.now(RELOJ).plusDays(2), null, 10);
    ReflectionTestUtils.setField(disp, "id", 5L);
    reserva = new Reserva(2L, "Laura", 2, "COP", 2L);
    reserva.agregarDetalle(new DetalleReserva(disp, 11L, 3L, "Caminata", "ACTIVIDAD", 2, new BigDecimal("90000")));
    when(reservas.buscarParaModificar(1L)).thenReturn(Optional.of(reserva));
  }

  @Test
  void noSeConfirmaSinPagoAprobado_RF044() {
    assertThatThrownBy(() -> prestador.confirmar(PRESTADOR, 1L))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("RF044");
  }

  @Test
  void conPagoAprobadoSeConfirma() {
    reserva.registrarPago(new Pago("SIM-1", MedioPago.PASARELA, EstadoPago.APROBADO, reserva.getTotal()));

    assertThat(prestador.confirmar(PRESTADOR, 1L).estado()).isEqualTo("CONFIRMADA");
    assertThat(reserva.getHistorial()).hasSize(2);
  }

  @Test
  void comprobanteAprobadoPermiteConfirmar() {
    reserva.registrarPago(new Pago("TRANSF-9", MedioPago.COMPROBANTE, EstadoPago.PENDIENTE, reserva.getTotal()));
    assertThatThrownBy(() -> prestador.confirmar(PRESTADOR, 1L)).isInstanceOf(ReglaNegocioException.class);

    prestador.validarPago(PRESTADOR, 1L, true);

    assertThat(prestador.confirmar(PRESTADOR, 1L).estado()).isEqualTo("CONFIRMADA");
  }

  @Test
  void rechazarLiberaCuposYReembolsa_RF038() {
    reserva.registrarPago(new Pago("SIM-1", MedioPago.PASARELA, EstadoPago.APROBADO, reserva.getTotal()));

    var resp = prestador.rechazar(PRESTADOR, 1L, null);

    assertThat(resp.estado()).isEqualTo("RECHAZADA");
    assertThat(resp.motivo()).isEqualTo("Rechazada por el prestador");
    assertThat(reserva.getPagos().get(0).getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
    verify(disponibilidades).liberarCupos(5L, 2);
  }

  @Test
  void unPrestadorNoGestionaReservasDeOtro() {
    assertThatThrownBy(() -> prestador.confirmar(new Identidad(77L, "PRESTADOR"), 1L))
        .isInstanceOf(AccesoDenegadoException.class);
  }

  @Test
  void loConfirmadoYaNoSeRechaza() {
    reserva.registrarPago(new Pago("SIM-1", MedioPago.PASARELA, EstadoPago.APROBADO, reserva.getTotal()));
    prestador.confirmar(PRESTADOR, 1L);

    assertThatThrownBy(() -> prestador.rechazar(PRESTADOR, 1L, "x"))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("confirmada");
  }

  // ---------- Procesos automáticos ----------

  private ProcesosAutomaticosService procesosEn(Clock reloj) {
    return new ProcesosAutomaticosService(reservas, new Cierres(disponibilidades), mock(Notificador.class), reloj, PROPS);
  }

  @Test
  void expiraLaPendienteSinPagoCuandoVenceElPlazo_RF056() {
    assertThat(procesosEn(RELOJ).expirar(1L)).as("aún dentro del plazo").isFalse();

    assertThat(procesosEn(Clock.offset(RELOJ, Duration.ofMinutes(16))).expirar(1L)).isTrue();
    assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    verify(disponibilidades).liberarCupos(5L, 2);
  }

  @Test
  void noExpiraSiHayUnComprobanteEnRevision() {
    reserva.registrarPago(new Pago("TRANSF-9", MedioPago.COMPROBANTE, EstadoPago.PENDIENTE, reserva.getTotal()));

    assertThat(procesosEn(Clock.offset(RELOJ, Duration.ofHours(2))).expirar(1L)).isFalse();
    assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
  }

  @Test
  void completaLaConfirmadaCuandoPasaLaFecha_RF049() {
    reserva.registrarPago(new Pago("SIM-1", MedioPago.PASARELA, EstadoPago.APROBADO, reserva.getTotal()));
    reserva.cambiarEstado(EstadoReserva.CONFIRMADA, 3L, "ok");

    assertThat(procesosEn(RELOJ).completar(1L)).as("la fecha aún no pasa").isFalse();
    assertThat(procesosEn(Clock.offset(RELOJ, Duration.ofDays(3))).completar(1L)).isTrue();
    assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);
  }
}
