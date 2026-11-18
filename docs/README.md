# Documentación de SAMAR

Organizada por las fases de RUP (ADR-01). Los documentos del primer y del segundo entregable se
conservan tal como se entregaron; lo que se decidió después, al construir el sistema, está en las ADR
05 a 14.

## Entregables

| Documento | Contenido |
|---|---|
| [Primer entregable (PDF)](entregables/Documento_primer_entregable.pdf) | Entendimiento del problema y requisitos |
| [Segundo entregable (PDF)](entregables/Documento_segundo_entregable.pdf) | Casos de uso, arquitectura, modelo de datos e interfaces |

## 00 · Gestión

- [Cronograma y presupuesto](00_gestion/cronograma-presupuesto.md)

## 01 · Entendimiento

- [Acta de entendimiento](01_entendimiento/acta-entendimiento-v0.1.md)
- [Problema](01_entendimiento/problema.md)
- [Visión y alcance](01_entendimiento/vision-alcance.md)
- [Stakeholders](01_entendimiento/stakeholders.md)
- [Supuestos](01_entendimiento/supuestos.md)
- [Fuera de alcance](01_entendimiento/fuera-alcance.md)

## 02 · Requisitos

- [Requisitos funcionales v3](02_requisitos/requisitos_funcionales_SAMAR_v3.md): la versión vigente,
  RF001 a RF100. Es la que se cita en el código y en las ADR.
- [Auditoría de requisitos](02_requisitos/auditoria_SAMAR.md): cambios de la versión 2 a la 3.
- [Especificación de requerimientos (SRS)](02_requisitos/especificacion_de_requerimientos.md): SRS
  del primer entregable, con requisitos no funcionales.
- Casos de uso:
  - [Primer entregable](02_requisitos/README.md): nivel 0 y nivel 1, en PNG y PlantUML (`puml/`).
  - [Segundo entregable](02_requisitos/casos-de-uso-entregable-2/): `casoUso0` (nivel 0) y
    `casoUso1_1-7` a `casoUso1_7-7` (nivel 1).
- [Histórico: requisitos funcionales v2](02_requisitos/historico/requisitos_funcionales_SAMAR.md)

## 03 · Arquitectura

- [ADR-01 a 04](03_arquitectura/decisiones-arquitectonicas.md): RUP, PostgreSQL, REST con webhooks
  firmados y pago con Wompi (sandbox).
- [ADR-05 a 14](adr/README.md): decisiones tomadas al construir (monorepo, una base por servicio,
  disponibilidad en reservas, dirección de dependencias, JWT en el gateway, calificaciones...).
- Diagramas del segundo entregable:
  - [Paquetes](03_arquitectura/diagramas/Diagrama%20de%20paquetes.drawio.png)
  - [Despliegue](03_arquitectura/diagramas/Diagrama%20de%20despligue.drawio.png)

> **Diferencias con lo implementado.** Los diagramas son los del segundo entregable. Al construir se
> corrigieron dos puntos: las dependencias entre servicios van en una sola dirección, sin ciclos
> (ADR-08), y cada servicio tiene su propia base de datos (ADR-06), así que el modelo entidad-relación
> quedó dividido en cuatro bases. El despliegue real usa Docker con Caddy como servidor HTTPS
> ([despliegue.md](despliegue.md)).

## 04 · Diseño

- Modelo de datos:
  - [Diagrama de clases](04_diseno/modelo-datos/Diagrama%20de%20Clases.drawio.png)
  - [Diagrama entidad-relación](04_diseno/modelo-datos/DiagramaEntidadRelaci%C3%B3n.drawio.png)
  - El esquema real de cada base está en las migraciones Flyway de cada servicio
    (`*-service/src/main/resources/db/migration/`).
- Interfaces:
  - [Mockups del primer entregable](04_diseno/05_interfaces-mockups.md)
  - [Wireframes y mockups del segundo entregable](04_diseno/interfaces-entregable-2/)

## Construcción y operación

- [API REST](api.md): rutas de cada servicio, quién puede usarlas y códigos de error.
- [Guía de desarrollo](desarrollo.md): capas, cómo correr un servicio y cómo agregar una funcionalidad.
- [Pruebas](pruebas.md): unitarias, de arquitectura, de punta a punta y datos de demostración.
- [Despliegue](despliegue.md): servidor, dominio, HTTPS y cómo publicar cada cambio.

## Prototipo del primer entregable

El prototipo monolítico del primer entregable está en su propio repositorio:
[turismo-santa-marta](https://github.com/sebastianCavadia/turismo-santa-marta).
