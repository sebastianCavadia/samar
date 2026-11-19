-- V4: alertas internas (RF037 al prestador, RF039 al turista). Antes solo se escribían en el log.
CREATE TABLE notificacion (
  id          BIGSERIAL    PRIMARY KEY,
  id_usuario  BIGINT       NOT NULL,                   -- usuario en cuentas-service (otra base: sin llave foránea)
  asunto      VARCHAR(120) NOT NULL,
  mensaje     VARCHAR(500) NOT NULL,
  leida       BOOLEAN      NOT NULL DEFAULT FALSE,
  creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notificacion_usuario ON notificacion (id_usuario, creado_en DESC);
CREATE INDEX idx_notificacion_no_leidas ON notificacion (id_usuario) WHERE NOT leida;
