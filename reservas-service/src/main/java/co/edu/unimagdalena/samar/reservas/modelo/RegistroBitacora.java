package co.edu.unimagdalena.samar.reservas.modelo;

import jakarta.persistence.*;

import java.time.Instant;

/** RF057: quién cambió el estado de la reserva y cuándo. id_usuario nulo = proceso automático. */
@Entity
@Table(name = "bitacora_reserva")
public class RegistroBitacora {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_reserva")
  private Reserva reserva;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_anterior")
  private EstadoReserva estadoAnterior;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_nuevo", nullable = false)
  private EstadoReserva estadoNuevo;

  @Column(name = "id_usuario")
  private Long idUsuario;

  private String detalle;

  @Column(nullable = false)
  private Instant fecha = Instant.now();

  protected RegistroBitacora() {
  }

  RegistroBitacora(Reserva reserva, EstadoReserva anterior, EstadoReserva nuevo, Long idUsuario, String detalle) {
    this.reserva = reserva;
    this.estadoAnterior = anterior;
    this.estadoNuevo = nuevo;
    this.idUsuario = idUsuario;
    this.detalle = detalle;
  }

  public EstadoReserva getEstadoAnterior() { return estadoAnterior; }
  public EstadoReserva getEstadoNuevo() { return estadoNuevo; }
  public Long getIdUsuario() { return idUsuario; }
  public String getDetalle() { return detalle; }
  public Instant getFecha() { return fecha; }
}
