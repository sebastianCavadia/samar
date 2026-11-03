package co.edu.unimagdalena.samar.reservas.modelo;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * RF036-RF049. La reserva protege su propia máquina de estados: solo cambia con
 * {@link #cambiarEstado}, que valida la transición y deja el registro en la bitácora (RF057).
 */
@Entity
@Table(name = "reserva")
public class Reserva {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Usuario TURISTA de cuentas-service (otra base: sin llave foránea). */
  @Column(name = "id_turista", nullable = false, updatable = false)
  private Long idTurista;

  @Column(name = "nombre_turista", updatable = false)
  private String nombreTurista;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoReserva estado = EstadoReserva.PENDIENTE;

  @Column(nullable = false)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(nullable = false)
  private String moneda = "COP";

  @Column(nullable = false, updatable = false)
  private int personas;

  private String motivo;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn = Instant.now();

  @Version
  private Long version;

  @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 20)
  private List<DetalleReserva> detalles = new ArrayList<>();

  @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL)
  @OrderBy("creadoEn DESC")
  @BatchSize(size = 20)
  private List<Pago> pagos = new ArrayList<>();

  @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL)
  @OrderBy("fecha ASC")
  @BatchSize(size = 20)
  private List<RegistroBitacora> historial = new ArrayList<>();

  protected Reserva() {
  }

  public Reserva(Long idTurista, String nombreTurista, int personas, String moneda, Long idUsuarioCreador) {
    this.idTurista = idTurista;
    this.nombreTurista = nombreTurista;
    this.personas = personas;
    this.moneda = moneda;
    historial.add(new RegistroBitacora(this, null, EstadoReserva.PENDIENTE, idUsuarioCreador, "Reserva creada"));
  }

  /** RF046: el total es la suma de los subtotales (impuestos y comisión: pendiente de definir). */
  public void agregarDetalle(DetalleReserva detalle) {
    detalle.setReserva(this);
    detalles.add(detalle);
    total = detalles.stream().map(DetalleReserva::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public void registrarPago(Pago pago) {
    pago.setReserva(this);
    pagos.add(pago);
  }

  /**
   * Única forma de cambiar el estado.
   * @throws IllegalStateException si la transición no está permitida (la capa servicio valida antes)
   */
  public void cambiarEstado(EstadoReserva nuevo, Long idUsuario, String detalle) {
    if (!estado.puedePasarA(nuevo)) {
      throw new IllegalStateException("Transición no permitida: " + estado + " → " + nuevo);
    }
    historial.add(new RegistroBitacora(this, estado, nuevo, idUsuario, detalle));
    estado = nuevo;
  }

  public Optional<Pago> pagoVigente() {
    return pagos.stream().filter(Pago::esVigente).findFirst();
  }

  public boolean tienePagoAprobado() {
    return pagos.stream().anyMatch(p -> p.getEstado() == EstadoPago.APROBADO);
  }

  /** Por ahora una reserva tiene un solo servicio; los paquetes (RF050) usarán varios detalles. */
  public DetalleReserva detallePrincipal() {
    return detalles.get(0);
  }

  public boolean esDelTurista(Long usuarioId) {
    return idTurista.equals(usuarioId);
  }

  public boolean esAtendidaPor(Long prestadorId) {
    return detalles.stream().anyMatch(d -> d.getIdPrestador().equals(prestadorId));
  }

  public void setMotivo(String motivo) { this.motivo = motivo; }

  public Long getId() { return id; }
  public Long getIdTurista() { return idTurista; }
  public String getNombreTurista() { return nombreTurista; }
  public EstadoReserva getEstado() { return estado; }
  public BigDecimal getTotal() { return total; }
  public String getMoneda() { return moneda; }
  public int getPersonas() { return personas; }
  public String getMotivo() { return motivo; }
  public Instant getCreadoEn() { return creadoEn; }
  public List<DetalleReserva> getDetalles() { return Collections.unmodifiableList(detalles); }
  public List<Pago> getPagos() { return Collections.unmodifiableList(pagos); }
  public List<RegistroBitacora> getHistorial() { return Collections.unmodifiableList(historial); }
}
