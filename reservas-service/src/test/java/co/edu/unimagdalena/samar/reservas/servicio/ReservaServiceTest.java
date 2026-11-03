package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.config.ReservasProperties;
import co.edu.unimagdalena.samar.reservas.integracion.CuentasClient;
import co.edu.unimagdalena.samar.reservas.integracion.CuentasClient.UsuarioRemoto;
import co.edu.unimagdalena.samar.reservas.integracion.IntegracionException;
import co.edu.unimagdalena.samar.reservas.integracion.Notificador;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient;
import co.edu.unimagdalena.samar.reservas.integracion.OfertaClient.ServicioRemoto;
import co.edu.unimagdalena.samar.reservas.modelo.*;
import co.edu.unimagdalena.samar.reservas.persistencia.DisponibilidadRepository;
import co.edu.unimagdalena.samar.reservas.persistencia.PagoRepository;
import co.edu.unimagdalena.samar.reservas.persistencia.ReservaRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
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

class ReservaServiceTest {

  private static final Identidad TURISTA = new Identidad(2L, "TURISTA");
  private static final Identidad PRESTADOR = new Identidad(3L, "PRESTADOR");
  private static final ReservasProperties PROPS = new ReservasProperties(15, 120, "America/Bogota");
  private static final Clock RELOJ = Clock.system(ZoneId.of("America/Bogota"));

  private ReservaRepository reservas;
  private DisponibilidadRepository disponibilidades;
  private OfertaClient oferta;
  private CuentasClient cuentas;
  private ReservaService servicio;
  private Disponibilidad disp;

