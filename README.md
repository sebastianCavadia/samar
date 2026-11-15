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
| cuentas-service: registro turista y prestador, login con bloqueo (RF001-006, RF013) | Listo, es **la plantilla** a copiar |
| Panel del administrador: validar o rechazar prestadores con motivo (RF095, RF096), buscar usuarios (RF091), ficha (RF092), activar/desactivar/bloquear con motivo y correo (RF093, RF100), editar datos (RF094), crear administradores con área (RF099), bitácora (RF097). Las ofertas de un prestador desactivado salen del catálogo (ADR-13) | Listo, probado en Docker (`pruebas/e2e-admin.sh`) |
| oferta-service: registrar/editar servicios (RF016, RF017, RF023-026), activar/desactivar (RF032), catálogo con búsqueda, filtros y paginación (RF073, RF074, RF077, RF082), solo prestadores validados publican (RF085) | Listo |
| reservas-service: cupos por fecha (RF018, RF034, RF035), reservar con precio fijado y validaciones (RF036, RF047, RF064-RF066), pago simulado o con comprobante (RF043, RF044, RF055), confirmar/rechazar/cancelar con reembolso (RF038, RF040, RF045), expiración y completado automáticos (RF049, RF056), bitácora (RF057) | Listo, probado en Docker (`pruebas/e2e-reservas.sh`) |
| Calificaciones: de 1 a 5 estrellas con reseña solo con reserva COMPLETADA y una vez por reserva (RF075, RF076), promedio recalculado (RF080), reseñas públicas filtrables por estrellas (RF081), respuesta pública del prestador (RF030). Entran por reservas y se guardan en oferta (ADR-11) | Listo, probado en Docker (`pruebas/e2e-calificaciones.sh`) |
| recomendaciones: capas, esquema Flyway, regla de capas | Esqueleto, falta la lógica |
| frontend: login, registro de turista y de negocio, catálogo y detalle con reseñas, reservar, "Mis reservas" y calificar, panel del prestador (servicios, fechas y cupos, reservas recibidas, reseñas), panel del administrador | Listo para estos casos de uso |

Pendiente en administración: moderar fotos o contenido de un servicio (RF094, segunda parte), preguntas
frecuentes (RF098), PQRS (RF015, pendiente de decidir si entra) e invalidar tokens al desactivar (RF012).

Pendiente en oferta: subida de archivos de imagen (RF019; hoy se registran URLs con texto alternativo),
tarifas por temporada (RF027), parqueo (RF028), vista previa (RF029), SEO (RF031) y certificaciones (RF033).

