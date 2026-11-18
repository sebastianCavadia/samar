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
