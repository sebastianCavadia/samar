package co.edu.unimagdalena.samar.reservas.persistencia;

import co.edu.unimagdalena.samar.reservas.modelo.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

  List<Disponibilidad> findByIdServicioAndFechaBetweenOrderByFechaAscHoraAsc(
      Long idServicio, LocalDate desde, LocalDate hasta);

  List<Disponibilidad> findByIdServicioAndActivoTrueAndFechaBetweenOrderByFechaAscHoraAsc(
      Long idServicio, LocalDate desde, LocalDate hasta);

  /** hora null se traduce a "hora IS NULL" (fecha sin horario). */
  Optional<Disponibilidad> findByIdServicioAndFechaAndHora(Long idServicio, LocalDate fecha, LocalTime hora);

  /**
   * Ocupa cupos de forma atómica (ADR-07): la condición y el incremento ocurren en una sola
   * sentencia, así que dos reservas simultáneas nunca pueden sobrevender el último cupo.
   * @return 1 si se ocuparon, 0 si no había cupos suficientes o la fecha está bloqueada
   */
  @Modifying(flushAutomatically = true)
  @Query("""
      update Disponibilidad d
         set d.cuposReservados = d.cuposReservados + :cantidad, d.version = d.version + 1
       where d.id = :id and d.activo = true and d.cuposReservados + :cantidad <= d.cuposTotales""")
  int ocuparCupos(@Param("id") Long id, @Param("cantidad") int cantidad);

  /** RF035/RF040: devuelve cupos al cancelar, rechazar o expirar una reserva. */
  @Modifying(flushAutomatically = true)
  @Query("""
      update Disponibilidad d
         set d.cuposReservados = d.cuposReservados - :cantidad, d.version = d.version + 1
       where d.id = :id and d.cuposReservados >= :cantidad""")
  int liberarCupos(@Param("id") Long id, @Param("cantidad") int cantidad);
}
