package co.edu.unimagdalena.samar.oferta.servicio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** RF074: información completa. Solo uno de hospedaje/actividad/gastronomico viene con datos. */
public record ServicioDetalleResponse(
    Long id,
    Long idPrestador,
    String tipo,
    String nombre,
    String descripcion,
    String modalidad,
    BigDecimal precioBase,
    String moneda,
    String direccion,
    String zona,
    Double latitud,
    Double longitud,
    boolean accesible,
    boolean activo,
    BigDecimal calificacionPromedio,
    int totalCalificaciones,
    boolean nuevo,
    Set<String> categorias,
    List<ServicioRequest.ImagenDatos> imagenes,
    ServicioRequest.HospedajeDatos hospedaje,
    ServicioRequest.ActividadDatos actividad,
    ServicioRequest.GastronomicoDatos gastronomico,
    Instant creadoEn) {
}
