package co.edu.unimagdalena.samar.cuentas.modelo;

import jakarta.persistence.*;

/** RF099: cada administrador tiene un área responsable. */
@Entity
@Table(name = "administrador")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Administrador extends Usuario {

  @Column(name = "area_responsable", nullable = false)
  private String areaResponsable;

  public Administrador() {
    super(Rol.ADMINISTRADOR);
  }

  public String getAreaResponsable() { return areaResponsable; }
  public void setAreaResponsable(String areaResponsable) { this.areaResponsable = areaResponsable; }
}
