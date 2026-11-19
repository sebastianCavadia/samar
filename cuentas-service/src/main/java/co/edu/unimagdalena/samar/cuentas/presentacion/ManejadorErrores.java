package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.AccesoDenegadoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.CredencialesInvalidasException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.DatosInvalidosException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.RecursoNoEncontradoException;
import co.edu.unimagdalena.samar.cuentas.servicio.excepcion.ReglaNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

  @ExceptionHandler(CredencialesInvalidasException.class)
  ProblemDetail credenciales(CredencialesInvalidasException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
  }

  @ExceptionHandler(AccesoDenegadoException.class)
  ProblemDetail accesoDenegado(AccesoDenegadoException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }

  /** JSON mal formado, tipos incorrectos (p. ej. una fecha inválida) o codificación que no es UTF-8. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
        "El cuerpo de la petición no es un JSON válido en UTF-8 o tiene campos con formato incorrecto");
  }

  /** El archivo supera el límite de spring.servlet.multipart (RF008: foto de hasta 2 MB). */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ProblemDetail archivoGrande(MaxUploadSizeExceededException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamaño máximo de 2 MB");
  }

  @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
  ProblemDetail sinArchivo(Exception e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Adjunta una imagen en el campo 'archivo'");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validacion(MethodArgumentNotValidException e) {
    String detalle = e.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + ": " + f.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
  }
}
