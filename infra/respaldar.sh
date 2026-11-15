#!/usr/bin/env bash
# Copia de seguridad de las cuatro bases. Guarda una carpeta por día y borra las de más de 7 días.
#   bash infra/respaldar.sh                 # a mano
#   crontab -e  →  0 3 * * * bash $HOME/samar/infra/respaldar.sh   # todos los días a las 3 a. m.
# Restaurar una base: docker exec -i samar-db pg_restore -U postgres -d samar_oferta --clean < samar_oferta.dump
set -euo pipefail
DESTINO="${RESPALDOS:-$HOME/respaldos}/$(date +%F)"
mkdir -p "$DESTINO"
for base in samar_cuentas samar_oferta samar_reservas samar_recomendaciones; do
  docker exec samar-db pg_dump -U postgres -Fc "$base" > "$DESTINO/$base.dump"
done
find "$(dirname "$DESTINO")" -mindepth 1 -maxdepth 1 -type d -mtime +7 -exec rm -rf {} +
echo "Respaldo en $DESTINO ($(du -sh "$DESTINO" | cut -f1))"
