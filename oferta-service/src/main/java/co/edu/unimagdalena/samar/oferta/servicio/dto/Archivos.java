package co.edu.unimagdalena.samar.oferta.servicio.dto;

/** RF019: imágenes subidas por el prestador. */
public final class Archivos {

  private Archivos() {
  }

  /** La url es la que se pone en la galería del servicio (ServicioRequest.imagenes). */
  public record Subida(Long id, String url, String tipo, int tamano) {
  }

  public record Contenido(byte[] bytes, String tipo) {
  }
}
