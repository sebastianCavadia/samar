package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.servicio.excepcion.DatosInvalidosException;

/**
 * RF019: solo JPEG o PNG y hasta 2 MB. El tipo se reconoce por los primeros bytes del archivo,
 * no por el nombre ni por lo que declara el navegador, que se pueden falsificar.
 */
final class Imagenes {

  static final int MAX_BYTES = 2 * 1024 * 1024;

  private Imagenes() {
  }

  /** @return el tipo MIME reconocido ("image/jpeg" o "image/png") */
  static String validar(byte[] contenido) {
    if (contenido == null || contenido.length == 0) {
      throw new DatosInvalidosException("El archivo está vacío");
    }
    if (contenido.length > MAX_BYTES) {
      throw new DatosInvalidosException("La imagen supera el tamaño máximo de 2 MB");
    }
    if (empiezaCon(contenido, 0xFF, 0xD8, 0xFF)) {
      return "image/jpeg";
    }
    if (empiezaCon(contenido, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
      return "image/png";
    }
    throw new DatosInvalidosException("Solo se aceptan imágenes JPEG o PNG");
  }

  private static boolean empiezaCon(byte[] datos, int... firma) {
    if (datos.length < firma.length) {
      return false;
    }
    for (int i = 0; i < firma.length; i++) {
      if ((datos[i] & 0xFF) != firma[i]) {
        return false;
      }
    }
    return true;
  }
}
