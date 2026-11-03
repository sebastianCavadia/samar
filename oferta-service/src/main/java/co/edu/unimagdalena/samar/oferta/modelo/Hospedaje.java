package co.edu.unimagdalena.samar.oferta.modelo;

import jakarta.persistence.*;

import java.time.LocalTime;

/** RF023 (hoteles) y RF064: la capacidad limita el número de personas por reserva. */
@Entity
@Table(name = "hospedaje")
@PrimaryKeyJoinColumn(name = "id_servicio")
@DiscriminatorValue("HOSPEDAJE")
public class Hospedaje extends ServicioTuristico {

  @Column(name = "tipo_habitacion")
  private String tipoHabitacion;

  @Column(name = "capacidad_personas", nullable = false)
  private int capacidadPersonas;

  @Column(name = "hora_check_in")
  private LocalTime horaCheckIn;

  @Column(name = "hora_check_out")
  private LocalTime horaCheckOut;

  @Override
  public TipoServicio getTipo() {
    return TipoServicio.HOSPEDAJE;
  }

  public String getTipoHabitacion() { return tipoHabitacion; }
  public void setTipoHabitacion(String tipoHabitacion) { this.tipoHabitacion = tipoHabitacion; }
  public int getCapacidadPersonas() { return capacidadPersonas; }
  public void setCapacidadPersonas(int capacidadPersonas) { this.capacidadPersonas = capacidadPersonas; }
  public LocalTime getHoraCheckIn() { return horaCheckIn; }
  public void setHoraCheckIn(LocalTime horaCheckIn) { this.horaCheckIn = horaCheckIn; }
  public LocalTime getHoraCheckOut() { return horaCheckOut; }
  public void setHoraCheckOut(LocalTime horaCheckOut) { this.horaCheckOut = horaCheckOut; }
}
