package co.edu.unimagdalena.samar.reservas.modelo;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Servicio incluido en una reserva. Copia nombre, tipo y precio al reservar (RF047):
 * si el prestador cambia la tarifa después, la reserva conserva el precio pactado.
 */
@Entity
@Table(name = "detalle_reserva")
public class DetalleReserva {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_reserva")
  private Reserva reserva;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_disponibilidad")
  private Disponibilidad disponibilidad;

  @Column(name = "id_servicio", nullable = false)
  private Long idServicio;

  @Column(name = "id_prestador", nullable = false)
  private Long idPrestador;

  @Column(name = "nombre_servicio", nullable = false)
  private String nombreServicio;

  @Column(name = "tipo_servicio")
  private String tipoServicio;

  /** Cupos que ocupa: 1 habitación en hospedaje, N personas en actividades y restaurantes. */
  @Column(nullable = false)
  private int cantidad;

  @Column(name = "precio_unitario", nullable = false)
  private BigDecimal precioUnitario;

  @Column(nullable = false)
  private BigDecimal subtotal;

  protected DetalleReserva() {
  }

  public DetalleReserva(Disponibilidad disponibilidad, Long idServicio, Long idPrestador, String nombreServicio,
      String tipoServicio, int cantidad, BigDecimal precioUnitario) {
    this.disponibilidad = disponibilidad;
    this.idServicio = idServicio;
    this.idPrestador = idPrestador;
    this.nombreServicio = nombreServicio;
    this.tipoServicio = tipoServicio;
    this.cantidad = cantidad;
    this.precioUnitario = precioUnitario;
    this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
  }

  void setReserva(Reserva reserva) { this.reserva = reserva; }

  public Long getId() { return id; }
  public Disponibilidad getDisponibilidad() { return disponibilidad; }
  public Long getIdServicio() { return idServicio; }
  public Long getIdPrestador() { return idPrestador; }
  public String getNombreServicio() { return nombreServicio; }
  public String getTipoServicio() { return tipoServicio; }
  public int getCantidad() { return cantidad; }
  public BigDecimal getPrecioUnitario() { return precioUnitario; }
  public BigDecimal getSubtotal() { return subtotal; }
}
