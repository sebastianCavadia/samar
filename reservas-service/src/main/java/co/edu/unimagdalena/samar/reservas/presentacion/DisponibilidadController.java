package co.edu.unimagdalena.samar.reservas.presentacion;

import co.edu.unimagdalena.samar.reservas.servicio.DisponibilidadService;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * RF034: consultar cupos antes de reservar. Pública (el gateway no exige token):
 * GET /api/reservas/disponibilidad?servicio=11&desde=2026-11-10&hasta=2026-11-30
 */
@RestController
@RequestMapping("/api/reservas/disponibilidad")
public class DisponibilidadController {

  private final DisponibilidadService disponibilidad;

  public DisponibilidadController(DisponibilidadService disponibilidad) {
    this.disponibilidad = disponibilidad;
  }

  @GetMapping
  public List<Respuestas.DisponibilidadPublica> consultar(@RequestParam("servicio") Long idServicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
    return disponibilidad.consultarPublica(idServicio, desde, hasta);
  }
}
