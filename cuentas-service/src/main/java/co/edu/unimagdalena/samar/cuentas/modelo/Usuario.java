package co.edu.unimagdalena.samar.cuentas.modelo;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Datos comunes de toda cuenta. Herencia JOINED: tabla usuario + una tabla por subtipo
 * (turista, prestador_turistico, administrador), igual que en el diagrama ER.
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nombre;

  @Column(nullable = false, unique = true)
  private String correo;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  private String telefono;

  @Column(nullable = false)
  private String idioma = "es";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Rol rol;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoCuenta estado = EstadoCuenta.ACTIVO;

  @Column(name = "intentos_fallidos", nullable = false)
  private int intentosFallidos;

  @Column(name = "bloqueado_hasta")
  private Instant bloqueadoHasta;

  @Column(name = "ultimo_acceso")
  private Instant ultimoAcceso;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn = Instant.now();

  protected Usuario() {
  }

  protected Usuario(Rol rol) {
    this.rol = rol;
  }

  /** RF005: true si sigue dentro de la ventana de bloqueo temporal. */
  public boolean estaBloqueadoTemporalmente(Instant ahora) {
    return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
  }

  /** RF005: suma un intento fallido y bloquea al alcanzar el máximo. */
  public void registrarIntentoFallido(int maxIntentos, Instant bloquearHasta) {
    intentosFallidos++;
    if (intentosFallidos >= maxIntentos) {
      bloqueadoHasta = bloquearHasta;
      intentosFallidos = 0;
    }
  }

  /** RF013: login correcto reinicia contadores y registra el último acceso. */
  public void registrarAccesoExitoso(Instant ahora) {
    intentosFallidos = 0;
    bloqueadoHasta = null;
    ultimoAcceso = ahora;
  }

  public Long getId() { return id; }
  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
  public String getCorreo() { return correo; }
  public void setCorreo(String correo) { this.correo = correo; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getTelefono() { return telefono; }
  public void setTelefono(String telefono) { this.telefono = telefono; }
  public String getIdioma() { return idioma; }
  public void setIdioma(String idioma) { this.idioma = idioma; }
  public Rol getRol() { return rol; }
  public EstadoCuenta getEstado() { return estado; }
  public void setEstado(EstadoCuenta estado) { this.estado = estado; }
  public int getIntentosFallidos() { return intentosFallidos; }
  public Instant getBloqueadoHasta() { return bloqueadoHasta; }
  public Instant getUltimoAcceso() { return ultimoAcceso; }
  public Instant getCreadoEn() { return creadoEn; }
}
