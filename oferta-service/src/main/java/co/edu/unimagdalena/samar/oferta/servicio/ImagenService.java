package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.modelo.ImagenArchivo;
import co.edu.unimagdalena.samar.oferta.persistencia.ImagenArchivoRepository;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Archivos;
import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.RecursoNoEncontradoException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.Set;

/**
 * RF019: el prestador sube imágenes JPEG o PNG para la galería de sus servicios, y al quitarlas
 * de la galería se eliminan. Se sirven públicamente, porque el catálogo es público.
 */
@Service
public class ImagenService {

  /** Evita que una cuenta llene la base: 10 imágenes por servicio da margen para muchos servicios. */
  static final int MAX_IMAGENES_POR_PRESTADOR = 300;

  private final ImagenArchivoRepository archivos;
  private final Clock clock;

  public ImagenService(ImagenArchivoRepository archivos, Clock clock) {
    this.archivos = archivos;
    this.clock = clock;
  }

  @Transactional
  public Archivos.Subida subir(Identidad actor, byte[] contenido) {
    if (!actor.esPrestador()) {
      throw new AccesoDenegadoException("Solo los prestadores suben imágenes de sus servicios");
    }
    String tipo = Imagenes.validar(contenido);
    if (archivos.countByIdPrestador(actor.usuarioId()) >= MAX_IMAGENES_POR_PRESTADOR) {
      throw new ReglaNegocioException("Alcanzaste el máximo de " + MAX_IMAGENES_POR_PRESTADOR + " imágenes");
    }
    ImagenArchivo archivo = archivos.save(new ImagenArchivo(actor.usuarioId(), contenido, tipo, clock.instant()));
    return new Archivos.Subida(archivo.getId(), archivo.url(), tipo, archivo.getTamano());
  }

  @Transactional(readOnly = true)
  public Archivos.Contenido obtener(Long id) {
    return archivos.findById(id)
        .map(a -> new Archivos.Contenido(a.getContenido(), a.getTipo()))
        .orElseThrow(() -> new RecursoNoEncontradoException("Imagen no encontrada"));
  }

  /**
   * RF019 (eliminar): borra los archivos propios que se quitaron de la galería, salvo que otro
   * servicio los siga usando. Las URL externas no se tocan. Se llama dentro de la transacción de editar.
   */
  void eliminarQuitadas(Long prestador, Collection<String> antes, Collection<String> despues) {
    Set<String> siguen = Set.copyOf(despues);
    for (String url : antes) {
      if (siguen.contains(url) || !url.startsWith(ImagenArchivo.PREFIJO_URL)) {
        continue;
      }
      Long id = idDe(url);
      if (id == null) {
        continue;
      }
      archivos.findById(id)
          .filter(a -> a.esDe(prestador) && !archivos.enUso(url))
          .ifPresent(archivos::delete);
    }
  }

  private static Long idDe(String url) {
    try {
      return Long.valueOf(url.substring(ImagenArchivo.PREFIJO_URL.length()));
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
