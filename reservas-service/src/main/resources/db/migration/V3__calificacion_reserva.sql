-- V3: la reserva recuerda si ya se calificó (RF076). La calificación en sí vive en oferta (ADR-11);
-- aquí queda una copia de la puntuación para mostrarla en "Mis reservas" sin consultar a oferta.
ALTER TABLE reserva ADD COLUMN calificacion SMALLINT CHECK (calificacion BETWEEN 1 AND 5);
ALTER TABLE reserva ADD COLUMN calificada_en TIMESTAMPTZ;
ALTER TABLE reserva ADD CONSTRAINT ck_reserva_calificada
  CHECK ((calificacion IS NULL) = (calificada_en IS NULL));
