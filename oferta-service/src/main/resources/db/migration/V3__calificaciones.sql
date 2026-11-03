-- V3: calificaciones (RF030, RF075, RF076, RF080, RF081). V1 ya está aplicada: nunca se edita.

-- Copia del nombre del turista (viene de reservas): las reseñas se muestran sin consultar a cuentas.
ALTER TABLE calificacion ADD COLUMN nombre_turista VARCHAR(120);

-- RF030: cuándo respondió el prestador. Hay respuesta si y solo si hay fecha de respuesta.
ALTER TABLE calificacion ADD COLUMN fecha_respuesta TIMESTAMPTZ;
ALTER TABLE calificacion ADD CONSTRAINT ck_calificacion_respuesta
  CHECK ((respuesta_prestador IS NULL) = (fecha_respuesta IS NULL));

-- Reseñas de un servicio, de la más reciente a la más antigua (RF081 filtra además por puntuación).
CREATE INDEX idx_calificacion_fecha ON calificacion (id_servicio, fecha DESC);

-- RF080: el promedio y el total siempre salen de las calificaciones guardadas.
UPDATE servicio_turistico s
   SET total_calificaciones  = (SELECT count(*) FROM calificacion c WHERE c.id_servicio = s.id),
       calificacion_promedio = (SELECT round(avg(c.puntuacion), 1) FROM calificacion c WHERE c.id_servicio = s.id);