### API de oferta-service

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/oferta/servicios?q&tipo&categoria&zona&precioMin&precioMax&calificacionMin&accesible&orden&pagina` | Público | Buscar/listar, 10 por página. `orden`: `recientes`, `precio_asc`, `precio_desc`, `calificacion` |
| `GET /api/oferta/servicios/{id}` | Público | Detalle (solo activos) |
| `POST /api/oferta/prestador/servicios` | Prestador validado | Registrar (con bloque `hospedaje`, `actividad` o `gastronomico` según `tipo`) |
| `PUT /api/oferta/prestador/servicios/{id}` | Dueño | Editar (el tipo no cambia) |
| `PATCH /api/oferta/prestador/servicios/{id}/estado` | Dueño | `{"activo": false}` lo saca del catálogo sin borrarlo |
| `GET /api/oferta/prestador/servicios[/{id}]` | Prestador | Sus servicios, incluidos los inactivos |
| `GET /api/oferta/servicios/{id}/calificaciones?estrellas&pagina` | Público | Promedio, cuántas hay de cada puntuación y reseñas de 5 en 5 (RF080, RF081) |
| `GET /api/oferta/prestador/calificaciones?sinResponder&pagina` | Prestador | Reseñas de todos sus servicios y cuántas faltan por responder |
| `PUT /api/oferta/prestador/calificaciones/{id}/respuesta` `{respuesta}` | Dueño del servicio | Respuesta pública a una reseña (RF030) |
| `GET /internal/servicios/{id}` | Solo red interna | Precio, dueño, modalidad, capacidad y edad mínima para reservas-service |
| `POST /internal/calificaciones` | Solo red interna | Registra la calificación que envía reservas; idempotente por reserva (ADR-11) |

### API de reservas-service

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/reservas/disponibilidad?servicio&desde&hasta` | Público | Fechas con cupos libres |
| `POST /api/reservas` `{idDisponibilidad, personas}` | Turista | Crea la reserva PENDIENTE y ocupa cupos (atómico, sin overbooking) |
| `POST /api/reservas/{id}/pagos` `{medio: PASARELA\|COMPROBANTE, referencia}` | Turista dueño | Pago simulado (aprobado al instante) o comprobante para revisión |
| `POST /api/reservas/{id}/cancelar` `{motivo}` | Turista o prestador | Libera cupos y reembolsa si estaba pagada |
| `POST /api/reservas/{id}/calificacion` `{puntuacion, comentario}` | Turista dueño | Califica una reserva COMPLETADA, una sola vez (RF075, RF076) |
| `GET /api/reservas/mis` · `GET /api/reservas/{id}` | Turista / partes | Historial con estado, pago y bitácora |
| `GET /api/reservas/prestador/reservas?estado` | Prestador | Reservas recibidas |
| `POST /api/reservas/prestador/reservas/{id}/confirmar` · `/rechazar` · `/pago {aprobado}` | Prestador dueño | Confirmar (solo con pago aprobado), rechazar, revisar comprobante |
| `GET`/`POST /api/reservas/prestador/servicios/{id}/disponibilidad` | Prestador dueño | Ver o abrir cupos por rango de fechas y días de la semana |
| `PATCH /api/reservas/prestador/disponibilidad/{id}` `{activo}` | Prestador dueño | Bloquear o desbloquear una fecha |

Ciclo de vida (RF048): `PENDIENTE → CONFIRMADA → COMPLETADA`, o `PENDIENTE → RECHAZADA/CANCELADA`, o
`CONFIRMADA → CANCELADA`. Cada minuto una tarea cancela las pendientes sin pago tras 15 min (RF056) y
completa las confirmadas cuya fecha pasó (RF049). Para probarlo rápido:
`EXPIRACION_SIN_PAGO_MIN=1 TAREA_MS=10000 docker compose up -d`.

### Prueba de punta a punta (incluye concurrencia)

Con el sistema levantado (`docker compose up -d`), desde la raíz del repositorio:

```bash
bash pruebas/e2e-reservas.sh todo
```

Recorre disponibilidad, reserva, pago, confirmación, rechazo, cancelación, permisos y caída de
servicios. Incluye **40 reservas simultáneas sobre 5 cupos**: deben resultar exactamente 5 reservas y
35 rechazos, sin sobreventa. Al final comprueba en la base que, en todas las fechas, los cupos
reservados coinciden con las reservas que los ocupan. La fase `tiempo` prueba la expiración y el
completado automáticos; requiere arrancar reservas con `EXPIRACION_SIN_PAGO_MIN=1 TAREA_MS=5000`.
Cada corrida crea sus propios servicios de prueba y al final los desactiva, así que se puede repetir.

```bash
bash pruebas/e2e-admin.sh
```

Prueba el panel del administrador: validar y rechazar prestadores, buscar, bloquear y reactivar
cuentas, editar, crear administradores y la bitácora. Al final desactiva a un prestador y comprueba
que sus servicios salen del catálogo y vuelven al reactivarlo (ADR-13). Para que no tarde hasta un
minuto en verse: `SINCRONIZACION_MS=5000 docker compose up -d oferta-service`.

```bash
bash pruebas/e2e-calificaciones.sh
```

Prueba las calificaciones: solo con la reserva COMPLETADA y una vez, permisos, 8 calificaciones
simultáneas del mismo servicio (el promedio debe dar exacto), 5 envíos simultáneos de la misma
reserva (debe contar una sola), filtro por estrellas, respuesta del prestador, oferta caído y la
reparación de la copia local (ADR-11). Para que las reservas se completen rápido:
`TAREA_MS=5000 docker compose up -d reservas-service`.

