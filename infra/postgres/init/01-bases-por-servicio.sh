#!/usr/bin/env bash
# Una base y un usuario por microservicio (ADR-02 / ADR-06).
# Las contraseñas se toman de variables de entorno para no dejarlas fijas en el repositorio.
# Este script solo se ejecuta cuando el volumen de Postgres está vacío.
set -euo pipefail

escapar() {
  printf "%s" "$1" | sed "s/'/''/g"
}

crear() {
  local usuario="$1" base="$2" password="$3"
  password="$(escapar "$password")"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    CREATE USER "$usuario" WITH PASSWORD '$password';
    CREATE DATABASE "$base" OWNER "$usuario";
    REVOKE CONNECT ON DATABASE "$base" FROM PUBLIC;
EOSQL
}

crear cuentas samar_cuentas "${CUENTAS_DB_PASSWORD:-cuentas}"
crear oferta samar_oferta "${OFERTA_DB_PASSWORD:-oferta}"
crear reservas samar_reservas "${RESERVAS_DB_PASSWORD:-reservas}"
crear recomendaciones samar_recomendaciones "${RECOMENDACIONES_DB_PASSWORD:-recomendaciones}"
