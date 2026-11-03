package co.edu.unimagdalena.samar.cuentas.persistencia;

import co.edu.unimagdalena.samar.cuentas.modelo.EstadoCuenta;
import co.edu.unimagdalena.samar.cuentas.modelo.Rol;
import co.edu.unimagdalena.samar.cuentas.modelo.Usuario;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** RF091: búsqueda de usuarios por nombre o correo, rol y estado (todos opcionales). */
public final class UsuarioEspecificaciones {

  private UsuarioEspecificaciones() {
  }

  public static Specification<Usuario> con(String texto, Rol rol, EstadoCuenta estado) {
    return (root, query, cb) -> {
      List<Predicate> condiciones = new ArrayList<>();
      if (texto != null) {
        String patron = "%" + texto.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        condiciones.add(cb.or(
            cb.like(cb.lower(root.get("nombre")), patron, '\\'),
            cb.like(cb.lower(root.get("correo")), patron, '\\')));
      }
      if (rol != null) {
        condiciones.add(cb.equal(root.get("rol"), rol));
      }
      if (estado != null) {
        condiciones.add(cb.equal(root.get("estado"), estado));
      }
      return cb.and(condiciones.toArray(Predicate[]::new));
    };
  }
}
