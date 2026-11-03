package co.edu.unimagdalena.samar.oferta.presentacion;

import co.edu.unimagdalena.samar.oferta.servicio.excepcion.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Traduce las excepciones de la capa servicio a respuestas HTTP claras (RFC 7807). */
@RestControllerAdvice
class ManejadorErrores {

  @ExceptionHandler(ReglaNegocioException.class)
  ProblemDetail reglaNegocio(ReglaNegocioException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  /** Respaldo de las restricciones de la base (p. ej. id_reserva UNIQUE en calificacion). */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail integridad(DataIntegrityViolationException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
        "La operación choca con datos que ya existen. Recarga e intenta de nuevo");
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

  /** JSON mal formado, tipos incorrectos (p. ej. texto en un número) o codificación que no es UTF-8. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
        "El cuerpo de la petición no es un JSON válido en UTF-8 o tiene campos con formato incorrecto");
  }

  /** Cubre @Valid en el cuerpo (MethodArgumentNotValidException) y en query params (BindException). */
  @ExceptionHandler(BindException.class)
  ProblemDetail validacion(BindException e) {
    String detalle = e.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + ": " + (f.isBindingFailure() ? "formato inválido" : f.getDefaultMessage()))
        .collect(Collectors.joining("; "));
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
  }
}
