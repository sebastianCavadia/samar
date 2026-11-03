package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.modelo.Categoria;
import co.edu.unimagdalena.samar.oferta.modelo.ServicioTuristico;
import co.edu.unimagdalena.samar.oferta.modelo.TipoServicio;
import co.edu.unimagdalena.samar.oferta.persistencia.ServicioEspecificaciones;
import co.edu.unimagdalena.samar.oferta.persistencia.ServicioTuristicoRepository;
import co.edu.unimagdalena.samar.oferta.servicio.dto.*;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.DatosInvalidosException;
import co.edu.unimagdalena.samar.oferta.servicio.excepcion.RecursoNoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Consulta del catálogo: pública (RF073, RF074, RF077) e interna para otros servicios. */
@Service
public class CatalogoService {

  /** RF073 / RF077: resultados de 10 en 10. */
  static final int TAMANO_PAGINA = 10;

  private final ServicioTuristicoRepository servicios;
  private final Clock clock;

  public CatalogoService(ServicioTuristicoRepository servicios, Clock clock) {
    this.servicios = servicios;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PaginaResponse<ServicioResumenResponse> buscar(FiltroBusqueda f, int pagina, String orden) {
    if (f.precioMin() != null && f.precioMax() != null && f.precioMin().compareTo(f.precioMax()) > 0) {
      throw new DatosInvalidosException("El precio mínimo no puede ser mayor que el máximo");
    }
    var especificacion = ServicioEspecificaciones.publicadosCon(
        textoOpcional(f.q()),
        Enums.opcional(TipoServicio.class, f.tipo(), "tipo"),
        Enums.opcional(Categoria.class, f.categoria(), "categoria"),
        textoOpcional(f.zona()),
        f.precioMin(), f.precioMax(), f.calificacionMin(), f.accesible());

    Page<ServicioTuristico> resultado = servicios.findAll(especificacion,
        PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA, ordenDe(orden)));

    var ahora = clock.instant();
    return new PaginaResponse<>(
        resultado.getContent().stream().map(s -> ServicioMapper.aResumen(s, ahora)).toList(),
        resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
  }

  /** RF074. Un servicio inactivo no existe para el público. */
  @Transactional(readOnly = true)
  public ServicioDetalleResponse detalle(Long id) {
    return servicios.findByIdAndActivoTrueAndPrestadorSuspendidoFalse(id)
        .map(s -> ServicioMapper.aDetalle(s, clock.instant()))
        .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
  }

  /** Para reservas-service: incluye inactivos, para que reservas decida (p. ej. no reservar). */
  @Transactional(readOnly = true)
  public ServicioInternoResponse consultaInterna(Long id) {
    return servicios.findById(id)
        .map(ServicioMapper::aInterno)
        .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
  }

  private static Sort ordenDe(String orden) {
    return switch (orden == null ? "recientes" : orden) {
      case "recientes" -> Sort.by(Sort.Order.desc("creadoEn"));
      case "precio_asc" -> Sort.by(Sort.Order.asc("precioBase"), Sort.Order.desc("creadoEn"));
      case "precio_desc" -> Sort.by(Sort.Order.desc("precioBase"), Sort.Order.desc("creadoEn"));
      case "calificacion" -> Sort.by(Sort.Order.desc("calificacionPromedio").nullsLast(), Sort.Order.desc("creadoEn"));
      default -> throw new DatosInvalidosException(
          "Orden no válido: " + orden + " (usa recientes, precio_asc, precio_desc o calificacion)");
    };
  }

  private static String textoOpcional(String valor) {
    return (valor == null || valor.isBlank()) ? null : valor.trim();
  }
}
