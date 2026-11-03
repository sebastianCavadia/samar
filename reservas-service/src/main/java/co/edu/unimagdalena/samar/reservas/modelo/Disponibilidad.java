package co.edu.unimagdalena.samar.reservas.modelo;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * RF018 / RF035: cupos de un servicio para una fecha (y hora opcional).
 * Vive en reservas (ADR-07) para descontar cupos y crear la reserva en una sola transacción.
 * Los cupos se ocupan y liberan con UPDATE atómicos (DisponibilidadRepository), no con setters.
 */
@Entity
@Table(name = "disponibilidad")
@BatchSize(size = 20)
public class Disponibilidad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Servicio de oferta-service (otra base: sin llave foránea). */
  @Column(name = "id_servicio", nullable = false, updatable = false)
  private Long idServicio;

  @Column(nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(updatable = false)
  private LocalTime hora;

  @Column(name = "cupos_totales", nullable = false)
  private int cuposTotales;

  @Column(name = "cupos_reservados", nullable = false, insertable = false, updatable = false)
  private int cuposReservados;

  @Column(nullable = false)
  private boolean activo = true;

  @Version
  private Long version;

  protected Disponibilidad() {
  }

  public Disponibilidad(Long idServicio, LocalDate fecha, LocalTime hora, int cuposTotales) {
    this.idServicio = idServicio;
    this.fecha = fecha;
    this.hora = hora;
    this.cuposTotales = cuposTotales;
  }

  public int cuposLibres() {
    return Math.max(cuposTotales - cuposReservados, 0);
  }

  public Long getId() { return id; }
  public Long getIdServicio() { return idServicio; }
  public LocalDate getFecha() { return fecha; }
  public LocalTime getHora() { return hora; }
  public int getCuposTotales() { return cuposTotales; }
  public void setCuposTotales(int cuposTotales) { this.cuposTotales = cuposTotales; }
  public int getCuposReservados() { return cuposReservados; }
  public boolean isActivo() { return activo; }
  public void setActivo(boolean activo) { this.activo = activo; }
}
