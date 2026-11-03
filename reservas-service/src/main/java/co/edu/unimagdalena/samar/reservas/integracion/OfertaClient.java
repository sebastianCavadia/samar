package co.edu.unimagdalena.samar.reservas.integracion;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Optional;

/** Consulta a oferta-service (/internal/servicios/{id}): precio vigente, dueño y restricciones. */
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
}
