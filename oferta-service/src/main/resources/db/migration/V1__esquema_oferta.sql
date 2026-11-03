-- Base samar_oferta: catálogo de servicios turísticos y calificaciones.
-- id_prestador, id_turista e id_reserva son IDs de OTROS servicios: se guardan como
-- números, sin FOREIGN KEY, porque esas tablas viven en otras bases (cuentas, reservas).

CREATE TABLE servicio_turistico (
  id                    BIGSERIAL PRIMARY KEY,
  id_prestador          BIGINT       NOT NULL,           -- usuario en cuentas-service
  tipo                  VARCHAR(20)  NOT NULL CHECK (tipo IN ('HOSPEDAJE', 'ACTIVIDAD', 'GASTRONOMICO')),
  nombre                VARCHAR(150) NOT NULL,
  descripcion           TEXT,
  modalidad             VARCHAR(20)  NOT NULL DEFAULT 'RESERVA' CHECK (modalidad IN ('RESERVA', 'WALK_IN')), -- RF016
  precio_base           NUMERIC(12, 2) NOT NULL CHECK (precio_base >= 0),                                 -- RF025
  moneda                VARCHAR(3)   NOT NULL DEFAULT 'COP',
  direccion             VARCHAR(200),
  zona                  VARCHAR(80),
  latitud               DOUBLE PRECISION,
  longitud              DOUBLE PRECISION,
  accesible             BOOLEAN      NOT NULL DEFAULT FALSE,  -- RF026
  activo                BOOLEAN      NOT NULL DEFAULT TRUE,   -- RF032
  calificacion_promedio NUMERIC(2, 1),                         -- RF080
  total_calificaciones  INT          NOT NULL DEFAULT 0,
  creado_en             TIMESTAMPTZ  NOT NULL DEFAULT now()  -- RF082 "Nuevo"
);

CREATE TABLE servicio_categoria (                              -- RF024
  id_servicio BIGINT      NOT NULL REFERENCES servicio_turistico (id) ON DELETE CASCADE,
  categoria   VARCHAR(30) NOT NULL CHECK (categoria IN ('PLAYA', 'NATURALEZA', 'CULTURA', 'AVENTURA', 'GASTRONOMIA', 'DESCANSO')),
  PRIMARY KEY (id_servicio, categoria)
);

CREATE TABLE imagen_servicio (                                 -- RF019 / RF021 (alt obligatorio)
  id          BIGSERIAL PRIMARY KEY,
  id_servicio BIGINT       NOT NULL REFERENCES servicio_turistico (id) ON DELETE CASCADE,
  url         VARCHAR(500) NOT NULL,
  descripcion VARCHAR(200) NOT NULL
);

CREATE TABLE hospedaje (                                       -- RF023 / RF064
  id_servicio         BIGINT PRIMARY KEY REFERENCES servicio_turistico (id) ON DELETE CASCADE,
  tipo_habitacion     VARCHAR(60),
  capacidad_personas  INT NOT NULL CHECK (capacidad_personas > 0),
  hora_check_in       TIME,
  hora_check_out      TIME
);

CREATE TABLE actividad_turistica (                             -- RF023 / RF065
  id_servicio    BIGINT PRIMARY KEY REFERENCES servicio_turistico (id) ON DELETE CASCADE,
  duracion_horas NUMERIC(4, 1),
  hora_inicio    TIME,
  dificultad     VARCHAR(20) CHECK (dificultad IN ('BAJA', 'MEDIA', 'ALTA')),
  edad_minima    INT
);

CREATE TABLE servicio_gastronomico (                           -- RF023
  id_servicio  BIGINT PRIMARY KEY REFERENCES servicio_turistico (id) ON DELETE CASCADE,
  tipo_comida  VARCHAR(60),
  aforo        INT,
  hora_apertura TIME,
  hora_cierre   TIME
);

CREATE TABLE servicio_gastronomico_dieta (                     -- RF079
  id_servicio BIGINT      NOT NULL REFERENCES servicio_gastronomico (id_servicio) ON DELETE CASCADE,
  tipo_dieta  VARCHAR(20) NOT NULL CHECK (tipo_dieta IN ('VEGETARIANA', 'VEGANA', 'CELIACA', 'KOSHER', 'HALAL')),
  PRIMARY KEY (id_servicio, tipo_dieta)
);

-- RF075/RF076: una calificación por reserva (corrige el ER: faltaba id_reserva).
CREATE TABLE calificacion (
  id                  BIGSERIAL PRIMARY KEY,
  id_servicio         BIGINT      NOT NULL REFERENCES servicio_turistico (id),
  id_turista          BIGINT      NOT NULL,                  -- usuario en cuentas-service
  id_reserva          BIGINT      NOT NULL UNIQUE,           -- reserva en reservas-service
  puntuacion          SMALLINT    NOT NULL CHECK (puntuacion BETWEEN 1 AND 5),
  comentario          TEXT,
  respuesta_prestador TEXT,                                  -- RF030
  fecha               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_servicio_busqueda ON servicio_turistico (tipo, zona, precio_base) WHERE activo;
CREATE INDEX idx_servicio_texto ON servicio_turistico
  USING gin (to_tsvector('spanish', nombre || ' ' || COALESCE(descripcion, '')));  -- RF073
CREATE INDEX idx_servicio_prestador ON servicio_turistico (id_prestador);
CREATE INDEX idx_calificacion_servicio ON calificacion (id_servicio, puntuacion);
