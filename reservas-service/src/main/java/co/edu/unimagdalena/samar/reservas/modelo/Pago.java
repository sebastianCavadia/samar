package co.edu.unimagdalena.samar.reservas.modelo;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** RF055: cada intento de pago queda registrado. Nunca se guarda número de tarjeta ni CVV (RF043). */
@Entity
@Table(name = "pago")
public class Pago {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_reserva")
  private Reserva reserva;

  @Column(nullable = false, unique = true, updatable = false)
  private String referencia;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private MedioPago medio;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoPago estado;

  @Column(nullable = false, updatable = false)
  private BigDecimal monto;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn = Instant.now();

  protected Pago() {
  }

  public Pago(String referencia, MedioPago medio, EstadoPago estado, BigDecimal monto) {
    this.referencia = referencia;
    this.medio = medio;
    this.estado = estado;
    this.monto = monto;
  }

  /** Vigente = aprobado o en revisión: mientras exista, no se acepta otro pago. */
  public boolean esVigente() {
    return estado == EstadoPago.APROBADO || estado == EstadoPago.PENDIENTE;
  }

  void setReserva(Reserva reserva) { this.reserva = reserva; }
  public void setEstado(EstadoPago estado) { this.estado = estado; }

  public Long getId() { return id; }
  public String getReferencia() { return referencia; }
  public MedioPago getMedio() { return medio; }
  public EstadoPago getEstado() { return estado; }
  public BigDecimal getMonto() { return monto; }
  public Instant getCreadoEn() { return creadoEn; }
}
