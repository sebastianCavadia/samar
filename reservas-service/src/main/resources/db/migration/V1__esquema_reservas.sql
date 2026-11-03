-- Base samar_reservas: disponibilidad + reservas + pagos.
-- La disponibilidad vive aquí (no en oferta) para que "descontar cupo + crear reserva"
-- sea UNA transacción local: es lo que evita el overbooking (ver ADR-07 en docs/adr).

CREATE TABLE disponibilidad (
  id                BIGSERIAL PRIMARY KEY,
  id_servicio       BIGINT  NOT NULL,                    -- servicio en oferta-service
  fecha             DATE    NOT NULL,
  hora              TIME,
  cupos_totales     INT     NOT NULL CHECK (cupos_totales >= 0),
  cupos_reservados  INT     NOT NULL DEFAULT 0,
  activo            BOOLEAN NOT NULL DEFAULT TRUE,
  version           BIGINT  NOT NULL DEFAULT 0,          -- bloqueo optimista (@Version) contra reservas simultáneas
  CHECK (cupos_reservados BETWEEN 0 AND cupos_totales),  -- la BD también impide sobrevender
  UNIQUE (id_servicio, fecha, hora)
);

CREATE TABLE reserva (
  id            BIGSERIAL PRIMARY KEY,
  id_turista    BIGINT         NOT NULL,                 -- usuario en cuentas-service
  estado        VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE'
                  CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA', 'COMPLETADA')), -- RF048
  total         NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
  moneda        VARCHAR(3)     NOT NULL DEFAULT 'COP',
  motivo_cancelacion VARCHAR(200),                       -- RF040
  creado_en     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Copia del nombre y precio al reservar (RF047): la reserva no depende de oferta para mostrarse.
CREATE TABLE detalle_reserva (
  id                 BIGSERIAL PRIMARY KEY,
  id_reserva         BIGINT         NOT NULL REFERENCES reserva (id) ON DELETE CASCADE,
  id_disponibilidad  BIGINT         NOT NULL REFERENCES disponibilidad (id),
  id_servicio        BIGINT         NOT NULL,
  id_prestador       BIGINT         NOT NULL,
  nombre_servicio    VARCHAR(150)   NOT NULL,
  cantidad           INT            NOT NULL CHECK (cantidad > 0),
  precio_unitario    NUMERIC(12, 2) NOT NULL,
  subtotal           NUMERIC(12, 2) NOT NULL
);

-- RF043/RF044/RF055: faltaba en el ER. Nunca se guarda PAN/CVV.
CREATE TABLE pago (
  id          BIGSERIAL PRIMARY KEY,
  id_reserva  BIGINT         NOT NULL REFERENCES reserva (id),
  referencia  VARCHAR(80)    NOT NULL UNIQUE,            -- referencia de la pasarela o del comprobante
  medio       VARCHAR(20)    NOT NULL CHECK (medio IN ('PASARELA', 'COMPROBANTE')),
  estado      VARCHAR(20)    NOT NULL CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO')),
  monto       NUMERIC(12, 2) NOT NULL,
  creado_en   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- RF057: bitácora de cambios de estado.
CREATE TABLE bitacora_reserva (
  id              BIGSERIAL PRIMARY KEY,
  id_reserva      BIGINT      NOT NULL REFERENCES reserva (id) ON DELETE CASCADE,
  estado_anterior VARCHAR(20),
  estado_nuevo    VARCHAR(20) NOT NULL,
  id_usuario      BIGINT,
  fecha           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reserva_turista ON reserva (id_turista, creado_en DESC);
CREATE INDEX idx_reserva_estado ON reserva (estado, creado_en);
CREATE INDEX idx_detalle_prestador ON detalle_reserva (id_prestador);
CREATE INDEX idx_disponibilidad_servicio ON disponibilidad (id_servicio, fecha);
