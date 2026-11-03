package co.edu.unimagdalena.samar.oferta.servicio.dto;

import java.math.BigDecimal;

/**
 * Lo que reservas-service necesita para crear una reserva: dueño, precio vigente (RF047),
 * modalidad (WALK_IN no se reserva), capacidad (RF064) y edad mínima (RF065).
 */
public record ServicioInternoResponse(
    Long id,
    Long idPrestador,
    String tipo,
    String nombre,
    String modalidad,
    BigDecimal precioBase,
    String moneda,
    boolean activo,
    Integer capacidadPersonas,
    Integer edadMinima) {
}
