#!/usr/bin/env bash
# Pone en producción la última versión de la rama main. Se corre en la VM, dentro del repositorio:
#   bash desplegar.sh            # trae los cambios con git pull, reconstruye y reinicia lo que cambió
#   bash desplegar.sh --sin-pull # reconstruye con el código que ya está en la carpeta
# Los datos no se tocan: el volumen de Postgres se conserva y Flyway aplica las migraciones nuevas.
set -euo pipefail
cd "$(dirname "$0")"

C=(docker compose -f docker-compose.prod.yml)
SERVICIOS=(cuentas-service oferta-service reservas-service recomendaciones-service api-gateway frontend)

[ -f .env ] || { echo "Falta .env: cp .env.example .env y completa el dominio y los secretos."; exit 1; }

if [ "${1:-}" != "--sin-pull" ]; then
  echo "== Trayendo cambios"
  git pull --ff-only
  git log -1 --format='   versión %h: %s (%an, %ar)'
fi

# De uno en uno: en una VM pequeña, compilar los seis a la vez se queda sin memoria.
echo "== Construyendo imágenes"
for s in "${SERVICIOS[@]}"; do
  echo "   $s"
  "${C[@]}" build -q "$s"
done

echo "== Reiniciando lo que cambió"
"${C[@]}" up -d --remove-orphans

sano() { # SERVICIO:PUERTO, consultado desde el contenedor de Caddy por la red interna
  "${C[@]}" exec -T frontend wget -qO- "http://$1/actuator/health" 2>/dev/null | grep -q '"UP"'
}

echo "== Esperando a que todos los servicios respondan"
for i in $(seq 1 60); do
  if sano cuentas-service:8081 && sano oferta-service:8082 && sano reservas-service:8083 \
     && sano recomendaciones-service:8084 && sano api-gateway:8080; then
    docker image prune -f >/dev/null
    "${C[@]}" ps --format 'table {{.Service}}\t{{.Status}}'
    DOMINIO=$(grep -E '^SAMAR_DOMINIO=' .env | cut -d= -f2-)
    echo "Listo: https://$DOMINIO"
    exit 0
  fi
  sleep 3
done

echo "Algún servicio no respondió en 3 minutos. Revisa los logs:"
echo "   docker compose -f docker-compose.prod.yml logs --tail 80"
exit 1
