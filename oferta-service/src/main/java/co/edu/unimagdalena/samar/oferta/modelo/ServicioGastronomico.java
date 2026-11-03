package co.edu.unimagdalena.samar.oferta.modelo;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** RF023 (restaurantes) y RF079 (dietas). */
@Entity
@Table(name = "servicio_gastronomico")
@PrimaryKeyJoinColumn(name = "id_servicio")
@DiscriminatorValue("GASTRONOMICO")
public class ServicioGastronomico extends ServicioTuristico {

  @Column(name = "tipo_comida")
  private String tipoComida;

  private Integer aforo;

  @Column(name = "hora_apertura")
  private LocalTime horaApertura;

  @Column(name = "hora_cierre")
  private LocalTime horaCierre;

  @ElementCollection
  @CollectionTable(name = "servicio_gastronomico_dieta", joinColumns = @JoinColumn(name = "id_servicio"))
  @Column(name = "tipo_dieta")
  @Enumerated(EnumType.STRING)
  @BatchSize(size = 20)
  private Set<TipoDieta> dietas = new HashSet<>();

  @Override
  public TipoServicio getTipo() {
    return TipoServicio.GASTRONOMICO;
  }

  public void reemplazarDietas(Collection<TipoDieta> nuevas) {
    dietas.clear();
    dietas.addAll(nuevas);
  }

  public String getTipoComida() { return tipoComida; }
  public void setTipoComida(String tipoComida) { this.tipoComida = tipoComida; }
  public Integer getAforo() { return aforo; }
  public void setAforo(Integer aforo) { this.aforo = aforo; }
  public LocalTime getHoraApertura() { return horaApertura; }
  public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }
  public LocalTime getHoraCierre() { return horaCierre; }
  public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }
  public Set<TipoDieta> getDietas() { return Collections.unmodifiableSet(dietas); }
}
