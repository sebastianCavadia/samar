package co.edu.unimagdalena.samar.oferta.modelo;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * RF075: puntuación de 1 a 5 y reseña opcional. RF076: una por reserva (id_reserva es UNIQUE);
 * que la reserva esté COMPLETADA lo comprueba reservas-service antes de registrarla (ADR-11).
 */
@Entity
@Table(name = "calificacion")
public class Calificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_servicio", updatable = false)
  private ServicioTuristico servicio;

  /** Usuario TURISTA de cuentas-service (otra base: sin llave foránea). */
  @Column(name = "id_turista", nullable = false, updatable = false)
  private Long idTurista;

  @Column(name = "nombre_turista", updatable = false)
  private String nombreTurista;

  /** Reserva de reservas-service (otra base: sin llave foránea). */
  @Column(name = "id_reserva", nullable = false, updatable = false, unique = true)
  private Long idReserva;

  @Column(nullable = false, updatable = false)
  private short puntuacion;

  @Column(updatable = false)
  private String comentario;

  @Column(name = "respuesta_prestador")
  private String respuestaPrestador;

  @Column(name = "fecha_respuesta")
  private Instant fechaRespuesta;

  @Column(nullable = false, updatable = false)
  private Instant fecha;

  protected Calificacion() {
  }

  public Calificacion(ServicioTuristico servicio, Long idTurista, String nombreTurista, Long idReserva,
      int puntuacion, String comentario, Instant fecha) {
    if (puntuacion < 1 || puntuacion > 5) {
      throw new IllegalArgumentException("La puntuación va de 1 a 5");
    }
    this.servicio = servicio;
    this.idTurista = idTurista;
    this.nombreTurista = nombreTurista;
    this.idReserva = idReserva;
    this.puntuacion = (short) puntuacion;
    this.comentario = comentario;
    this.fecha = fecha;
  }

  /** RF030: respuesta pública del prestador. Puede corregirla; la fecha muestra la última versión. */
  public void responder(String respuesta, Instant ahora) {
    this.respuestaPrestador = respuesta;
    this.fechaRespuesta = ahora;
  }

  public Long getId() { return id; }
  public ServicioTuristico getServicio() { return servicio; }
  public Long getIdTurista() { return idTurista; }
  public String getNombreTurista() { return nombreTurista; }
  public Long getIdReserva() { return idReserva; }
  public int getPuntuacion() { return puntuacion; }
  public String getComentario() { return comentario; }
  public String getRespuestaPrestador() { return respuestaPrestador; }
  public Instant getFechaRespuesta() { return fechaRespuesta; }
  public Instant getFecha() { return fecha; }
}
