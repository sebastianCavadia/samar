package co.edu.unimagdalena.samar.cuentas.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** RF008: foto de perfil. Tabla aparte de usuario para no leer los bytes en cada consulta. */
@Entity
@Table(name = "foto_perfil")
public class FotoPerfil {

  @Id
  @Column(name = "id_usuario")
  private Long idUsuario;

  @Column(nullable = false)
  private byte[] contenido;

  @Column(nullable = false)
  private String tipo;

  @Column(name = "actualizada_en", nullable = false)
  private Instant actualizadaEn;

  protected FotoPerfil() {
  }

  public FotoPerfil(Long idUsuario, byte[] contenido, String tipo, Instant ahora) {
    this.idUsuario = idUsuario;
    reemplazar(contenido, tipo, ahora);
  }

  public void reemplazar(byte[] contenido, String tipo, Instant ahora) {
    this.contenido = contenido;
    this.tipo = tipo;
    this.actualizadaEn = ahora;
  }

  public Long getIdUsuario() { return idUsuario; }
  public byte[] getContenido() { return contenido; }
  public String getTipo() { return tipo; }
  public Instant getActualizadaEn() { return actualizadaEn; }
}