  @BeforeEach
  void setUp() {
    reservas = mock(ReservaRepository.class);
    disponibilidades = mock(DisponibilidadRepository.class);
    oferta = mock(OfertaClient.class);
    cuentas = mock(CuentasClient.class);
    servicio = crearServicio(RELOJ);

    disp = new Disponibilidad(11L, LocalDate.now(RELOJ).plusDays(2), null, 10);
    ReflectionTestUtils.setField(disp, "id", 5L);
    when(disponibilidades.findById(5L)).thenReturn(Optional.of(disp));
    when(disponibilidades.getReferenceById(5L)).thenReturn(disp);
    when(disponibilidades.ocuparCupos(eq(5L), anyInt())).thenReturn(1);
    when(reservas.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    when(cuentas.buscarUsuario(2L)).thenReturn(Optional.of(
        new UsuarioRemoto(2L, "Laura", "TURISTA", "ACTIVO", LocalDate.of(1998, 5, 20))));
  }

  private ReservaService crearServicio(Clock reloj) {
    var tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
    return new ReservaService(reservas, disponibilidades, mock(PagoRepository.class), oferta, cuentas,
        mock(Notificador.class), new Cierres(disponibilidades), tx, reloj, PROPS);
  }

  private void ofertaDevuelve(String tipo, String modalidad, String precio, Integer capacidad, Integer edadMinima) {
    when(oferta.buscarServicio(11L)).thenReturn(Optional.of(new ServicioRemoto(11L, 3L, tipo, "Servicio demo",
        modalidad, new BigDecimal(precio), "COP", true, capacidad, edadMinima)));
  }

  private Respuestas.Reserva reservar(int personas) {
    return servicio.crear(TURISTA, new Solicitudes.CrearReserva(5L, personas));
  }

  @Test
  void hospedajeOcupaUnaHabitacionYFijaElPrecio_RF047() {
    ofertaDevuelve("HOSPEDAJE", "RESERVA", "350000", 2, null);

    Respuestas.Reserva r = reservar(2);

    verify(disponibilidades).ocuparCupos(5L, 1);
    assertThat(r.estado()).isEqualTo("PENDIENTE");
    assertThat(r.cantidad()).isEqualTo(1);
    assertThat(r.personas()).isEqualTo(2);
    assertThat(r.total()).isEqualByComparingTo("350000");
    assertThat(r.nombreTurista()).isEqualTo("Laura");
    assertThat(r.expiraEn()).as("tiene plazo para pagar").isNotNull();
  }

  @Test
  void actividadOcupaUnCupoPorPersona() {
    ofertaDevuelve("ACTIVIDAD", "RESERVA", "100000", null, null);

    Respuestas.Reserva r = reservar(3);

    verify(disponibilidades).ocuparCupos(5L, 3);
    assertThat(r.total()).isEqualByComparingTo("300000");
  }

  @Test
  void sinCuposNoSeCreaLaReserva() {
    ofertaDevuelve("ACTIVIDAD", "RESERVA", "100000", null, null);
    when(disponibilidades.ocuparCupos(5L, 4)).thenReturn(0);

    assertThatThrownBy(() -> reservar(4)).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cupos");
    verify(reservas, never()).saveAndFlush(any());
  }

  @Test
  void noSeReservaUnaFechaPasada_RF066() {
    ReflectionTestUtils.setField(disp, "fecha", LocalDate.now(RELOJ).minusDays(1));

    assertThatThrownBy(() -> reservar(1)).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("RF066");
    verifyNoInteractions(oferta);
  }

  @Test
  void hospedajeNoAdmiteMasPersonasQueSuCapacidad_RF064() {
    ofertaDevuelve("HOSPEDAJE", "RESERVA", "350000", 2, null);

    assertThatThrownBy(() -> reservar(3)).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("RF064");
  }

  @Test
  void actividadConEdadMinimaRechazaAMenores_RF065() {
    ofertaDevuelve("ACTIVIDAD", "RESERVA", "100000", null, 18);
    when(cuentas.buscarUsuario(2L)).thenReturn(Optional.of(
        new UsuarioRemoto(2L, "Sofía", "TURISTA", "ACTIVO", LocalDate.now(RELOJ).minusYears(15))));

    assertThatThrownBy(() -> reservar(1)).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("RF065");
  }

  @Test
  void servicioSinReservaNoSeReserva() {
    ofertaDevuelve("GASTRONOMICO", "WALK_IN", "65000", null, null);

    assertThatThrownBy(() -> reservar(2)).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("orden de llegada");
  }

  @Test
  void unPrestadorNoPuedeReservar() {
    assertThatThrownBy(() -> servicio.crear(PRESTADOR, new Solicitudes.CrearReserva(5L, 1)))
        .isInstanceOf(AccesoDenegadoException.class);
  }

  @Test
  void siCuentasCaeSeSigueReservandoCuandoNoHayEdadMinima() {
    ofertaDevuelve("ACTIVIDAD", "RESERVA", "100000", null, null);
    when(cuentas.buscarUsuario(2L)).thenThrow(new IntegracionException("caído", null));

    assertThat(reservar(1).estado()).isEqualTo("PENDIENTE");
  }

  @Test
  void siCuentasCaeYHayEdadMinimaNoSeArriesga() {
    ofertaDevuelve("ACTIVIDAD", "RESERVA", "100000", null, 12);
    when(cuentas.buscarUsuario(2L)).thenThrow(new IntegracionException("caído", null));

    assertThatThrownBy(() -> reservar(1)).isInstanceOf(ServicioNoDisponibleException.class);
    verify(disponibilidades, never()).ocuparCupos(anyLong(), anyInt());
  }

  // ---------- Pagos ----------

  private Reserva reservaPendiente() {
    Reserva r = new Reserva(2L, "Laura", 2, "COP", 2L);
    r.agregarDetalle(new DetalleReserva(disp, 11L, 3L, "Servicio demo", "ACTIVIDAD", 2, new BigDecimal("100000")));
    when(reservas.buscarParaModificar(1L)).thenReturn(Optional.of(r));
    return r;
  }

  @Test
  void pagoSimuladoSeApruebaAlInstante_RF043() {
    Reserva r = reservaPendiente();

    Respuestas.Reserva resp = servicio.pagar(TURISTA, 1L, new Solicitudes.Pagar("PASARELA", null));

    assertThat(resp.pago().estado()).isEqualTo("APROBADO");
    assertThat(resp.pago().referencia()).startsWith("SIM-");
    assertThat(resp.pago().monto()).isEqualByComparingTo("200000");
    assertThat(resp.expiraEn()).as("ya no corre el plazo").isNull();
    assertThat(r.tienePagoAprobado()).isTrue();
  }

  @Test
  void noSeAceptaUnSegundoPagoMientrasHayUnoVigente() {
    reservaPendiente();
    servicio.pagar(TURISTA, 1L, new Solicitudes.Pagar("COMPROBANTE", "TRANSF-1"));

    assertThatThrownBy(() -> servicio.pagar(TURISTA, 1L, new Solicitudes.Pagar("PASARELA", null)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("comprobante");
  }

  @Test
  void fueraDelPlazoNoSePaga_RF056() {
    reservaPendiente();
    ReservaService enElFuturo = crearServicio(Clock.offset(RELOJ, Duration.ofMinutes(20)));

    assertThatThrownBy(() -> enElFuturo.pagar(TURISTA, 1L, new Solicitudes.Pagar("PASARELA", null)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("plazo");
  }

  @Test
  void otroTuristaNoPuedePagarLaReserva() {
    reservaPendiente();

    assertThatThrownBy(() -> servicio.pagar(new Identidad(99L, "TURISTA"), 1L, new Solicitudes.Pagar("PASARELA", null)))
        .isInstanceOf(AccesoDenegadoException.class);
  }

  // ---------- Cancelar ----------

  @Test
  void cancelarUnaReservaPagadaLiberaCuposYReembolsa_RF040_RF045() {
    Reserva r = reservaPendiente();
    servicio.pagar(TURISTA, 1L, new Solicitudes.Pagar("PASARELA", null));

    Respuestas.Reserva resp = servicio.cancelar(TURISTA, 1L, "Cambio de planes");

    assertThat(resp.estado()).isEqualTo("CANCELADA");
    assertThat(resp.motivo()).isEqualTo("Cambio de planes");
    assertThat(r.getPagos().get(0).getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
    verify(disponibilidades).liberarCupos(5L, 2);
  }
}
