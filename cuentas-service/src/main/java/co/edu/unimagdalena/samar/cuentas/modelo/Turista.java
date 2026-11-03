package co.edu.unimagdalena.samar.cuentas.modelo;

import jakarta.persistence.*;

import java.time.LocalDate;

/** RF002: datos propios del turista. */
@Entity
@Table(name = "turista")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Turista extends Usuario {

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_documento", nullable = false)
  private TipoDocumento tipoDocumento;

  @Column(name = "numero_documento", nullable = false, unique = true)
  private String numeroDocumento;

  private String nacionalidad;

  @Column(name = "pais_residencia")
  private String paisResidencia;

  @Column(name = "fecha_nacimiento")
  private LocalDate fechaNacimiento;

  @Column(nullable = false)
  private boolean discapacidad;

  public Turista() {
    super(Rol.TURISTA);
  }

  public TipoDocumento getTipoDocumento() { return tipoDocumento; }
  public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }
  public String getNumeroDocumento() { return numeroDocumento; }
  public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
  public String getNacionalidad() { return nacionalidad; }
  public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
  public String getPaisResidencia() { return paisResidencia; }
  public void setPaisResidencia(String paisResidencia) { this.paisResidencia = paisResidencia; }
  public LocalDate getFechaNacimiento() { return fechaNacimiento; }
  public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
  public boolean isDiscapacidad() { return discapacidad; }
  public void setDiscapacidad(boolean discapacidad) { this.discapacidad = discapacidad; }
}
