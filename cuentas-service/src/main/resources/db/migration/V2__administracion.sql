-- V2: panel del administrador (RF091-RF100). V1 ya está aplicada: nunca se edita.

-- RF095: la validación pasa de sí/no a tres estados, para poder rechazar con motivo.
ALTER TABLE prestador_turistico
  ADD COLUMN estado_validacion VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
    CHECK (estado_validacion IN ('PENDIENTE', 'VALIDADO', 'RECHAZADO')),
  ADD COLUMN motivo_rechazo VARCHAR(300);

UPDATE prestador_turistico SET estado_validacion = 'VALIDADO' WHERE validado;

-- La columna booleana queda reemplazada (su índice se elimina con ella).
ALTER TABLE prestador_turistico DROP COLUMN validado;
CREATE INDEX idx_prestador_validacion ON prestador_turistico (estado_validacion);

-- RF097 + RF099: cada acción de un administrador, con su área responsable y el motivo.
CREATE TABLE bitacora_admin (
  id                BIGSERIAL PRIMARY KEY,
  id_administrador  BIGINT       NOT NULL REFERENCES usuario (id),
  area_responsable  VARCHAR(120),
  id_usuario        BIGINT       NOT NULL REFERENCES usuario (id),
  accion            VARCHAR(30)  NOT NULL CHECK (accion IN
                      ('VALIDAR_PRESTADOR', 'RECHAZAR_PRESTADOR', 'CAMBIAR_ESTADO', 'EDITAR_DATOS', 'CREAR_ADMINISTRADOR')),
  detalle           VARCHAR(400),
  fecha             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_bitacora_usuario ON bitacora_admin (id_usuario, fecha DESC);
CREATE INDEX idx_bitacora_fecha ON bitacora_admin (fecha DESC);
CREATE INDEX idx_usuario_busqueda ON usuario (rol, estado);
