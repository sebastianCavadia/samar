package co.edu.unimagdalena.samar.cuentas;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Hace cumplir la arquitectura por capas del microservicio: si alguien, por ejemplo,
 * usa un repository desde un controller, el build falla.
 *
 *   presentacion  →  servicio  →  persistencia / integracion  →  modelo
 */
class ArquitecturaCapasTest {

  private static final String PAQUETE = "co.edu.unimagdalena.samar.cuentas";

  @Test
  void cadaCapaSoloConoceALaDeAbajo() {
    JavaClasses clases = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(PAQUETE);

    layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .withOptionalLayers(true)
        .layer("Presentacion").definedBy(PAQUETE + ".presentacion..")
        .layer("Servicio").definedBy(PAQUETE + ".servicio..")
        .layer("Persistencia").definedBy(PAQUETE + ".persistencia..")
        .layer("Integracion").definedBy(PAQUETE + ".integracion..")
        .layer("Modelo").definedBy(PAQUETE + ".modelo..")
        .whereLayer("Presentacion").mayNotBeAccessedByAnyLayer()
        .whereLayer("Servicio").mayOnlyBeAccessedByLayers("Presentacion")
        .whereLayer("Persistencia").mayOnlyBeAccessedByLayers("Servicio")
        .whereLayer("Integracion").mayOnlyBeAccessedByLayers("Servicio")
        .whereLayer("Modelo").mayOnlyBeAccessedByLayers("Servicio", "Persistencia")
        .check(clases);
  }
}
