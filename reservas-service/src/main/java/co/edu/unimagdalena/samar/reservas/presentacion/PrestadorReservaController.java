package co.edu.unimagdalena.samar.reservas.presentacion;

import co.edu.unimagdalena.samar.reservas.servicio.PrestadorReservaService;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Solicitudes;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Reservas recibidas por el prestador: confirmar o rechazar (RF038) y revisar comprobantes (RF044). */
@RestController
@RequestMapping("/api/reservas/prestador/reservas")
public class PrestadorReservaController {

  private final PrestadorReservaService prestador;

  public PrestadorReservaController(PrestadorReservaService prestador) {
    this.prestador = prestador;
  }

  @GetMapping
  public List<Respuestas.Reserva> recibidas(Identidad actor, @RequestParam(required = false) String estado) {
    return prestador.recibidas(actor, estado);
  }

  @PostMapping("/{id}/confirmar")
  public Respuestas.Reserva confirmar(Identidad actor, @PathVariable Long id) {
    return prestador.confirmar(actor, id);
  }

  @PostMapping("/{id}/rechazar")
  public Respuestas.Reserva rechazar(Identidad actor, @PathVariable Long id,
      @Valid @RequestBody(required = false) Solicitudes.Motivo req) {
    return prestador.rechazar(actor, id, req == null ? null : req.motivo());
  }

  @PostMapping("/{id}/pago")
  public Respuestas.Reserva validarPago(Identidad actor, @PathVariable Long id,
      @Valid @RequestBody Solicitudes.ValidarPago req) {
    return prestador.validarPago(actor, id, req.aprobado());
  }
}
