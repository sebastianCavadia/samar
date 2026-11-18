# Guía de desarrollo

## Capas dentro de cada microservicio

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

La regla se **verifica automáticamente**: cada servicio tiene `ArquitecturaCapasTest` (ArchUnit) y el
build falla si, por ejemplo, un controller usa un repository o devuelve una entidad.

## Correr un servicio desde el IDE

```bash
docker compose up postgres
```

Luego correr la clase `*Application` del servicio. Por defecto se conecta a `localhost:5433`.

## Cómo agregar una funcionalidad (ejemplo: RF016 registrar servicio en oferta)

1. **modelo**: entidad `ServicioTuristico` que mapea la tabla de `V1__esquema_oferta.sql`.
   Si cambia el esquema, crear una migración nueva (`V4__...sql`). **Nunca se edita una migración ya
   aplicada.**
2. **persistencia**: `ServicioTuristicoRepository extends JpaRepository`.
3. **servicio**: el DTO de entrada y el método `registrar(Identidad, req)` con las reglas (solo
   PRESTADOR, prestador validado vía `CuentasClient`...).
4. **presentacion**: el controller con `@PostMapping`, que recibe `Identidad actor` y el DTO.
5. Prueba unitaria del servicio y `mvn verify`.

Mira `cuentas-service` para ver cada paso ya resuelto.
