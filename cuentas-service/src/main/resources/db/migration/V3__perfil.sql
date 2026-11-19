-- V3: editar perfil con foto (RF008). V1 y V2 ya están aplicadas: nunca se editan.
-- La foto vive en su propia tabla para no cargar los bytes cada vez que se lee un usuario
-- (por ejemplo, en cada inicio de sesión).
CREATE TABLE foto_perfil (
  id_usuario      BIGINT      PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
  contenido       BYTEA       NOT NULL,
  tipo            VARCHAR(20) NOT NULL CHECK (tipo IN ('image/jpeg', 'image/png')),
  actualizada_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
