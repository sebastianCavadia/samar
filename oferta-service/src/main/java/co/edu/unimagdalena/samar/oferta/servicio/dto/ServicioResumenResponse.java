package co.edu.unimagdalena.samar.oferta.servicio.dto;

import java.math.BigDecimal;
import java.util.Set;

/** Tarjeta del listado (RF077) y de "Mis servicios". */
public record ServicioResumenResponse(
    Long id,
    String tipo,
    String nombre,
    String zona,
    BigDecimal precioBase,
    String moneda,
    boolean accesible,
    boolean activo,
    BigDecimal calificacionPromedio,
    int totalCalificaciones,
    boolean nuevo,
    Set<String> categorias,
    ServicioRequest.ImagenDatos imagenPrincipal) {
}
