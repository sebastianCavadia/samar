package co.edu.unimagdalena.samar.cuentas.modelo;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * RF003: hotel, restaurante o agencia turística. La cuenta queda pendiente
 * hasta que un Administrador valide su documentación (RF095).
 */
@Entity
@Table(name = "prestador_turistico")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class PrestadorTuristico extends Usuario {

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_prestador", nullable = false)
  private TipoPrestador tipoPrestador;

  @Column(nullable = false, unique = true)
  private String nit;

  @Column(nullable = false, unique = true)
  private String rnt;

  @Column(nullable = false)
  private String direccion;

  private String zona;
  private Double latitud;
  private Double longitud;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_validacion", nullable = false)
  private EstadoValidacion estadoValidacion = EstadoValidacion.PENDIENTE;

  @Column(name = "validado_en")
  private Instant validadoEn;

  @Column(name = "motivo_rechazo")
  private String motivoRechazo;

  /** Solo agencias: idiomas en los que presta servicio su personal guía. */
  @ElementCollection
  @CollectionTable(name = "agencia_idioma", joinColumns = @JoinColumn(name = "id_usuario"))
  @Column(name = "idioma")
  private Set<String> idiomasGuia = new HashSet<>();

  public PrestadorTuristico() {
    super(Rol.PRESTADOR);
  }

  /** Se valida una solicitud pendiente o una rechazada que el administrador reconsidera. */
  public void validar(Instant ahora) {
    this.estadoValidacion = EstadoValidacion.VALIDADO;
    this.validadoEn = ahora;
    this.motivoRechazo = null;
  }

  public void rechazar(String motivo) {
    this.estadoValidacion = EstadoValidacion.RECHAZADO;
    this.motivoRechazo = motivo;
  }

  public boolean isValidado() {
    return estadoValidacion == EstadoValidacion.VALIDADO;
  }

  public TipoPrestador getTipoPrestador() { return tipoPrestador; }
  public void setTipoPrestador(TipoPrestador tipoPrestador) { this.tipoPrestador = tipoPrestador; }
  public String getNit() { return nit; }
  public void setNit(String nit) { this.nit = nit; }
  public String getRnt() { return rnt; }
  public void setRnt(String rnt) { this.rnt = rnt; }
  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }
  public String getZona() { return zona; }
  public void setZona(String zona) { this.zona = zona; }
  public Double getLatitud() { return latitud; }
  public void setLatitud(Double latitud) { this.latitud = latitud; }
  public Double getLongitud() { return longitud; }
  public void setLongitud(Double longitud) { this.longitud = longitud; }
  public EstadoValidacion getEstadoValidacion() { return estadoValidacion; }
  public Instant getValidadoEn() { return validadoEn; }
  public String getMotivoRechazo() { return motivoRechazo; }
  public Set<String> getIdiomasGuia() { return idiomasGuia; }
  public void setIdiomasGuia(Set<String> idiomasGuia) { this.idiomasGuia = idiomasGuia; }
}
