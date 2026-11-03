package co.edu.unimagdalena.samar.oferta.servicio.dto;

import java.util.List;

/** Página de resultados con un formato estable para el frontend (en vez de serializar Page de Spring). */
public record PaginaResponse<T>(
    List<T> contenido,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas) {
}
