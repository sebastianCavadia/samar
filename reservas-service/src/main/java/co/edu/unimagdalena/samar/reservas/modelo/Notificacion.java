package co.edu.unimagdalena.samar.reservas.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** RF037 y RF039: alerta interna para el turista o el prestador sobre una reserva. */
@Entity
@Table(name = "notificacion")
public class Notificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Usuario de cuentas-service (otra base: sin llave foránea). */
  @Column(name = "id_usuario", nullable = false, updatable = false)
  private Long idUsuario;

  @Column(nullable = false, updatable = false)
  private String asunto;

  @Column(nullable = false, updatable = false)
  private String mensaje;

  @Column(nullable = false)
  private boolean leida;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn;

  protected Notificacion() {
  }

  public Notificacion(Long idUsuario, String asunto, String mensaje, Instant ahora) {
    this.idUsuario = idUsuario;
    this.asunto = asunto;
    this.mensaje = mensaje;
    this.creadoEn = ahora;
  }

  public void marcarLeida() {
    this.leida = true;
  }

  public boolean esDe(Long usuarioId) {
    return idUsuario.equals(usuarioId);
  }

  public Long getId() { return id; }
  public Long getIdUsuario() { return idUsuario; }
  public String getAsunto() { return asunto; }
  public String getMensaje() { return mensaje; }
  public boolean isLeida() { return leida; }
  public Instant getCreadoEn() { return creadoEn; }
}
