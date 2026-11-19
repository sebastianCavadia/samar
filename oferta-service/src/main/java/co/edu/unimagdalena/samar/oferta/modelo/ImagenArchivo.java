package co.edu.unimagdalena.samar.oferta.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** RF019: archivo de imagen subido por un prestador. Inmutable: una imagen nueva es un archivo nuevo. */
@Entity
@Table(name = "imagen_archivo")
public class ImagenArchivo {

  /** Prefijo de la URL pública con la que la galería referencia el archivo. */
  public static final String PREFIJO_URL = "/api/oferta/imagenes/";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Usuario PRESTADOR de cuentas-service (otra base: sin llave foránea). */
  @Column(name = "id_prestador", nullable = false, updatable = false)
  private Long idPrestador;

  @Column(nullable = false, updatable = false)
  private byte[] contenido;

  @Column(nullable = false, updatable = false)
  private String tipo;

  @Column(nullable = false, updatable = false)
  private int tamano;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn;

  protected ImagenArchivo() {
  }

  public ImagenArchivo(Long idPrestador, byte[] contenido, String tipo, Instant ahora) {
    this.idPrestador = idPrestador;
    this.contenido = contenido;
    this.tipo = tipo;
    this.tamano = contenido.length;
    this.creadoEn = ahora;
  }

  public String url() {
    return PREFIJO_URL + id;
  }

  public boolean esDe(Long prestador) {
    return idPrestador.equals(prestador);
  }

  public Long getId() { return id; }
  public Long getIdPrestador() { return idPrestador; }
  public byte[] getContenido() { return contenido; }
  public String getTipo() { return tipo; }
  public int getTamano() { return tamano; }
  public Instant getCreadoEn() { return creadoEn; }
}
