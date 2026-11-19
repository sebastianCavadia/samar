package co.edu.unimagdalena.samar.oferta.presentacion;

import co.edu.unimagdalena.samar.oferta.servicio.ImagenService;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Archivos;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/** RF019: subir imágenes (prestador) y verlas (público, como el resto del catálogo). */
@RestController
public class ImagenController {

  private final ImagenService imagenes;

  public ImagenController(ImagenService imagenes) {
    this.imagenes = imagenes;
  }

  /** Devuelve la URL que se agrega a la galería del servicio al registrarlo o editarlo. */
  @PostMapping(path = "/api/oferta/prestador/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Archivos.Subida subir(Identidad actor, @RequestParam("archivo") MultipartFile archivo) throws IOException {
    return imagenes.subir(actor, archivo.getBytes());
  }

  /** Un archivo nunca cambia (una imagen nueva es otro id), así que el navegador puede guardarlo un año. */
  @GetMapping("/api/oferta/imagenes/{id}")
  public ResponseEntity<byte[]> ver(@PathVariable Long id) {
    Archivos.Contenido imagen = imagenes.obtener(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(imagen.tipo()))
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
        .body(imagen.bytes());
  }
}
