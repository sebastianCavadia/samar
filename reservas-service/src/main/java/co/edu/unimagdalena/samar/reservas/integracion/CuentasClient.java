package co.edu.unimagdalena.samar.reservas.integracion;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Optional;

/** Consulta a cuentas-service (/internal/usuarios/{id}): nombre y fecha de nacimiento del turista. */
@Component
public class CuentasClient {

  public record UsuarioRemoto(Long id, String nombre, String rol, String estado, LocalDate fechaNacimiento) {
  }

  private final RestClient http;

  public CuentasClient(@Qualifier("cuentasRestClient") RestClient http) {
    this.http = http;
  }

  /**
   * @return vacío si el usuario no existe
   * @throws IntegracionException si cuentas-service está caído, tarda demasiado o falla
   */
  public Optional<UsuarioRemoto> buscarUsuario(Long id) {
    try {
      return http.get()
          .uri("/internal/usuarios/{id}", id)
          .exchange((req, res) -> {
            HttpStatusCode status = res.getStatusCode();
            if (status.value() == 404) {
              return Optional.<UsuarioRemoto>empty();
            }
            if (status.isError()) {
              throw new IntegracionException("cuentas-service respondió " + status, null);
            }
            return Optional.ofNullable(res.bodyTo(UsuarioRemoto.class));
          });
    } catch (RestClientException e) {
      throw new IntegracionException("cuentas-service no disponible", e);
    }
  }
}
