package co.edu.unimagdalena.samar.reservas.servicio;

import co.edu.unimagdalena.samar.reservas.modelo.Disponibilidad;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

/** RF066: una fecha (u hora de hoy) que ya pasó no se puede reservar. */
final class Fechas {

  private Fechas() {
  }

  static boolean yaPaso(Disponibilidad d, Clock clock) {
    LocalDate hoy = LocalDate.now(clock);
    if (d.getFecha().isBefore(hoy)) {
      return true;
    }
    return d.getFecha().equals(hoy) && d.getHora() != null && d.getHora().isBefore(LocalTime.now(clock));
  }
}
