package co.edu.unimagdalena.samar.oferta.persistencia;

import co.edu.unimagdalena.samar.oferta.modelo.ImagenArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImagenArchivoRepository extends JpaRepository<ImagenArchivo, Long> {

  long countByIdPrestador(Long idPrestador);

  /** Si alguna galería sigue usando esa URL (de este u otro servicio), el archivo no se borra. */
  @Query("select count(i) > 0 from ImagenServicio i where i.url = :url")
  boolean enUso(@Param("url") String url);
}
