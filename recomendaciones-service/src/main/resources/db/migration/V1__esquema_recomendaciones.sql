-- Base samar_recomendaciones: módulo de IA y reportes del administrador.
-- Lee datos de oferta y reservas por su API REST; nunca de sus bases.

CREATE TABLE configuracion_algoritmo (                        -- RF072
  id                     BIGSERIAL PRIMARY KEY,
  version                VARCHAR(20) NOT NULL UNIQUE,
  peso_calificacion      DOUBLE PRECISION NOT NULL,
  peso_presupuesto       DOUBLE PRECISION NOT NULL,
  peso_categorias        DOUBLE PRECISION NOT NULL,
  peso_accesibilidad     DOUBLE PRECISION NOT NULL,
  max_aparicion_prestador INT NOT NULL,                       -- imparcialidad (RF067)
  activo                 BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE solicitud_recomendacion (                        -- RF068
  id              BIGSERIAL PRIMARY KEY,
  id_turista      BIGINT      NOT NULL,
  numero_personas INT         NOT NULL CHECK (numero_personas > 0),
  fecha_inicio    DATE        NOT NULL,
  fecha_fin       DATE        NOT NULL,
  presupuesto     NUMERIC(12, 2),
  solo_accesibles BOOLEAN     NOT NULL DEFAULT FALSE,
  creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (fecha_fin >= fecha_inicio)
);

CREATE TABLE solicitud_categoria (
  id_solicitud BIGINT      NOT NULL REFERENCES solicitud_recomendacion (id) ON DELETE CASCADE,
  categoria    VARCHAR(30) NOT NULL,
  PRIMARY KEY (id_solicitud, categoria)
);

CREATE TABLE solicitud_dieta (
  id_solicitud BIGINT      NOT NULL REFERENCES solicitud_recomendacion (id) ON DELETE CASCADE,
  tipo_dieta   VARCHAR(20) NOT NULL,
  PRIMARY KEY (id_solicitud, tipo_dieta)
);

-- Sin es_editada: el turista no modifica lo que genera el modelo (RF069).
CREATE TABLE recomendacion (                                  -- RF067 / RF070
  id                 BIGSERIAL PRIMARY KEY,
  id_solicitud       BIGINT      NOT NULL REFERENCES solicitud_recomendacion (id) ON DELETE CASCADE,
  id_servicio        BIGINT      NOT NULL,                    -- servicio en oferta-service
  puntaje            DOUBLE PRECISION NOT NULL,
  explicacion        TEXT        NOT NULL,                    -- explicabilidad (RF067)
  version_algoritmo  VARCHAR(20) NOT NULL,
  fecha              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ejecucion_algoritmo (                            -- RF071
  id                   BIGSERIAL PRIMARY KEY,
  id_solicitud         BIGINT      NOT NULL REFERENCES solicitud_recomendacion (id) ON DELETE CASCADE,
  id_configuracion     BIGINT      NOT NULL REFERENCES configuracion_algoritmo (id),
  fecha_hora           TIMESTAMPTZ NOT NULL DEFAULT now(),
  tiempo_ms            DOUBLE PRECISION,
  candidatos_evaluados INT,
  exitoso              BOOLEAN     NOT NULL
);

CREATE TABLE registro_flujo (                                 -- RF089
  id                 BIGSERIAL PRIMARY KEY,
  zona               VARCHAR(80) NOT NULL,
  fecha              DATE        NOT NULL,
  numero_visitantes  INT         NOT NULL DEFAULT 0,
  numero_reservas    INT         NOT NULL DEFAULT 0,
  UNIQUE (zona, fecha)
);

CREATE TABLE reporte (                                        -- RF090
  id                BIGSERIAL PRIMARY KEY,
  id_administrador  BIGINT      NOT NULL,
  tipo              VARCHAR(20) NOT NULL CHECK (tipo IN ('ESTADISTICAS', 'FLUJO', 'ALGORITMO')),
  periodo_inicio    DATE        NOT NULL,
  periodo_fin       DATE        NOT NULL,
  generado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE medicion (
  id          BIGSERIAL PRIMARY KEY,
  id_reporte  BIGINT       NOT NULL REFERENCES reporte (id) ON DELETE CASCADE,
  nombre      VARCHAR(100) NOT NULL,
  valor       DOUBLE PRECISION NOT NULL,
  unidad      VARCHAR(30)
);

INSERT INTO configuracion_algoritmo
  (version, peso_calificacion, peso_presupuesto, peso_categorias, peso_accesibilidad, max_aparicion_prestador, activo)
VALUES ('v1', 0.35, 0.25, 0.30, 0.10, 2, TRUE);
