package co.edu.unimagdalena.samar.oferta.servicio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Registro (RF016) y edición (RF017) de un servicio turístico.
 * Según "tipo" debe venir el bloque de datos específico (RF023): hospedaje, actividad o gastronomico.
 */
public record ServicioRequest(
    @NotBlank @Pattern(regexp = "HOSPEDAJE|ACTIVIDAD|GASTRONOMICO") String tipo,
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 4000) String descripcion,
    @NotBlank @Pattern(regexp = "RESERVA|WALK_IN") String modalidad,
    @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal precioBase,
    @Pattern(regexp = "[A-Z]{3}") String moneda,
    @NotBlank @Size(max = 200) String direccion,
    @NotBlank @Size(max = 80) String zona,
    @DecimalMin("-90") @DecimalMax("90") Double latitud,
    @DecimalMin("-180") @DecimalMax("180") Double longitud,
    boolean accesible,
    @NotEmpty Set<@Pattern(regexp = "PLAYA|NATURALEZA|CULTURA|AVENTURA|GASTRONOMIA|DESCANSO") String> categorias,
    @Size(max = 10) List<@Valid ImagenDatos> imagenes,
    @Valid HospedajeDatos hospedaje,
    @Valid ActividadDatos actividad,
    @Valid GastronomicoDatos gastronomico) {

  public record ImagenDatos(
      @NotBlank @Size(max = 500) @Pattern(regexp = "https?://.+") String url,
      @NotBlank @Size(max = 200) String descripcion) {
  }

  public record HospedajeDatos(
      @Size(max = 60) String tipoHabitacion,
      @NotNull @Min(1) @Max(500) Integer capacidadPersonas,
      LocalTime horaCheckIn,
      LocalTime horaCheckOut) {
  }

  public record ActividadDatos(
      @DecimalMin("0.5") @Digits(integer = 3, fraction = 1) BigDecimal duracionHoras,
      LocalTime horaInicio,
      @Pattern(regexp = "BAJA|MEDIA|ALTA") String dificultad,
      @Min(0) @Max(99) Integer edadMinima) {
  }

  public record GastronomicoDatos(
      @Size(max = 60) String tipoComida,
      @Min(1) Integer aforo,
      LocalTime horaApertura,
      LocalTime horaCierre,
      Set<@Pattern(regexp = "VEGETARIANA|VEGANA|CELIACA|KOSHER|HALAL") String> dietas) {
  }
}
