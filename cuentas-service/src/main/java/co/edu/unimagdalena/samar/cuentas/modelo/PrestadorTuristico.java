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

  @Column(nullable = false)
  private boolean validado;

  @Column(name = "validado_en")
  private Instant validadoEn;

  /** Solo agencias: idiomas en los que presta servicio su personal guía. */
  @ElementCollection
  @CollectionTable(name = "agencia_idioma", joinColumns = @JoinColumn(name = "id_usuario"))
  @Column(name = "idioma")
  private Set<String> idiomasGuia = new HashSet<>();

  public PrestadorTuristico() {
    super(Rol.PRESTADOR);
  }

  public void validar(Instant ahora) {
    this.validado = true;
    this.validadoEn = ahora;
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
  public boolean isValidado() { return validado; }
  public Instant getValidadoEn() { return validadoEn; }
  public Set<String> getIdiomasGuia() { return idiomasGuia; }
  public void setIdiomasGuia(Set<String> idiomasGuia) { this.idiomasGuia = idiomasGuia; }
}
