-- V2: lo que necesita el flujo de reserva (RF036-RF057). V1 ya está aplicada: nunca se edita.

-- El motivo sirve para cancelación (RF040) y para rechazo (RF038).
ALTER TABLE reserva RENAME COLUMN motivo_cancelacion TO motivo;

-- Personas reales (en hospedaje se ocupa 1 habitación para N personas, RF064).
ALTER TABLE reserva ADD COLUMN personas INT NOT NULL DEFAULT 1 CHECK (personas > 0);

-- Copia del nombre para que el prestador vea quién reservó sin consultar a cuentas cada vez.
ALTER TABLE reserva ADD COLUMN nombre_turista VARCHAR(120);

-- Bloqueo optimista: cada cambio de estado incrementa la versión (confirmar y expirar a la vez → uno falla).
ALTER TABLE reserva ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE detalle_reserva ADD COLUMN tipo_servicio VARCHAR(20);

ALTER TABLE bitacora_reserva ADD COLUMN detalle VARCHAR(200);

-- En V1, UNIQUE (id_servicio, fecha, hora) permitía duplicados cuando hora es NULL
-- (en SQL dos NULL son distintos). NULLS NOT DISTINCT (PostgreSQL 15+) lo corrige.
DO $$
DECLARE
  nombre text;
BEGIN
  SELECT conname INTO nombre FROM pg_constraint
   WHERE conrelid = 'disponibilidad'::regclass AND contype = 'u';
  IF nombre IS NOT NULL THEN
    EXECUTE format('ALTER TABLE disponibilidad DROP CONSTRAINT %I', nombre);
  END IF;
END $$;
ALTER TABLE disponibilidad ADD CONSTRAINT uq_disponibilidad UNIQUE NULLS NOT DISTINCT (id_servicio, fecha, hora);

CREATE INDEX idx_pago_reserva ON pago (id_reserva);
CREATE INDEX idx_detalle_reserva ON detalle_reserva (id_reserva);
CREATE INDEX idx_bitacora_reserva ON bitacora_reserva (id_reserva);
