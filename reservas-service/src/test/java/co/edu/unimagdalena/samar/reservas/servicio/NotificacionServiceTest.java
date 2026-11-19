package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.modelo.Notificacion;
import co.edu.unimagdalena.samar.reservas.persistencia.NotificacionRepository;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.excepcion.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificacionServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-01T15:00:00Z");
  private static final Identidad ANA = new Identidad(2L, "TURISTA");

  private NotificacionRepository repo;
  private NotificacionService servicio;

  @BeforeEach
  void setUp() {
    repo = mock(NotificacionRepository.class);
    servicio = new NotificacionService(repo, Clock.fixed(AHORA, ZoneOffset.UTC));
  }

  @Test
  void guardaLaAlertaParaElUsuario_RF037_RF039() {
    servicio.notificar(2L, "Reserva confirmada", "Tu reserva #4 está confirmada");

    var guardada = ArgumentCaptor.forClass(Notificacion.class);
    verify(repo).save(guardada.capture());
    assertThat(guardada.getValue().getIdUsuario()).isEqualTo(2L);
    assertThat(guardada.getValue().isLeida()).isFalse();
    assertThat(guardada.getValue().getCreadoEn()).isEqualTo(AHORA);
  }

  @Test
  void recortaMensajesLargosYOmiteDestinatarioNulo() {
    servicio.notificar(2L, "Reserva cancelada", "x".repeat(800));
    var guardada = ArgumentCaptor.forClass(Notificacion.class);
    verify(repo).save(guardada.capture());
    assertThat(guardada.getValue().getMensaje()).hasSize(500).endsWith("…");

    servicio.notificar(null, "Sin destinatario", "no se guarda");
    verify(repo, times(1)).save(any());
  }

  @Test
  void listaLasRecientesConElNumeroDeNoLeidas() {
    when(repo.findTop20ByIdUsuarioOrderByCreadoEnDescIdDesc(2L))
        .thenReturn(List.of(new Notificacion(2L, "Pago aprobado", "Reserva #4", AHORA)));
    when(repo.countByIdUsuarioAndLeidaFalse(2L)).thenReturn(1L);

    var r = servicio.mias(ANA);

    assertThat(r.noLeidas()).isEqualTo(1);
    assertThat(r.recientes()).extracting("asunto").containsExactly("Pago aprobado");
  }

  @Test
  void soloSeMarcaComoLeidaUnaNotificacionPropia() {
    var propia = new Notificacion(2L, "A", "B", AHORA);
    var ajena = new Notificacion(9L, "A", "B", AHORA);
    when(repo.findById(1L)).thenReturn(Optional.of(propia));
    when(repo.findById(2L)).thenReturn(Optional.of(ajena));

    servicio.marcarLeida(ANA, 1L);
    assertThat(propia.isLeida()).isTrue();

    assertThatThrownBy(() -> servicio.marcarLeida(ANA, 2L)).isInstanceOf(RecursoNoEncontradoException.class);
    assertThat(ajena.isLeida()).isFalse();
  }
}
