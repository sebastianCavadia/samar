package co.edu.unimagdalena.samar.reservas.modelo;

import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samar.reservas.modelo.EstadoReserva.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoReservaTest {

  @Test
  void transicionesPermitidas_RF048() {
    assertThat(PENDIENTE.puedePasarA(CONFIRMADA)).isTrue();
    assertThat(PENDIENTE.puedePasarA(RECHAZADA)).isTrue();
    assertThat(PENDIENTE.puedePasarA(CANCELADA)).isTrue();
    assertThat(CONFIRMADA.puedePasarA(CANCELADA)).isTrue();
    assertThat(CONFIRMADA.puedePasarA(COMPLETADA)).isTrue();
  }

  @Test
  void transicionesProhibidas() {
    assertThat(PENDIENTE.puedePasarA(COMPLETADA)).as("no se completa sin confirmar").isFalse();
    assertThat(CONFIRMADA.puedePasarA(RECHAZADA)).as("lo confirmado ya no se rechaza, se cancela").isFalse();
    for (EstadoReserva finalizado : new EstadoReserva[] {RECHAZADA, CANCELADA, COMPLETADA}) {
      for (EstadoReserva otro : EstadoReserva.values()) {
        assertThat(finalizado.puedePasarA(otro)).as(finalizado + " es final").isFalse();
      }
    }
  }

  @Test
  void laReservaNoAceptaTransicionesInvalidasYRegistraBitacora_RF057() {
    Reserva r = new Reserva(1L, "Laura", 2, "COP", 1L);
    assertThatThrownBy(() -> r.cambiarEstado(COMPLETADA, null, "x")).isInstanceOf(IllegalStateException.class);

    r.cambiarEstado(CANCELADA, 1L, "Cambio de planes");
    assertThat(r.getHistorial()).extracting(RegistroBitacora::getEstadoNuevo).containsExactly(PENDIENTE, CANCELADA);
    assertThat(r.getHistorial().get(1).getEstadoAnterior()).isEqualTo(PENDIENTE);
  }
}
