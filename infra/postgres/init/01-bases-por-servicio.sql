-- Una base y un usuario por microservicio, en un solo contenedor Postgres (ADR-02 / ADR-06).
-- Cada servicio solo tiene credenciales de SU base: no puede leer las tablas de otro.
-- Las tablas las crea cada servicio con Flyway al arrancar (src/main/resources/db/migration).

CREATE USER cuentas WITH PASSWORD 'cuentas';
CREATE DATABASE samar_cuentas OWNER cuentas;

CREATE USER oferta WITH PASSWORD 'oferta';
CREATE DATABASE samar_oferta OWNER oferta;

CREATE USER reservas WITH PASSWORD 'reservas';
CREATE DATABASE samar_reservas OWNER reservas;

CREATE USER recomendaciones WITH PASSWORD 'recomendaciones';
CREATE DATABASE samar_recomendaciones OWNER recomendaciones;

REVOKE CONNECT ON DATABASE samar_cuentas FROM PUBLIC;
REVOKE CONNECT ON DATABASE samar_oferta FROM PUBLIC;
REVOKE CONNECT ON DATABASE samar_reservas FROM PUBLIC;
REVOKE CONNECT ON DATABASE samar_recomendaciones FROM PUBLIC;
