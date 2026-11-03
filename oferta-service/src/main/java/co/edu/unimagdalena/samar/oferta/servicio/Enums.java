package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.servicio.excepcion.DatosInvalidosException;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Convierte los textos de la API a enums del modelo con un error 400 claro si no existen. */
final class Enums {

  private Enums() {
  }

  static <E extends Enum<E>> E opcional(Class<E> tipo, String valor, String campo) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new DatosInvalidosException("Valor no válido para " + campo + ": " + valor);
    }
  }

  static <E extends Enum<E>> Set<E> conjunto(Class<E> tipo, Collection<String> valores, String campo) {
    if (valores == null) {
      return Set.of();
    }
    return valores.stream().map(v -> opcional(tipo, v, campo)).collect(Collectors.toSet());
  }

  static Set<String> nombres(Collection<? extends Enum<?>> valores) {
    return valores.stream().map(Enum::name).collect(Collectors.toCollection(java.util.TreeSet::new));
  }
}
