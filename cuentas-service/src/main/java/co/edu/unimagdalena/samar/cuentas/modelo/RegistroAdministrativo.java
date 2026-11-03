package co.edu.unimagdalena.samar.cuentas.modelo;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;

/**
 * RF097: qué hizo un administrador, sobre quién, cuándo y por qué.
 * Guarda una copia del área responsable (RF099) por si luego el administrador cambia de área.
 */
@Entity
@Table(name = "bitacora_admin")
@BatchSize(size = 20)
public class RegistroAdministrativo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_administrador", updatable = false)
  private Usuario administrador;

  @Column(name = "area_responsable", updatable = false)
  private String areaResponsable;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_usuario", updatable = false)
  private Usuario usuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private AccionAdministrativa accion;

  @Column(updatable = false)
  private String detalle;

  @Column(nullable = false, updatable = false)
  private Instant fecha;

  protected RegistroAdministrativo() {
  }

  public RegistroAdministrativo(Administrador administrador, Usuario usuario, AccionAdministrativa accion,
      String detalle, Instant fecha) {
    this.administrador = administrador;
    this.areaResponsable = administrador.getAreaResponsable();
    this.usuario = usuario;
    this.accion = accion;
    this.detalle = detalle;
    this.fecha = fecha;
  }

  public Long getId() { return id; }
  public Usuario getAdministrador() { return administrador; }
  public String getAreaResponsable() { return areaResponsable; }
  public Usuario getUsuario() { return usuario; }
  public AccionAdministrativa getAccion() { return accion; }
  public String getDetalle() { return detalle; }
  public Instant getFecha() { return fecha; }
}
