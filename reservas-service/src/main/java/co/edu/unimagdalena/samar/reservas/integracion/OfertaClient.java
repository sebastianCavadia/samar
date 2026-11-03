package co.edu.unimagdalena.samar.reservas.integracion;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Optional;

/** Llama a oferta-service: precio vigente, dueño y restricciones de un servicio, y registrar calificaciones. */
@Component
public class OfertaClient {

  public record ServicioRemoto(Long id, Long idPrestador, String tipo, String nombre, String modalidad,
      BigDecimal precioBase, String moneda, boolean activo, Integer capacidadPersonas, Integer edadMinima) {

    public boolean esHospedaje() {
      return "HOSPEDAJE".equals(tipo);
    }

    public boolean admiteReservas() {
      return "RESERVA".equals(modalidad);
    }
  }

  public record NuevaCalificacion(Long idReserva, Long idServicio, Long idTurista, String nombreTurista,
      int puntuacion, String comentario) {
  }

  /** @param nueva false si oferta ya tenía una calificación para esa reserva (la devuelve tal cual). */
  public record CalificacionRegistrada(Long id, Long idReserva, int puntuacion, boolean nueva) {
  }

  private final RestClient http;

  public OfertaClient(@Qualifier("ofertaRestClient") RestClient http) {
    this.http = http;
  }

  /**
   * @return vacío si el servicio no existe
   * @throws IntegracionException si oferta-service está caído, tarda demasiado o falla
   */
  public Optional<ServicioRemoto> buscarServicio(Long id) {
    try {
      return http.get()
          .uri("/internal/servicios/{id}", id)
          .exchange((req, res) -> {
            HttpStatusCode status = res.getStatusCode();
            if (status.value() == 404) {
              return Optional.<ServicioRemoto>empty();
            }
            if (status.isError()) {
              throw new IntegracionException("oferta-service respondió " + status, null);
            }
            return Optional.ofNullable(res.bodyTo(ServicioRemoto.class));
          });
    } catch (RestClientException e) {
      throw new IntegracionException("oferta-service no disponible", e);
    }
  }

  /**
   * ADR-11: POST /internal/calificaciones. Oferta es idempotente por reserva, así que reintentar es seguro.
   * @return vacío si el servicio ya no existe en oferta
   * @throws IntegracionException si oferta-service está caído, tarda demasiado o falla
   */
  public Optional<CalificacionRegistrada> registrarCalificacion(NuevaCalificacion calificacion) {
    try {
      return http.post()
          .uri("/internal/calificaciones")
          .contentType(MediaType.APPLICATION_JSON)
          .body(calificacion)
          .exchange((req, res) -> {
            HttpStatusCode status = res.getStatusCode();
            if (status.value() == 404) {
              return Optional.<CalificacionRegistrada>empty();
            }
            if (status.isError()) {
              throw new IntegracionException("oferta-service respondió " + status, null);
            }
            return Optional.ofNullable(res.bodyTo(CalificacionRegistrada.class));
          });
    } catch (RestClientException e) {
      throw new IntegracionException("oferta-service no disponible", e);
    }
  }
}
