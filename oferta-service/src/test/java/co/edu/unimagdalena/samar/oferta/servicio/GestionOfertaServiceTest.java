package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.integracion.CuentasClient;
import co.edu.unimagdalena.samar.oferta.integracion.CuentasClient.UsuarioRemoto;
import co.edu.unimagdalena.samar.oferta.integracion.IntegracionException;
import co.edu.unimagdalena.samar.oferta.modelo.Hospedaje;
import co.edu.unimagdalena.samar.oferta.modelo.ServicioTuristico;
import co.edu.unimagdalena.samar.oferta.persistencia.ServicioTuristicoRepository;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioDetalleResponse;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioRequest;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioRequest.HospedajeDatos;
import co.edu.unimagdalena.samar.oferta.servicio.dto.ServicioRequest.ImagenDatos;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GestionOfertaServiceTest {

  private static final Identidad PRESTADOR = new Identidad(3L, "PRESTADOR");
  private static final Identidad OTRO_PRESTADOR = new Identidad(9L, "PRESTADOR");
  private static final Identidad TURISTA = new Identidad(2L, "TURISTA");

  private ServicioTuristicoRepository repo;
  private CuentasClient cuentas;
  private GestionOfertaService servicio;

  @BeforeEach
  void setUp() {
    repo = mock(ServicioTuristicoRepository.class);
    cuentas = mock(CuentasClient.class);
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    servicio = new GestionOfertaService(repo, cuentas,
        Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC));
  }

  private static ServicioRequest hotel(HospedajeDatos datos) {
    return new ServicioRequest("HOSPEDAJE", "Hotel Rodadero Mar", "Frente al mar", "RESERVA",
        new BigDecimal("350000"), null, "Cra 1 #5-20", "Rodadero", 11.2, -74.2, true,
        Set.of("PLAYA", "DESCANSO"),
        List.of(new ImagenDatos("https://img.samar.co/1.jpg", "Fachada del hotel")),
        datos, null, null);
  }

  private void prestadorValidado(boolean validado) {
    when(cuentas.buscarUsuario(3L))
        .thenReturn(Optional.of(new UsuarioRemoto(3L, "Tayrona Tours", "PRESTADOR", validado)));
  }

  @Test
  void prestadorValidadoRegistraHospedajeComoDueno() {
    prestadorValidado(true);

    ServicioDetalleResponse r = servicio.registrar(PRESTADOR, hotel(new HospedajeDatos("Doble", 2, null, null)));

    assertThat(r.idPrestador()).isEqualTo(3L);
    assertThat(r.tipo()).isEqualTo("HOSPEDAJE");
    assertThat(r.hospedaje().capacidadPersonas()).isEqualTo(2);
    assertThat(r.categorias()).containsExactly("DESCANSO", "PLAYA");
    assertThat(r.moneda()).isEqualTo("COP");
    assertThat(r.nuevo()).isTrue();
  }

  @Test
  void prestadorSinValidarNoPublica_RF085() {
    prestadorValidado(false);

    assertThatThrownBy(() -> servicio.registrar(PRESTADOR, hotel(new HospedajeDatos(null, 2, null, null))))
        .isInstanceOf(AccesoDenegadoException.class)
        .hasMessageContaining("validada");
    verify(repo, never()).save(any());
  }

  @Test
  void turistaNoPuedeRegistrarNiSeConsultaCuentas() {
    assertThatThrownBy(() -> servicio.registrar(TURISTA, hotel(new HospedajeDatos(null, 2, null, null))))
        .isInstanceOf(AccesoDenegadoException.class);
    verifyNoInteractions(cuentas);
  }

  @Test
  void hospedajeSinBloqueDeHospedajeEsInvalido_RF023() {
    prestadorValidado(true);

    assertThatThrownBy(() -> servicio.registrar(PRESTADOR, hotel(null)))
        .isInstanceOf(DatosInvalidosException.class)
        .hasMessageContaining("hospedaje");
  }

  @Test
  void siCuentasNoRespondeSeRechazaSinPublicar() {
    when(cuentas.buscarUsuario(3L)).thenThrow(new IntegracionException("caído", null));

    assertThatThrownBy(() -> servicio.registrar(PRESTADOR, hotel(new HospedajeDatos(null, 2, null, null))))
        .isInstanceOf(ServicioNoDisponibleException.class);
    verify(repo, never()).save(any());
  }

  @Test
  void nadieEditaElServicioDeOtroPrestador() {
    when(repo.findById(1L)).thenReturn(Optional.of(hospedajeDe(3L)));

    assertThatThrownBy(() -> servicio.editar(OTRO_PRESTADOR, 1L, hotel(new HospedajeDatos(null, 4, null, null))))
        .isInstanceOf(AccesoDenegadoException.class);
  }

  @Test
  void alEditarNoSePuedeCambiarElTipo() {
    when(repo.findById(1L)).thenReturn(Optional.of(hospedajeDe(3L)));
    ServicioRequest comoActividad = new ServicioRequest("ACTIVIDAD", "x", null, "RESERVA", BigDecimal.ONE,
        null, "dir", "zona", null, null, false, Set.of("AVENTURA"), null, null, null, null);

    assertThatThrownBy(() -> servicio.editar(PRESTADOR, 1L, comoActividad))
        .isInstanceOf(ReglaNegocioException.class);
  }

  @Test
  void desactivarDejaElServicioInactivo_RF032() {
    ServicioTuristico s = hospedajeDe(3L);
    s.setNombre("Hotel");
    s.setPrecioBase(BigDecimal.TEN);
    when(repo.findById(1L)).thenReturn(Optional.of(s));

    assertThat(servicio.cambiarEstado(PRESTADOR, 1L, false).activo()).isFalse();
    assertThat(s.isActivo()).isFalse();
  }

  private static Hospedaje hospedajeDe(Long prestadorId) {
    Hospedaje h = new Hospedaje();
    h.setIdPrestador(prestadorId);
    return h;
  }
}
