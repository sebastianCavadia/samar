package co.edu.unimagdalena.samar.cuentas.servicio.dto;

import java.time.Instant;

public record PrestadorPendienteResponse(
    Long id,
    String nombre,
    String correo,
    String tipoPrestador,
    String nit,
    String rnt,
    String direccion,
    Instant registradoEn) {
}
