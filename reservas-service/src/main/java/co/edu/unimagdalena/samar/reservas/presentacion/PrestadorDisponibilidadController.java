package co.edu.unimagdalena.samar.reservas.presentacion;

import co.edu.unimagdalena.samar.reservas.servicio.DisponibilidadService;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Respuestas;
import co.edu.unimagdalena.samar.reservas.servicio.dto.Solicitudes;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** RF018: el prestador abre cupos por fecha y bloquea fechas. */
@RestController
@RequestMapping("/api/reservas/prestador")
public class PrestadorDisponibilidadController {

  private final DisponibilidadService disponibilidad;

  public PrestadorDisponibilidadController(DisponibilidadService disponibilidad) {
    this.disponibilidad = disponibilidad;
  }

  @GetMapping("/servicios/{idServicio}/disponibilidad")
  public List<Respuestas.DisponibilidadPrestador> consultar(Identidad actor, @PathVariable Long idServicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
    return disponibilidad.consultarPrestador(actor, idServicio, desde, hasta);
  }

  @PostMapping("/servicios/{idServicio}/disponibilidad")
  public Respuestas.ResultadoApertura abrir(Identidad actor, @PathVariable Long idServicio,
      @Valid @RequestBody Solicitudes.AbrirDisponibilidad req) {
    return disponibilidad.abrir(actor, idServicio, req);
  }

  @PatchMapping("/disponibilidad/{id}")
  public Respuestas.DisponibilidadPrestador cambiar(Identidad actor, @PathVariable Long id,
      @Valid @RequestBody Solicitudes.CambiarDisponibilidad req) {
    return disponibilidad.cambiarEstado(actor, id, req.activo());
  }
}
