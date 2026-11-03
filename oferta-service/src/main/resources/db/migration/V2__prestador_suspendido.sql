-- V2 (ADR-13): si el administrador desactiva o bloquea a un prestador, sus ofertas dejan de verse.
-- Lo marca una tarea que consulta a cuentas-service; no lo cambia el prestador al editar su servicio.
ALTER TABLE servicio_turistico ADD COLUMN prestador_suspendido BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_servicio_suspendido ON servicio_turistico (prestador_suspendido) WHERE prestador_suspendido;