Las cuentas de demo las crea `pruebas/seed-demo.sh` (clave `ClaveSegura1`): turista `ana@demo.co`,
turista `laura@demo.co` (sin fecha de nacimiento para las pruebas de edad mínima), prestador validado
`tours@demo.co` y prestador pendiente `hostal@demo.co`. El administrador lo crea cuentas-service al
primer arranque con `ADMIN_CORREO` / `ADMIN_PASSWORD` (por defecto `admin@samar.co` / `Admin12345`).

## Despliegue en producción

La guía paso a paso (VM, dominio, primer despliegue y cómo publicar cada cambio) está en
[docs/despliegue.md](docs/despliegue.md). El compose de producción sirve la SPA con Caddy y deja
Postgres y el gateway en la red interna. En resumen, en la VM:

```bash
bash infra/preparar-vm.sh             # una sola vez: Docker, swap y firewall
cp .env.example .env                  # completar dominio y secretos
bash desplegar.sh                        # git pull, construye y reinicia solo lo que cambió
SEED_BD=1 bash pruebas/seed-demo.sh   # una sola vez: datos demo (lee SAMAR_DOMINIO y ADMIN_* de .env)
```

- **Dominio:** apuntar `SAMAR_DOMINIO` (por ejemplo `samar.duckdns.org`) a la IP de la VM. Caddy
  obtiene el certificado HTTPS automáticamente. Abrir solo los puertos 80 y 443.
- **Firewall:** además de la lista de seguridad del proveedor, abrir 80/443 en el firewall de la VM
  (`sudo ufw allow 80,443/tcp` en Ubuntu; en Oracle también hay reglas de iptables).
- **Secretos:** cambiar `JWT_SECRET`, `ADMIN_PASSWORD` y las claves de Postgres antes del primer
  arranque. Las claves de Postgres solo se aplican cuando el volumen está vacío.
- **Seed:** crear los datos demo una sola vez. En la VM hace falta `SEED_BD=1` para acceder a
  Postgres por `docker exec`; sin eso se omiten la reserva completada y el ajuste de Laura.
  En local, con el compose de desarrollo, usar `BASE_URL=http://localhost:8080 bash pruebas/seed-demo.sh`.
- **Admin:** el administrador inicial se crea solo si no existe ninguno. Si cambiás `ADMIN_PASSWORD`
  después del primer arranque, el admin anterior no se actualiza.
- **Demo:** `EXPIRACION_SIN_PAGO_MIN=1440`, `TAREA_MS=5000` y `SINCRONIZACION_MS=5000` dejan la
  expiración, el completado y la suspensión de ofertas con tiempos cómodos. Para probar la
  expiración rápido, usar `EXPIRACION_SIN_PAGO_MIN=1` y reiniciar `reservas-service`.
- **Datos:** `docker compose -f docker-compose.prod.yml down` conserva el volumen; `down -v` lo
  borra. `bash infra/respaldar.sh` guarda las cuatro bases en `~/respaldos` (programarlo con cron).
- **Logs:** `docker compose -f docker-compose.prod.yml logs -f`.
- **Si Oracle no permite crear la VM:** usar Azure for Students o un VPS pequeño; el compose es el
  mismo. Para un dominio propio, cambiar `SAMAR_DOMINIO` y volver a levantar `frontend`.

## Cómo agregar una funcionalidad (ejemplo: RF016 registrar servicio en oferta)

1. **modelo**: entidad `ServicioTuristico` que mapea la tabla de `V1__esquema_oferta.sql`.
   Si cambia el esquema, crear `V2__...sql`. **Nunca se edita una migración ya aplicada.**
2. **persistencia**: `ServicioTuristicoRepository extends JpaRepository`.
3. **servicio**: `dto/RegistrarServicioRequest`, `ServicioTuristicoService.registrar(Identidad, req)`
   con las reglas (solo PRESTADOR, prestador validado vía `CuentasClient`...).
4. **presentacion**: `ServicioController` con `@PostMapping`, recibe `Identidad actor` y el DTO.
5. Prueba unitaria del servicio y `mvn verify`.

Mira `cuentas-service` para ver cada paso ya resuelto.
