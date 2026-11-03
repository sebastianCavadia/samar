package co.edu.unimagdalena.samar.oferta.persistencia;

import co.edu.unimagdalena.samar.oferta.modelo.ServicioTuristico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicioTuristicoRepository
    extends JpaRepository<ServicioTuristico, Long>, JpaSpecificationExecutor<ServicioTuristico> {

  List<ServicioTuristico> findByIdPrestadorOrderByCreadoEnDesc(Long idPrestador);

  /** Detalle público: publicado y con el prestador activo. */
  Optional<ServicioTuristico> findByIdAndActivoTrueAndPrestadorSuspendidoFalse(Long id);

  /** ADR-13: oculta las ofertas de prestadores desactivados. SQL directo: la columna es solo de lectura en la entidad. */
  @Modifying
  @Query(value = "UPDATE servicio_turistico SET prestador_suspendido = TRUE "
      + "WHERE id_prestador IN (:ids) AND NOT prestador_suspendido", nativeQuery = true)
  int suspenderPrestadores(@Param("ids") Collection<Long> ids);

  /** ADR-13: vuelve a mostrar las ofertas de prestadores reactivados. */
  @Modifying
  @Query(value = "UPDATE servicio_turistico SET prestador_suspendido = FALSE "
      + "WHERE prestador_suspendido AND id_prestador NOT IN (:ids)", nativeQuery = true)
  int reactivarExcepto(@Param("ids") Collection<Long> ids);

  @Modifying
  @Query(value = "UPDATE servicio_turistico SET prestador_suspendido = FALSE WHERE prestador_suspendido",
      nativeQuery = true)
  int reactivarTodos();

  /**
   * Bloquea la fila del servicio hasta el fin de la transacción. Las calificaciones de un mismo
   * servicio se registran una tras otra, así el recálculo del promedio ve todas las anteriores.
   */
  @Query(value = "SELECT id FROM servicio_turistico WHERE id = :id FOR UPDATE", nativeQuery = true)
  Optional<Long> bloquearParaCalificar(@Param("id") Long id);

  /** RF080: promedio y total desde todas las calificaciones guardadas, no sumando sobre el valor anterior. */
  @Modifying(flushAutomatically = true)
  @Query(value = """
      UPDATE servicio_turistico s
         SET total_calificaciones  = (SELECT count(*) FROM calificacion c WHERE c.id_servicio = s.id),
             calificacion_promedio = (SELECT round(avg(c.puntuacion), 1) FROM calificacion c WHERE c.id_servicio = s.id)
       WHERE s.id = :id""", nativeQuery = true)
  int recalcularCalificacion(@Param("id") Long id);
}
