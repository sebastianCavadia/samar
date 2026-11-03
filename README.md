# SAMAR: Plataforma de turismo sostenible en Santa Marta

Proyecto de Arquitectura de Software, Ingeniería de Sistemas, Universidad del Magdalena (2026-2).

Equipo: Julio Alexander Amador Calderón · Valentina Bonilla Nigrinis · Iván Camilo Alarcón Moreno ·
Sergio Andrés Mosquera Molina · Sebastián Mateo Cavadía Maldonado.

## Arquitectura

Microservicios (decisión del entregable 2), cada uno organizado internamente por capas.

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

Las flechas indican quién llama a quién por REST interno (`/internal/**`). La dirección es única y
**no hay ciclos**: cuentas no depende de nadie y a recomendaciones solo la llama el gateway.
Las razones están en [docs/adr](docs/adr).

| Servicio | Responsabilidad (CU nivel 1) | Dueño de los datos |
|---|---|---|
| `cuentas-service` | Gestionar cuenta, Administrar usuarios, emite el JWT | usuario, turista, prestador, administrador |
| `oferta-service` | Gestionar oferta turística, Visualizar oferta turística | servicio turístico, imágenes, categorías, calificaciones |
| `reservas-service` | Gestionar reserva (disponibilidad, pago, estados) | disponibilidad, reserva, detalle, pago, bitácora |
| `recomendaciones-service` | Gestionar recomendaciones (IA), Gestionar reportes | solicitudes, recomendaciones, algoritmo, reportes, flujo |
| `api-gateway` | Punto único de entrada y autenticación | - |

### Capas dentro de cada microservicio

```
presentacion   controllers REST + manejo de errores        → solo conoce a servicio
     ↓
servicio       reglas de negocio, @Transactional, DTOs     → usa persistencia e integracion
     ↓
persistencia   repositories JPA                            → usa modelo
integracion    clientes REST a otros servicios, correo, IA
     ↓
modelo         entidades JPA y enums (nunca salen por la API)
config         beans transversales (no es una capa de negocio)
```

La regla se **verifica automáticamente**: cada servicio tiene `ArquitecturaCapasTest` (ArchUnit) y el build
falla si, por ejemplo, un controller usa un repository o devuelve una entidad.

## Cómo correrlo

Requisitos: JDK 21 o superior, Maven 3.9, Node 20 o superior y Docker Desktop.

```bash
# Todo con Docker (gateway en http://localhost:8080)
docker compose up --build

# Frontend en modo desarrollo (http://localhost:5173, reenvía /api al gateway)
cd frontend && npm install && npm run dev

# Compilar y probar todos los servicios (incluye las reglas de capas)
mvn verify
```

Para desarrollar un servicio desde el IDE: `docker compose up postgres` y luego correr la clase
`*Application` del servicio. Por defecto se conecta a `localhost:5433`.

Administrador inicial: `admin@samar.co` / `Admin12345` (se cambia con `ADMIN_CORREO` y `ADMIN_PASSWORD`).

### Probar el flujo de cuentas

```bash
curl -X POST localhost:8080/api/cuentas/auth/registro/turista -H "Content-Type: application/json" \
  -d '{"nombre":"Laura","correo":"laura@demo.co","password":"ClaveSegura1","tipoDocumento":"CC","numeroDocumento":"123"}'
curl -X POST localhost:8080/api/cuentas/auth/login -H "Content-Type: application/json" \
  -d '{"correo":"laura@demo.co","password":"ClaveSegura1"}'
curl localhost:8080/api/cuentas/perfil -H "Authorization: Bearer <token>"
```

## Estado

| Pieza | Estado |
|---|---|
| Monorepo, docker-compose, bases por servicio | Listo |
| api-gateway (rutas + JWT) | Listo |
| cuentas-service: registro turista y prestador, login con bloqueo (RF001-006, RF013), validación de prestadores (RF095) | Listo, es **la plantilla** a copiar |
| oferta-service: registrar/editar servicios (RF016, RF017, RF023-026), activar/desactivar (RF032), catálogo con búsqueda, filtros y paginación (RF073, RF074, RF077, RF082), solo prestadores validados publican (RF085) | Listo |
| reservas, recomendaciones: capas, esquema Flyway, regla de capas | Esqueleto, falta la lógica |
| frontend: login, registro, catálogo con filtros y detalle, panel "Mis servicios" del prestador | Listo para estos casos de uso |

Pendiente en oferta: subida de archivos de imagen (RF019; hoy se registran URLs con texto alternativo),
tarifas por temporada (RF027), parqueo (RF028), vista previa (RF029), SEO (RF031), certificaciones (RF033)
y calificaciones (RF075/RF076, que dependen de reservas COMPLETADAS, ver ADR-11).

### API de oferta-service

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/oferta/servicios?q&tipo&categoria&zona&precioMin&precioMax&calificacionMin&accesible&orden&pagina` | Público | Buscar/listar, 10 por página. `orden`: `recientes`, `precio_asc`, `precio_desc`, `calificacion` |
| `GET /api/oferta/servicios/{id}` | Público | Detalle (solo activos) |
| `POST /api/oferta/prestador/servicios` | Prestador validado | Registrar (con bloque `hospedaje`, `actividad` o `gastronomico` según `tipo`) |
| `PUT /api/oferta/prestador/servicios/{id}` | Dueño | Editar (el tipo no cambia) |
| `PATCH /api/oferta/prestador/servicios/{id}/estado` | Dueño | `{"activo": false}` lo saca del catálogo sin borrarlo |
| `GET /api/oferta/prestador/servicios[/{id}]` | Prestador | Sus servicios, incluidos los inactivos |
| `GET /internal/servicios/{id}` | Solo red interna | Precio, dueño, modalidad, capacidad y edad mínima para reservas-service |

## Cómo agregar una funcionalidad (ejemplo: RF016 registrar servicio en oferta)

1. **modelo**: entidad `ServicioTuristico` que mapea la tabla de `V1__esquema_oferta.sql`.
   Si cambia el esquema, crear `V2__...sql`. **Nunca se edita una migración ya aplicada.**
2. **persistencia**: `ServicioTuristicoRepository extends JpaRepository`.
3. **servicio**: `dto/RegistrarServicioRequest`, `ServicioTuristicoService.registrar(Identidad, req)`
   con las reglas (solo PRESTADOR, prestador validado vía `CuentasClient`...).
4. **presentacion**: `ServicioController` con `@PostMapping`, recibe `Identidad actor` y el DTO.
5. Prueba unitaria del servicio y `mvn verify`.

Mira `cuentas-service` para ver cada paso ya resuelto.
