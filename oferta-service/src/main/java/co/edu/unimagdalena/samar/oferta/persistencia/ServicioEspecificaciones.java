package co.edu.unimagdalena.samar.oferta.persistencia;

import co.edu.unimagdalena.samar.oferta.modelo.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Consulta dinámica del catálogo (RF073/RF077): cada filtro es opcional y solo se agrega
 * si viene. Siempre excluye los servicios inactivos (RF032) y los de prestadores desactivados (ADR-13).
 */
public final class ServicioEspecificaciones {

  private ServicioEspecificaciones() {
  }

  public static Specification<ServicioTuristico> publicadosCon(String texto, TipoServicio tipo,
      Categoria categoria, String zona, BigDecimal precioMin, BigDecimal precioMax,
      BigDecimal calificacionMin, Boolean accesible, TipoDieta dieta) {
    return (root, query, cb) -> {
      List<Predicate> condiciones = new ArrayList<>();
      condiciones.add(cb.isTrue(root.get("activo")));
      condiciones.add(cb.isFalse(root.get("prestadorSuspendido"))); // ADR-13

      if (texto != null) {
        // Insensible a mayúsculas (RF073). Busca en nombre, descripción y zona.
        String patron = "%" + escaparLike(texto.toLowerCase(Locale.ROOT)) + "%";
        condiciones.add(cb.or(
            cb.like(cb.lower(root.get("nombre")), patron, '\\'),
            cb.like(cb.lower(root.get("descripcion")), patron, '\\'),
            cb.like(cb.lower(root.get("zona")), patron, '\\')));
      }
      if (tipo != null) {
        condiciones.add(cb.equal(root.type(), claseDe(tipo)));
      }
      if (categoria != null) {
        condiciones.add(cb.isMember(categoria, root.<Set<Categoria>>get("categorias")));
      }
      if (zona != null) {
        condiciones.add(cb.equal(cb.lower(root.get("zona")), zona.toLowerCase(Locale.ROOT)));
      }
      if (precioMin != null) {
        condiciones.add(cb.greaterThanOrEqualTo(root.get("precioBase"), precioMin));
      }
      if (precioMax != null) {
        condiciones.add(cb.lessThanOrEqualTo(root.get("precioBase"), precioMax));
      }
      if (calificacionMin != null) {
        condiciones.add(cb.greaterThanOrEqualTo(root.get("calificacionPromedio"), calificacionMin));
      }
      if (Boolean.TRUE.equals(accesible)) {
        condiciones.add(cb.isTrue(root.get("accesible")));
      }
      if (dieta != null) {
        // RF079: solo restaurantes que declaran esa dieta. Subconsulta porque las dietas
        // son propias de ServicioGastronomico y la consulta parte de ServicioTuristico.
        Subquery<Long> conDieta = query.subquery(Long.class);
        Root<ServicioGastronomico> g = conDieta.from(ServicioGastronomico.class);
        Join<ServicioGastronomico, TipoDieta> d = g.join("dietas");
        conDieta.select(g.get("id")).where(cb.equal(g.get("id"), root.get("id")), cb.equal(d, dieta));
        condiciones.add(cb.exists(conDieta));
      }
      return cb.and(condiciones.toArray(Predicate[]::new));
    };
  }

  private static Class<? extends ServicioTuristico> claseDe(TipoServicio tipo) {
    return switch (tipo) {
      case HOSPEDAJE -> Hospedaje.class;
      case ACTIVIDAD -> ActividadTuristica.class;
      case GASTRONOMICO -> ServicioGastronomico.class;
    };
  }

  /** Evita que un "%" o "_" escrito por el usuario actúe como comodín. */
  private static String escaparLike(String valor) {
    return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
