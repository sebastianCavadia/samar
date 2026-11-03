package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Peticiones y respuestas del panel del administrador (RF091-RF100). */
public final class Admin {

  private Admin() {
  }

  // ---------- Peticiones ----------

  /** RF093 + RF100: el motivo es obligatorio y se le comunica al usuario. */
  public record CambiarEstado(@NotBlank @Pattern(regexp = "ACTIVO|INACTIVO|BLOQUEADO") String estado,
      @NotBlank @Size(max = 300) String motivo) {
  }

  /** RF094: datos que el administrador puede corregir. */
  public record EditarUsuario(@NotBlank @Size(max = 120) String nombre, @Size(max = 40) String telefono) {
  }

  /** RF093 + RF099: un administrador crea a otro, con su área responsable. */
  public record CrearAdministrador(@NotBlank @Size(max = 120) String nombre, @NotBlank @Email String correo,
      @NotBlank @Size(min = 8, max = 72) String password, @NotBlank @Size(max = 120) String areaResponsable) {
  }

  /** RF095: rechazar una solicitud siempre lleva el motivo, que verá el prestador. */
  public record Rechazar(@NotBlank @Size(max = 300) String motivo) {
  }

  // ---------- Respuestas ----------

  public record Resumen(long prestadoresPendientes, long usuarios, long turistas, long prestadores,
      long administradores, long inactivos, long bloqueados) {
  }

  /** Fila de la lista de usuarios (RF091). */
  public record UsuarioFila(Long id, String nombre, String correo, String rol, String estado,
      Instant ultimoAcceso, String estadoValidacion) {
  }

  public record DatosTurista(String tipoDocumento, String numeroDocumento, String nacionalidad,
      String paisResidencia, LocalDate fechaNacimiento, boolean discapacidad) {
  }

  /** Solicitud de prestador (RF095) y datos de prestador en la ficha (RF092). */
  public record Prestador(Long id, String nombre, String correo, String telefono, String tipoPrestador,
      String nit, String rnt, String direccion, String zona, Set<String> idiomasGuia, String estadoCuenta,
      String estadoValidacion, String motivoRechazo, Instant registradoEn, Instant validadoEn) {
  }

  public record Evento(Long id, Instant fecha, Long idAdministrador, String administrador, String areaResponsable,
      Long idUsuario, String usuario, String accion, String detalle) {
  }

  /** RF092: ficha completa. Solo uno de turista, prestador o areaResponsable viene con datos. */
  public record UsuarioFicha(Long id, String nombre, String correo, String telefono, String rol, String estado,
      String idioma, Instant creadoEn, Instant ultimoAcceso, Instant bloqueadoHasta, DatosTurista turista,
      Prestador prestador, String areaResponsable, List<Evento> historial) {
  }
}
