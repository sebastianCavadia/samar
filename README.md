# SAMAR: Plataforma de turismo sostenible en Santa Marta

Proyecto de Arquitectura de Software, Ingeniería de Sistemas, Universidad del Magdalena (2026-2).

Equipo: Julio Alexander Amador Calderón · Valentina Bonilla Nigrinis · Iván Camilo Alarcón Moreno ·
Sergio Andrés Mosquera Molina · Sebastián Mateo Cavadía Maldonado.

Turistas que buscan, reservan, pagan y califican hospedajes, tours y gastronomía local; prestadores que
publican su oferta y gestionan sus reservas; y administradores que validan prestadores y gestionan
usuarios.

**Demo en línea:** https://samar-unimag.eastus.cloudapp.azure.com (puede estar apagada fuera de las
presentaciones).

## Arquitectura

Microservicios (decisión del segundo entregable), cada uno organizado internamente por capas.

```
             Navegador (SPA React)
                     │  /api/**
              ┌──────▼──────┐
              │ api-gateway │  valida el JWT, enruta, pone X-Usuario-Id / X-Usuario-Rol
              └──────┬──────┘
   ┌─────────────┬───┴──────────┬────────────────────┐
   ▼             ▼              ▼                    ▼
cuentas ◄──── oferta ◄────── reservas ◄────── recomendaciones
 (8081)       (8082)          (8083)              (8084)
   │             │              │                    │
samar_cuentas samar_oferta  samar_reservas   samar_recomendaciones   ← 1 Postgres, 1 base y 1 usuario por servicio
```

Las flechas indican quién llama a quién por REST interno. La dirección es única y **no hay ciclos**
(ADR-08).

| Servicio | Responsabilidad | Dueño de los datos |
|---|---|---|
| `api-gateway` | Punto único de entrada y autenticación | - |
| `cuentas-service` | Registro, inicio de sesión, administración de usuarios; emite el JWT | usuario, turista, prestador, administrador |
| `oferta-service` | Publicar y buscar servicios turísticos, reseñas | servicio turístico, imágenes, categorías, calificaciones |
| `reservas-service` | Disponibilidad, reservas, pagos y sus estados | disponibilidad, reserva, detalle, pago, bitácora |
| `recomendaciones-service` | Recomendaciones con IA y reportes | solicitudes, recomendaciones, reportes |

**Tecnologías:** Java 21, Spring Boot 3.5, Spring Cloud Gateway, Spring Data JPA, PostgreSQL 16,
Flyway, React + Vite, Docker y Caddy.

## Cómo correrlo

Requisitos: JDK 21, Maven 3.9, Node 20 y Docker Desktop.

```bash
docker compose up --build                       # todo el backend, gateway en http://localhost:8080
cd frontend && npm install && npm run dev       # frontend en http://localhost:5173
bash pruebas/seed-demo.sh                       # opcional: datos de demostración
mvn verify                                      # pruebas, incluida la regla de capas
```

Cuentas de demo (clave `ClaveSegura1`): turista `ana@demo.co`, prestador `tours@demo.co`.
Administrador en desarrollo: `admin@samar.co` / `Admin12345`.

## Estado

| Módulo | Estado |
|---|---|
| Cuentas: registro, inicio de sesión con bloqueo, perfil con foto y cambio de contraseña, panel del administrador | Listo |
| Oferta: publicar y editar servicios con galería de imágenes, catálogo con búsqueda, filtros (incluida dieta) y búsquedas recientes | Listo |
| Reservas: cupos por fecha, reservar, pagar (simulado), confirmar, cancelar, estados automáticos, notificaciones en la app | Listo |
| Calificaciones: reseñas de 1 a 5 estrellas y respuesta del prestador | Listo |
| Recomendaciones con IA | Estructura lista, falta la lógica |

Pendientes principales: recomendaciones con IA (RF067-RF072), pasarela de pago real, envío real de
correos (las notificaciones hoy son alertas dentro de la app), política de cancelación (RF042), impuestos y comisión (RF046), reportes (RF086-RF090) e
invalidar tokens al desactivar una cuenta (RF012).

## Documentación

Todo está en [docs/](docs/README.md), organizado por fases de RUP.

| Documento | Para qué |
|---|---|
| [Índice de documentación](docs/README.md) | Entregables, requisitos, casos de uso, diagramas y diseño |
| [Requisitos funcionales v3](docs/02_requisitos/requisitos_funcionales_SAMAR_v3.md) | RF001 a RF100 |
| [Decisiones de arquitectura](docs/adr/README.md) | ADR-05 a 14 (las ADR-01 a 04 están en [03_arquitectura](docs/03_arquitectura/decisiones-arquitectonicas.md)) |
| [API REST](docs/api.md) | Rutas de cada servicio; Swagger en `/docs/{cuentas,oferta,reservas}/swagger-ui/index.html` |
| [Guía de desarrollo](docs/desarrollo.md) | Capas y cómo agregar una funcionalidad |
| [Pruebas](docs/pruebas.md) | Unitarias, de arquitectura, de punta a punta y de carga (k6) |
| [Despliegue](docs/despliegue.md) | Servidor, HTTPS y cómo publicar cambios |

## Estructura del repositorio

```
api-gateway/              punto de entrada y validación del JWT
cuentas-service/          microservicio de cuentas
oferta-service/           microservicio de oferta turística
reservas-service/         microservicio de reservas
recomendaciones-service/  microservicio de recomendaciones (estructura)
frontend/                 SPA en React
infra/                    creación de las bases, preparación del servidor y respaldos
pruebas/                  pruebas de punta a punta y datos de demostración
docs/                     documentación del proyecto
docker-compose.yml        entorno de desarrollo
docker-compose.prod.yml   producción (con Caddy y HTTPS)
desplegar.sh              publica la última versión en el servidor
```
