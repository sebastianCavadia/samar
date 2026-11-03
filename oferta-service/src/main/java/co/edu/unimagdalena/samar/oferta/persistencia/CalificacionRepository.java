package co.edu.unimagdalena.samar.oferta.persistencia;

import co.edu.unimagdalena.samar.oferta.modelo.Calificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

  Optional<Calificacion> findByIdReserva(Long idReserva);

  Page<Calificacion> findByServicioIdOrderByFechaDescIdDesc(Long idServicio, Pageable pagina);

  /** RF081: solo las reseñas con esa puntuación. */
  Page<Calificacion> findByServicioIdAndPuntuacionOrderByFechaDescIdDesc(Long idServicio, short puntuacion,
      Pageable pagina);

  Page<Calificacion> findByServicioIdPrestadorOrderByFechaDescIdDesc(Long idPrestador, Pageable pagina);

  Page<Calificacion> findByServicioIdPrestadorAndRespuestaPrestadorIsNullOrderByFechaDescIdDesc(Long idPrestador,
      Pageable pagina);

  long countByServicioIdPrestadorAndRespuestaPrestadorIsNull(Long idPrestador);

  /** Cuántas reseñas hay de cada puntuación (las que no tienen ninguna no aparecen). */
  @Query("select c.puntuacion as puntuacion, count(c) as total from Calificacion c "
      + "where c.servicio.id = :servicio group by c.puntuacion")
  List<ConteoPorPuntuacion> distribucion(@Param("servicio") Long idServicio);

  interface ConteoPorPuntuacion {
    short getPuntuacion();
    long getTotal();
  }
}
