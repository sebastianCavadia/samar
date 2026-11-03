package co.edu.unimagdalena.samar.oferta.presentacion;

import co.edu.unimagdalena.samar.oferta.servicio.dto.Identidad;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * Permite declarar un parámetro {@code Identidad actor} en cualquier controller.
 * Se arma con las cabeceras que pone el gateway después de validar el JWT.
 */
class IdentidadArgumentResolver implements HandlerMethodArgumentResolver {

  static final String CABECERA_USUARIO_ID = "X-Usuario-Id";
  static final String CABECERA_USUARIO_ROL = "X-Usuario-Rol";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Identidad.class.equals(parameter.getParameterType());
  }

  @Override
  public Identidad resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
      NativeWebRequest request, WebDataBinderFactory binderFactory) {
    String id = request.getHeader(CABECERA_USUARIO_ID);
    String rol = request.getHeader(CABECERA_USUARIO_ROL);
    if (id == null || rol == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere iniciar sesión");
    }
    try {
      return new Identidad(Long.valueOf(id), rol);
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identidad inválida");
    }
  }
}
