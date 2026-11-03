package co.edu.unimagdalena.samar.reservas.modelo;

import java.util.Set;

/**
 * RF048: ciclo de vida de la reserva.
 *
 *   PENDIENTE ──► CONFIRMADA ──► COMPLETADA
 *       │              │
 *       ├──► RECHAZADA └──► CANCELADA
 *       └──► CANCELADA
 */
public enum EstadoReserva {
  PENDIENTE,
  CONFIRMADA,
  RECHAZADA,
  CANCELADA,
  COMPLETADA;

  public boolean puedePasarA(EstadoReserva nuevo) {
    return switch (this) {
      case PENDIENTE -> Set.of(CONFIRMADA, RECHAZADA, CANCELADA).contains(nuevo);
      case CONFIRMADA -> Set.of(CANCELADA, COMPLETADA).contains(nuevo);
      case RECHAZADA, CANCELADA, COMPLETADA -> false;
    };
  }

  /** Activa = todavía ocupa cupos. */
  public boolean esActiva() {
    return this == PENDIENTE || this == CONFIRMADA;
  }
}
