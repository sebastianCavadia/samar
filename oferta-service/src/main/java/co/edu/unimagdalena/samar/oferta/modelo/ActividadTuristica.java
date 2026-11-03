package co.edu.unimagdalena.samar.oferta.modelo;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalTime;

/** RF023 (actividades) y RF065: edad mínima que reservas valida contra la fecha de nacimiento. */
@Entity
@Table(name = "actividad_turistica")
@PrimaryKeyJoinColumn(name = "id_servicio")
@DiscriminatorValue("ACTIVIDAD")
public class ActividadTuristica extends ServicioTuristico {

  @Column(name = "duracion_horas")
  private BigDecimal duracionHoras;

  @Column(name = "hora_inicio")
  private LocalTime horaInicio;

  @Enumerated(EnumType.STRING)
  private Dificultad dificultad;

  @Column(name = "edad_minima")
  private Integer edadMinima;

  @Override
  public TipoServicio getTipo() {
    return TipoServicio.ACTIVIDAD;
  }

  public BigDecimal getDuracionHoras() { return duracionHoras; }
  public void setDuracionHoras(BigDecimal duracionHoras) { this.duracionHoras = duracionHoras; }
  public LocalTime getHoraInicio() { return horaInicio; }
  public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
  public Dificultad getDificultad() { return dificultad; }
  public void setDificultad(Dificultad dificultad) { this.dificultad = dificultad; }
  public Integer getEdadMinima() { return edadMinima; }
  public void setEdadMinima(Integer edadMinima) { this.edadMinima = edadMinima; }
}
