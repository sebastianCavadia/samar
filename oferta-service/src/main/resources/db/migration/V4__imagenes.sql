-- V4: imágenes subidas por el prestador (RF019). Antes solo se registraban URLs externas.
-- Se guardan en la base para que el respaldo diario (pg_dump) también las incluya.
-- La galería de cada servicio (imagen_servicio) las referencia por su URL: /api/oferta/imagenes/{id}.
CREATE TABLE imagen_archivo (
  id            BIGSERIAL   PRIMARY KEY,
  id_prestador  BIGINT      NOT NULL,                    -- usuario en cuentas-service
  contenido     BYTEA       NOT NULL,
  tipo          VARCHAR(20) NOT NULL CHECK (tipo IN ('image/jpeg', 'image/png')),
  tamano        INT         NOT NULL CHECK (tamano > 0),
  creado_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_imagen_archivo_prestador ON imagen_archivo (id_prestador);
CREATE INDEX idx_imagen_servicio_url ON imagen_servicio (url);
