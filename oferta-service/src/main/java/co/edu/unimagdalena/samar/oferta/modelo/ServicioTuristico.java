package co.edu.unimagdalena.samar.oferta.modelo;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Datos comunes de toda oferta. Herencia JOINED: tabla servicio_turistico + una tabla por
 * tipo (hospedaje, actividad_turistica, servicio_gastronomico). La columna "tipo" es el
 * discriminador que Hibernate llena solo según la subclase.
 */
@Entity
@Table(name = "servicio_turistico")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class ServicioTuristico {

  private static final Duration VENTANA_NUEVO = Duration.ofDays(30);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Usuario PRESTADOR de cuentas-service (otra base: sin llave foránea). */
  @Column(name = "id_prestador", nullable = false, updatable = false)
  private Long idPrestador;

  @Column(nullable = false)
  private String nombre;

  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Modalidad modalidad = Modalidad.RESERVA;

  @Column(name = "precio_base", nullable = false)
  private BigDecimal precioBase;

  @Column(nullable = false)
  private String moneda = "COP";

  private String direccion;
  private String zona;
  private Double latitud;
  private Double longitud;

  @Column(nullable = false)
  private boolean accesible;

  @Column(nullable = false)
  private boolean activo = true;

  @Column(name = "calificacion_promedio")
  private BigDecimal calificacionPromedio;

  @Column(name = "total_calificaciones", nullable = false)
  private int totalCalificaciones;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn = Instant.now();

  @ElementCollection
  @CollectionTable(name = "servicio_categoria", joinColumns = @JoinColumn(name = "id_servicio"))
  @Column(name = "categoria")
  @Enumerated(EnumType.STRING)
  @BatchSize(size = 20)
  private Set<Categoria> categorias = new HashSet<>();

  @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  @BatchSize(size = 20)
  private List<ImagenServicio> imagenes = new ArrayList<>();

  public abstract TipoServicio getTipo();

  public boolean perteneceA(Long prestadorId) {
    return idPrestador.equals(prestadorId);
  }

  /** RF082: etiqueta "Nuevo" durante los primeros 30 días. */
  public boolean esNuevo(Instant ahora) {
    return creadoEn.isAfter(ahora.minus(VENTANA_NUEVO));
  }

  public void reemplazarCategorias(Collection<Categoria> nuevas) {
    categorias.clear();
    categorias.addAll(nuevas);
  }

  public void reemplazarImagenes(Collection<ImagenServicio> nuevas) {
    imagenes.clear();
    for (ImagenServicio imagen : nuevas) {
      imagen.setServicio(this);
      imagenes.add(imagen);
    }
  }

  public Long getId() { return id; }
  public Long getIdPrestador() { return idPrestador; }
  public void setIdPrestador(Long idPrestador) { this.idPrestador = idPrestador; }
  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
  public Modalidad getModalidad() { return modalidad; }
  public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }
  public BigDecimal getPrecioBase() { return precioBase; }
  public void setPrecioBase(BigDecimal precioBase) { this.precioBase = precioBase; }
  public String getMoneda() { return moneda; }
  public void setMoneda(String moneda) { this.moneda = moneda; }
  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }
  public String getZona() { return zona; }
  public void setZona(String zona) { this.zona = zona; }
  public Double getLatitud() { return latitud; }
  public void setLatitud(Double latitud) { this.latitud = latitud; }
  public Double getLongitud() { return longitud; }
  public void setLongitud(Double longitud) { this.longitud = longitud; }
  public boolean isAccesible() { return accesible; }
  public void setAccesible(boolean accesible) { this.accesible = accesible; }
  public boolean isActivo() { return activo; }
  public void setActivo(boolean activo) { this.activo = activo; }
  public BigDecimal getCalificacionPromedio() { return calificacionPromedio; }
  public int getTotalCalificaciones() { return totalCalificaciones; }
  public Instant getCreadoEn() { return creadoEn; }
  public Set<Categoria> getCategorias() { return Collections.unmodifiableSet(categorias); }
  public List<ImagenServicio> getImagenes() { return Collections.unmodifiableList(imagenes); }
}
