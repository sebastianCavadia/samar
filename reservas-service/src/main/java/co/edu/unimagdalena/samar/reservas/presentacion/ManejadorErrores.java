package co.edu.unimagdalena.samar.reservas.presentacion;

import co.edu.unimagdalena.samar.reservas.servicio.excepcion.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/** Traduce las excepciones de la capa servicio a respuestas HTTP claras (RFC 7807). */
@RestControllerAdvice
class ManejadorErrores {

  @ExceptionHandler(ReglaNegocioException.class)
  ProblemDetail reglaNegocio(ReglaNegocioException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(DatosInvalidosException.class)
  ProblemDetail datosInvalidos(DatosInvalidosException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(RecursoNoEncontradoException.class)
  ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(AccesoDenegadoException.class)
  ProblemDetail accesoDenegado(AccesoDenegadoException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler(ServicioNoDisponibleException.class)
  ProblemDetail noDisponible(ServicioNoDisponibleException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
  }

  /** Dos operaciones a la vez sobre la misma reserva o fecha: gana una y la otra debe reintentar. */
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ProblemDetail concurrencia(ObjectOptimisticLockingFailureException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
        "Alguien modificó esto al mismo tiempo que tú. Recarga e intenta de nuevo");
  }

  /** La base de datos rechazó el cambio (cupos por debajo de lo reservado, fecha o comprobante duplicado...). */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail integridad(DataIntegrityViolationException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
        "La operación entra en conflicto con datos que acaban de cambiar. Recarga e intenta de nuevo");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
        "El cuerpo de la petición no es un JSON válido en UTF-8 o tiene campos con formato incorrecto");
  }

  @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
  ProblemDetail parametro(Exception e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parámetro faltante o con formato inválido");
  }

  @ExceptionHandler(BindException.class)
  ProblemDetail validacion(BindException e) {
    String detalle = e.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + ": " + (f.isBindingFailure() ? "formato inválido" : f.getDefaultMessage()))
        .collect(Collectors.joining("; "));
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
  }
}
