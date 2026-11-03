package co.edu.unimagdalena.samar.oferta.modelo;

import jakarta.persistence.*;

/** RF021: toda imagen lleva una descripción (texto alternativo para lectores de pantalla). */
@Entity
@Table(name = "imagen_servicio")
public class ImagenServicio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_servicio")
  private ServicioTuristico servicio;

  @Column(nullable = false)
  private String url;

  @Column(nullable = false)
  private String descripcion;

  protected ImagenServicio() {
  }

  public ImagenServicio(String url, String descripcion) {
    this.url = url;
    this.descripcion = descripcion;
  }

  void setServicio(ServicioTuristico servicio) { this.servicio = servicio; }

  public Long getId() { return id; }
  public String getUrl() { return url; }
  public String getDescripcion() { return descripcion; }
}
