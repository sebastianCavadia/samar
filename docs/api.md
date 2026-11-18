# API REST

Todas las rutas pasan por el API Gateway (`/api/...`). Las rutas `/internal/...` solo existen dentro
de la red de Docker: las usan los servicios entre ellos y no se pueden llamar desde afuera.

Las rutas que no son públicas exigen la cabecera `Authorization: Bearer <token>`. El token se obtiene
al iniciar sesión.

## cuentas-service

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `POST /api/cuentas/auth/registro/turista` | Público | Registro de turista (RF001) |
| `POST /api/cuentas/auth/registro/prestador` | Público | Registro de prestador; queda pendiente de validación (RF003) |
| `POST /api/cuentas/auth/login` `{correo, password}` | Público | Devuelve el JWT; bloqueo tras 5 intentos fallidos (RF004-RF006) |
| `GET /api/cuentas/perfil` | Con sesión | Datos del usuario que inició sesión |
| `/api/cuentas/admin/...` | Administrador | Validar prestadores, buscar y gestionar usuarios, crear administradores, bitácora (RF091-RF100) |

Ejemplo con curl, contra el compose de desarrollo:

```bash
curl -X POST localhost:8080/api/cuentas/auth/registro/turista -H "Content-Type: application/json" \
  -d '{"nombre":"Laura","correo":"laura@demo.co","password":"ClaveSegura1","tipoDocumento":"CC","numeroDocumento":"123"}'
curl -X POST localhost:8080/api/cuentas/auth/login -H "Content-Type: application/json" \
  -d '{"correo":"laura@demo.co","password":"ClaveSegura1"}'
curl localhost:8080/api/cuentas/perfil -H "Authorization: Bearer <token>"
```

## oferta-service

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

## reservas-service

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

## Errores

Todas las respuestas de error usan el formato estándar `ProblemDetail` (RFC 7807), con el mensaje en
`detail`:

| Código | Cuándo |
|---|---|
| 400 | Datos con formato inválido |
| 401 | Falta iniciar sesión o el token venció |
| 403 | El usuario no tiene permiso para esa operación |
| 404 | El recurso no existe |
| 409 | Una regla de negocio lo impide (sin cupos, ya calificada, estado no permitido...) |
| 503 | Otro microservicio no respondió a tiempo |
