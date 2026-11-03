package co.edu.unimagdalena.samar.reservas.presentacion;

import co.edu.unimagdalena.samar.reservas.servicio.ReservaService;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Solicitudes;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestionar reserva (turista). */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

  private final ReservaService reservas;

  public ReservaController(ReservaService reservas) {
    this.reservas = reservas;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Respuestas.Reserva crear(Identidad actor, @Valid @RequestBody Solicitudes.CrearReserva req) {
    return reservas.crear(actor, req);
  }

  @GetMapping("/mis")
  public List<Respuestas.Reserva> mis(Identidad actor) {
    return reservas.misReservas(actor);
  }

  @GetMapping("/{id}")
  public Respuestas.Reserva detalle(Identidad actor, @PathVariable Long id) {
    return reservas.detalle(actor, id);
  }

  @PostMapping("/{id}/pagos")
  public Respuestas.Reserva pagar(Identidad actor, @PathVariable Long id, @Valid @RequestBody Solicitudes.Pagar req) {
    return reservas.pagar(actor, id, req);
  }

  /** Lo usan tanto el turista como el prestador (RF040). */
  @PostMapping("/{id}/cancelar")
  public Respuestas.Reserva cancelar(Identidad actor, @PathVariable Long id, @Valid @RequestBody Solicitudes.Motivo req) {
    return reservas.cancelar(actor, id, req.motivo());
  }
}
