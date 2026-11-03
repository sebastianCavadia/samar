package co.edu.unimagdalena.samar.oferta.servicio.dto;

import java.math.BigDecimal;

/** Parámetros opcionales de búsqueda del catálogo (RF073). Llegan como query params. */
public record FiltroBusqueda(
    String q,
    String tipo,
    String categoria,
    String zona,
    BigDecimal precioMin,
    BigDecimal precioMax,
    BigDecimal calificacionMin,
    Boolean accesible) {
}
