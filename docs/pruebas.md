# Pruebas

## Pruebas unitarias y de arquitectura

```bash
mvn verify
```

Corre las pruebas de cada servicio (JUnit y Mockito) y `ArquitecturaCapasTest` (ArchUnit), que hace
fallar el build si una capa usa otra que no debe.

## Pruebas de punta a punta

Se corren contra el sistema levantado con Docker (`docker compose up -d`), desde la raíz del
repositorio. Cada corrida crea sus propios datos de prueba y al final los desactiva, así que se pueden
repetir.

### Reservas

```bash
bash pruebas/e2e-reservas.sh todo
```

Recorre disponibilidad, reserva, pago, confirmación, rechazo, cancelación, permisos y caída de
servicios. Incluye **40 reservas simultáneas sobre 5 cupos**: deben resultar exactamente 5 reservas y
35 rechazos, sin sobreventa. Al final comprueba en la base que, en todas las fechas, los cupos
reservados coinciden con las reservas que los ocupan. La fase `tiempo` prueba la expiración y el
completado automáticos; requiere arrancar reservas con `EXPIRACION_SIN_PAGO_MIN=1 TAREA_MS=5000`.

### Administración

```bash
bash pruebas/e2e-admin.sh
```

Prueba el panel del administrador: validar y rechazar prestadores, buscar, bloquear y reactivar
cuentas, editar, crear administradores y la bitácora. Al final desactiva a un prestador y comprueba
que sus servicios salen del catálogo y vuelven al reactivarlo (ADR-13). Para que no tarde hasta un
minuto en verse: `SINCRONIZACION_MS=5000 docker compose up -d oferta-service`.

### Calificaciones

```bash
bash pruebas/e2e-calificaciones.sh
```

Prueba las calificaciones: solo con la reserva COMPLETADA y una vez, permisos, 8 calificaciones
simultáneas del mismo servicio (el promedio debe dar exacto), 5 envíos simultáneos de la misma
reserva (debe contar una sola), filtro por estrellas, respuesta del prestador, oferta caído y la
reparación de la copia local (ADR-11). Para que las reservas se completen rápido:
`TAREA_MS=5000 docker compose up -d reservas-service`.

### Perfil, imágenes, dietas, notificaciones y Swagger

```bash
bash pruebas/e2e-perfil-imagenes.sh
```

Prueba editar el perfil, la foto de perfil y el cambio de contraseña (RF008, RF009); subir imágenes a la
galería, rechazar formatos que no son JPEG o PNG aunque digan serlo y archivos de más de 2 MB, y que al
quitar una imagen de la galería se elimine su archivo (RF019, ADR-15); el filtro por dieta (RF079); las
alertas internas al prestador y al turista (RF037, RF039); y que la documentación Swagger se publique por
el gateway sin exponer `/internal` (RNF-034). Deja los datos demo como estaban.

## Prueba de carga

```bash
docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6:0.54.0 run - < pruebas/carga-k6.js
docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 -e PAUSA=3 grafana/k6:0.54.0 run - < pruebas/carga-k6.js
```

Con [k6](https://k6.io) en un contenedor, sin instalar nada. Durante minuto y medio simula 200 usuarios
navegando al mismo tiempo (buscar, filtrar, ver un servicio, sus cupos y reseñas) y, a la vez, 20 usuarios
creando reservas. `PAUSA` es el tiempo entre clics de cada usuario: 1 s por defecto, un usuario muy
agresivo; 3 s se parece más a alguien que lee lo que ve. Los umbrales son los de los RNF, y si alguno no se
cumple k6 termina con error:

| RNF | Umbral |
|---|---|
| RNF-001 | Búsqueda y filtrado: p95 por debajo de 3 s |
| RNF-002 | Crear una reserva: p95 por debajo de 5 s |
| RNF-004 | 200 usuarios simultáneos sin errores (menos del 1 %) |

### Resultados (19/11/2026)

Medido en local: un portátil de 6 núcleos con Docker Desktop, donde los 6 contenedores y el propio k6
comparten la CPU y 3,5 GB de RAM. Sin carga, cada petición tarda 30-50 ms.

| Corrida | Navegar p95 | Errores al navegar | Reservar p95 | Reservas creadas | Peticiones/s |
|---|---|---|---|---|---|
| PAUSA=1, servicios recién reiniciados (JVM fría) | 5,4 s ✗ | 0 % ✓ | 5,7 s ✗ | 77 % ✗ | 64 |
| PAUSA=1, segunda corrida | 3,7 s ✗ | 0 % ✓ | 8,3 s ✗ | 97 % ✗ | 87 |
| PAUSA=1, tercera corrida | 2,1 s ✓ | 0 % ✓ | 4,9 s ✓ | 98,6 % ✗ | 113 |
| PAUSA=3 | 0,9 s ✓ | 0 % ✓ | 1,8 s ✓ | 100 % ✓ | 60 |

**Lectura:**
- **El límite fue la CPU del portátil, no la memoria ni la base.** En el pico, los contenedores usaban
  unos 4,5 de los 6 núcleos (oferta 130 %, reservas 104 %, Postgres 77 %), con la memoria sobrada.
- **Navegar nunca falló**, ni en la peor corrida: los tiempos subieron, pero ninguna petición dio error.
- **Las reservas fallidas fueron todas 503.** Al reservar, reservas consulta a oferta, que era el servicio
  más cargado; cuando oferta tardó más de 3 s, reservas cortó y respondió 503 en vez de quedarse
  colgado (ADR-12). Ninguna reserva quedó a medias ni sobrevendió cupos.
- **La JVM necesita calentarse.** La primera corrida tras reiniciar es la peor, porque Java todavía está
  compilando el código más usado.
- **Para producción:** oferta es el primer servicio que conviene escalar. Como no guarda estado de sesión
  (el JWT viaja en cada petición), se pueden levantar más copias detrás del gateway (RNF-006).

## Datos de demostración

```bash
bash pruebas/seed-demo.sh
```

Crea las cuentas de demo (clave `ClaveSegura1`): turista `ana@demo.co`, turista `laura@demo.co` (sin
fecha de nacimiento, para las pruebas de edad mínima), prestador validado `tours@demo.co` y prestador
pendiente `hostal@demo.co`, con servicios, fechas, reservas en cada estado y reseñas. Se puede correr
varias veces sin duplicar datos.

El administrador lo crea cuentas-service al primer arranque con `ADMIN_CORREO` / `ADMIN_PASSWORD`
(por defecto `admin@samar.co` / `Admin12345` en desarrollo).
