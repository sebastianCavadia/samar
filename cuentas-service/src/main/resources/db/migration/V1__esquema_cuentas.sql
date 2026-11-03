-- Base samar_cuentas: solo la usa cuentas-service.
-- Sigue el diagrama ER (usuario + subtipos) con los campos que faltaban según la auditoría:
-- intentos_fallidos y bloqueado_hasta (RF005).

CREATE TABLE usuario (
  id                BIGSERIAL PRIMARY KEY,
  nombre            VARCHAR(120) NOT NULL,
  correo            VARCHAR(150) NOT NULL UNIQUE,
  password_hash     VARCHAR(100) NOT NULL,
  telefono          VARCHAR(40),
  idioma            VARCHAR(10)  NOT NULL DEFAULT 'es',
  rol               VARCHAR(20)  NOT NULL CHECK (rol IN ('TURISTA', 'PRESTADOR', 'ADMINISTRADOR')),
  estado            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),
  intentos_fallidos INT          NOT NULL DEFAULT 0,
  bloqueado_hasta   TIMESTAMPTZ,
  ultimo_acceso     TIMESTAMPTZ,
  creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE turista (
  id_usuario       BIGINT PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
  tipo_documento   VARCHAR(20) NOT NULL CHECK (tipo_documento IN ('CC', 'CE', 'PASAPORTE')),
  numero_documento VARCHAR(40) NOT NULL UNIQUE,
  nacionalidad     VARCHAR(80),
  pais_residencia  VARCHAR(80),
  fecha_nacimiento DATE,
  discapacidad     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE prestador_turistico (
  id_usuario     BIGINT PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
  tipo_prestador VARCHAR(30)  NOT NULL CHECK (tipo_prestador IN ('HOTEL', 'RESTAURANTE', 'AGENCIA_TURISTICA')),
  nit            VARCHAR(40)  NOT NULL UNIQUE,
  rnt            VARCHAR(40)  NOT NULL UNIQUE,
  direccion      VARCHAR(200) NOT NULL,
  zona           VARCHAR(80),
  latitud        DOUBLE PRECISION,
  longitud       DOUBLE PRECISION,
  validado       BOOLEAN      NOT NULL DEFAULT FALSE,
  validado_en    TIMESTAMPTZ
);

CREATE TABLE agencia_idioma (
  id_usuario BIGINT      NOT NULL REFERENCES prestador_turistico (id_usuario) ON DELETE CASCADE,
  idioma     VARCHAR(40) NOT NULL,
  PRIMARY KEY (id_usuario, idioma)
);

CREATE TABLE administrador (
  id_usuario       BIGINT PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
  area_responsable VARCHAR(120) NOT NULL
);

CREATE INDEX idx_prestador_pendiente ON prestador_turistico (validado) WHERE NOT validado;
