#!/usr/bin/env bash
# Seed reproducible de los datos demo de SAMAR.
#
# Uso local (docker compose de desarrollo, gateway en 8080):
#   bash pruebas/seed-demo.sh
#
# Uso en la VM (detrás de Caddy):
#   BASE_URL=https://samar.duckdns.org SEED_BD=1 bash pruebas/seed-demo.sh
#
# Variables:
#   BASE_URL        URL del gateway (por defecto https://$SAMAR_DOMINIO si hay .env, si no localhost:8080)
#   ADMIN_CORREO    correo del admin (por defecto admin@samar.co)
#   ADMIN_PASSWORD  clave del admin (por defecto Admin12345)
#   SEED_BD         auto|1|0  (auto: usa la base solo si BASE_URL es local)
#   DB_CONTENEDOR   nombre del contenedor Postgres (por defecto samar-db)
set -euo pipefail
export PYTHONIOENCODING=utf-8

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
if [ -f "$RAIZ/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$RAIZ/.env"
  set +a
fi

BASE_URL="${BASE_URL:-}"
if [ -z "$BASE_URL" ] && [ -n "${SAMAR_DOMINIO:-}" ]; then
  BASE_URL="https://$SAMAR_DOMINIO"
fi
BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_CORREO="${ADMIN_CORREO:-admin@samar.co}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin12345}"
CLAVE_DEMO="ClaveSegura1"
DB_CONTENEDOR="${DB_CONTENEDOR:-samar-db}"
SEED_BD="${SEED_BD:-auto}"

if [ "$SEED_BD" = auto ]; then
  case "$BASE_URL" in
    http://localhost*|http://127.0.0.1*) SEED_BD=1 ;;
    *) SEED_BD=0 ;;
  esac
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

paso() { printf '\n== %s ==\n' "$1" >&2; }
ok() { printf '  OK     %s\n' "$1" >&2; }
aviso() { printf '  AVISO  %s\n' "$1" >&2; }
fallar() { printf '  ERROR  %s\n' "$1" >&2; exit 1; }

http() { # METODO RUTA [TOKEN] [CUERPO] -> COD y RESP
  local metodo="$1" ruta="$2" token="${3:-}" cuerpo="${4:-}"
  local args=(-sS -o "$TMP/resp" -w "%{http_code}" -X "$metodo" "$BASE_URL$ruta" -H "Content-Type: application/json")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  # Desde un archivo: en Windows, curl recibe los argumentos en ANSI y las tildes llegan como JSON inválido.
  if [ -n "$cuerpo" ]; then printf '%s' "$cuerpo" > "$TMP/cuerpo"; args+=(--data-binary "@$TMP/cuerpo"); fi
  COD="$(curl "${args[@]}" || true)"
  RESP="$(cat "$TMP/resp" 2>/dev/null || true)"
}

campo() { # EXPRESION_PYTHON_SOBRE_d
  printf '%s' "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1" 2>/dev/null || true
}

login() { # CORREO CLAVE -> token (vacío si falla)
  http POST /api/cuentas/auth/login "" "{\"correo\":\"$1\",\"password\":\"$2\"}"
  [ "$COD" = 200 ] && campo "d['token']" || true
}

esperar_gateway() {
  paso "Esperando al gateway en $BASE_URL"
  local i
  for i in $(seq 1 90); do
    http GET /api/oferta/servicios "" ""
    if [ "$COD" = 200 ]; then ok "gateway disponible"; return; fi
    sleep 2
  done
  fallar "el gateway no respondió. ¿Está levantado el compose?"
}

esperar_admin() {
  paso "Entrando como administrador"
  local i
  for i in $(seq 1 30); do
    TA="$(login "$ADMIN_CORREO" "$ADMIN_PASSWORD")"
    if [ -n "$TA" ]; then ok "sesión de $ADMIN_CORREO"; return; fi
    sleep 2
  done
  fallar "no se pudo entrar como admin. Revisá ADMIN_CORREO/ADMIN_PASSWORD (el admin solo se crea al primer arranque)."
}

registrar_turista() { # CORREO NOMBRE DOCUMENTO FECHA_NACIMIENTO TELEFONO -> token
  local correo="$1" nombre="$2" documento="$3" nacimiento="$4" telefono="$5"
  local token
  token="$(login "$correo" "$CLAVE_DEMO")"
  if [ -n "$token" ]; then echo "$token"; return; fi

  http POST /api/cuentas/auth/registro/turista "" "{\"nombre\":\"$nombre\",\"correo\":\"$correo\",\"password\":\"$CLAVE_DEMO\",\"telefono\":\"$telefono\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"$documento\",\"nacionalidad\":\"Colombiana\",\"paisResidencia\":\"Colombia\",\"fechaNacimiento\":\"$nacimiento\",\"discapacidad\":false}"
  if [ "$COD" = 201 ]; then
    token="$(login "$correo" "$CLAVE_DEMO")"
    [ -n "$token" ] || fallar "se registró $correo pero no pudo iniciar sesión"
    ok "turista $correo"
    echo "$token"
    return
  fi
  if [ "$COD" = 409 ]; then
    token="$(login "$correo" "$CLAVE_DEMO")"
    [ -n "$token" ] || fallar "$correo ya existe pero la clave no es $CLAVE_DEMO"
    ok "turista $correo (ya existía)"
    echo "$token"
    return
  fi
  fallar "no se pudo registrar $correo ($COD): $RESP"
}

registrar_prestador() { # CORREO NOMBRE TIPO NIT RNT DIRECCION ZONA IDIOMAS_JSON -> token
  local correo="$1" nombre="$2" tipo="$3" nit="$4" rnt="$5" direccion="$6" zona="$7" idiomas="$8"
  local token
  token="$(login "$correo" "$CLAVE_DEMO")"
  if [ -n "$token" ]; then echo "$token"; return; fi

  http POST /api/cuentas/auth/registro/prestador "" "{\"nombre\":\"$nombre\",\"correo\":\"$correo\",\"password\":\"$CLAVE_DEMO\",\"telefono\":\"3001112233\",\"tipoPrestador\":\"$tipo\",\"nit\":\"$nit\",\"rnt\":\"$rnt\",\"direccion\":\"$direccion\",\"zona\":\"$zona\",\"idiomasGuia\":$idiomas}"
  if [ "$COD" = 201 ]; then
    token="$(login "$correo" "$CLAVE_DEMO")"
    [ -n "$token" ] || fallar "se registró $correo pero no pudo iniciar sesión"
    ok "prestador $correo"
    echo "$token"
    return
  fi
  if [ "$COD" = 409 ]; then
    token="$(login "$correo" "$CLAVE_DEMO")"
    [ -n "$token" ] || fallar "$correo ya existe pero la clave no es $CLAVE_DEMO"
    ok "prestador $correo (ya existía)"
    echo "$token"
    return
  fi
  fallar "no se pudo registrar $correo ($COD): $RESP"
}

perfil() { # TOKEN -> deja RESP con el perfil
  http GET /api/cuentas/perfil "$1" ""
  [ "$COD" = 200 ] || fallar "no se pudo leer el perfil ($COD): $RESP"
}

validar_prestador() { # TOKEN_PRESTADOR
  perfil "$1"
  if [ "$(campo "d['prestadorValidado']")" = "True" ]; then
    ok "prestador ya validado"
    return
  fi
  local id
  id="$(campo "d['id']")"
  http POST "/api/cuentas/admin/prestadores/$id/validar" "$TA" ""
  [ "$COD" = 200 ] || fallar "no se pudo validar al prestador ($COD): $RESP"
  ok "prestador validado por el admin"
}

id_servicio_por_nombre() { # NOMBRE -> id
  http GET /api/oferta/prestador/servicios "$TP_TOURS" ""
  [ "$COD" = 200 ] || fallar "no se pudieron listar los servicios del prestador ($COD): $RESP"
  printf '%s' "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(next((s['id'] for s in d if s['nombre']==sys.argv[1]), ''))" "$1"
}

crear_servicio() { # NOMBRE JSON -> id
  local nombre="$1" json="$2" id
  id="$(id_servicio_por_nombre "$nombre")"
  if [ -n "$id" ]; then
    ok "servicio «$nombre» (ya existía, id $id)"
    echo "$id"
    return
  fi
  http POST /api/oferta/prestador/servicios "$TP_TOURS" "$json"
  [ "$COD" = 201 ] || fallar "no se pudo crear «$nombre» ($COD): $RESP"
  id="$(campo "d['id']")"
  ok "servicio «$nombre» (id $id)"
  echo "$id"
}

abrir_fechas() { # ID_SERVICIO DESDE HASTA CUPOS [HORA]
  local id="$1" desde="$2" hasta="$3" cupos="$4" hora="${5:-}"
  local cuerpo="{\"desde\":\"$desde\",\"hasta\":\"$hasta\",\"cupos\":$cupos"
  [ -n "$hora" ] && cuerpo="$cuerpo,\"hora\":\"$hora\""
  cuerpo="$cuerpo}"
  http POST "/api/reservas/prestador/servicios/$id/disponibilidad" "$TP_TOURS" "$cuerpo"
  [ "$COD" = 200 ] || fallar "no se pudieron abrir fechas del servicio $id ($COD): $RESP"
  ok "fechas abiertas para el servicio $id ($(campo "d['creadas']") creadas, $(campo "d['actualizadas']") actualizadas)"
}

id_disponibilidad() { # ID_SERVICIO FECHA -> id
  http GET "/api/reservas/disponibilidad?servicio=$1&desde=$2&hasta=$2" "" ""
  [ "$COD" = 200 ] || fallar "no se pudo consultar disponibilidad ($COD): $RESP"
  campo "d[0]['id']"
}

reservar() { # TOKEN ID_DISPONIBILIDAD PERSONAS -> id
  http POST /api/reservas "$1" "{\"idDisponibilidad\":$2,\"personas\":$3}"
  [ "$COD" = 201 ] || fallar "no se pudo reservar ($COD): $RESP"
  campo "d['id']"
}

pagar() { # TOKEN ID_RESERVA
  http POST "/api/reservas/$2/pagos" "$1" '{"medio":"PASARELA"}'
  [ "$COD" = 200 ] || fallar "no se pudo pagar la reserva $2 ($COD): $RESP"
}

confirmar() { # ID_RESERVA
  http POST "/api/reservas/prestador/reservas/$1/confirmar" "$TP_TOURS" ""
  [ "$COD" = 200 ] || fallar "no se pudo confirmar la reserva $1 ($COD): $RESP"
}

cancelar() { # TOKEN ID_RESERVA MOTIVO
  http POST "/api/reservas/$2/cancelar" "$1" "{\"motivo\":\"$3\"}"
  [ "$COD" = 200 ] || fallar "no se pudo cancelar la reserva $2 ($COD): $RESP"
}

esperar_estado() { # TOKEN ID_RESERVA ESTADO
  local i estado
  for i in $(seq 1 60); do
    http GET "/api/reservas/$2" "$1" ""
    estado="$(campo "d['estado']")"
    [ "$estado" = "$3" ] && return 0
    sleep 3
  done
  return 1
}

sql_cuentas() { docker exec "$DB_CONTENEDOR" psql -U postgres -d samar_cuentas -tAc "$1"; }
sql_reservas() { docker exec "$DB_CONTENEDOR" psql -U postgres -d samar_reservas -tAc "$1"; }

# ------------------------------------------------------------------ inicio
esperar_gateway
esperar_admin

paso "Cuentas demo"
TT_ANA="$(registrar_turista "ana@demo.co" "Ana Torres" "1098765432" "1995-03-15" "3001234567")"
TT_LAURA="$(registrar_turista "laura@demo.co" "Laura Gomez" "1098765433" "1994-07-20" "3009876543")"
TP_TOURS="$(registrar_prestador "tours@demo.co" "Tours Caribe" "AGENCIA_TURISTICA" "900123456-1" "RNT-12345" "Centro Histórico, Santa Marta" "Centro Histórico" '["Español","Inglés"]')"
TP_HOSTAL="$(registrar_prestador "hostal@demo.co" "Hostal Taganga" "HOTEL" "900987654-1" "RNT-67890" "Taganga, Santa Marta" "Taganga" '[]')"

validar_prestador "$TP_TOURS"

perfil "$TP_HOSTAL"
if [ "$(campo "d['estadoValidacion']")" != "PENDIENTE" ]; then
  aviso "hostal@demo.co no está PENDIENTE (estado: $(campo "d['estadoValidacion']")); se deja como está"
else
  ok "prestador hostal@demo.co queda pendiente de validación"
fi

paso "Servicios de la agencia"
S_PASEO="$(crear_servicio "Paseo a Minca" '{"tipo":"ACTIVIDAD","nombre":"Paseo a Minca","descripcion":"Caminata por cascadas y cafetales de la Sierra Nevada, con guía local y almuerzo típico.","modalidad":"RESERVA","precioBase":120000,"direccion":"Vía a Minca km 5","zona":"Minca","accesible":false,"categorias":["NATURALEZA","AVENTURA"],"imagenes":[{"url":"https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1200&q=80","descripcion":"Cascada en la selva de Minca"}],"actividad":{"duracionHoras":6.0,"horaInicio":"08:00","dificultad":"MEDIA","edadMinima":8}}')"
S_TOURMINCA="$(crear_servicio "Tour Minca Café y Cascadas" '{"tipo":"ACTIVIDAD","nombre":"Tour Minca Café y Cascadas","descripcion":"Recorrido por una finca cafetera, cascadas y miradores de Minca.","modalidad":"RESERVA","precioBase":150000,"direccion":"Minca, Sierra Nevada","zona":"Minca","accesible":false,"categorias":["NATURALEZA","CULTURA"],"imagenes":[{"url":"https://images.unsplash.com/photo-1551632811-561732d1e306?auto=format&fit=crop&w=1200&q=80","descripcion":"Sendero de montaña"}],"actividad":{"duracionHoras":7.0,"horaInicio":"07:30","dificultad":"MEDIA","edadMinima":10}}')"
S_HOTEL="$(crear_servicio "Hotel Boutique Rodadero" '{"tipo":"HOSPEDAJE","nombre":"Hotel Boutique Rodadero","descripcion":"Habitación frente al mar con desayuno, piscina y acceso directo a la playa.","modalidad":"RESERVA","precioBase":380000,"direccion":"Calle 5 # 1-20, Rodadero","zona":"Rodadero","accesible":true,"categorias":["PLAYA","DESCANSO"],"imagenes":[{"url":"https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80","descripcion":"Hotel frente al mar"}],"hospedaje":{"tipoHabitacion":"Doble con vista al mar","capacidadPersonas":3,"horaCheckIn":"15:00","horaCheckOut":"12:00"}}')"
S_TAYRONA="$(crear_servicio "Cabo San Juan Tayrona" '{"tipo":"ACTIVIDAD","nombre":"Cabo San Juan Tayrona","descripcion":"Caminata guiada por el Parque Tayrona hasta Cabo San Juan, con paradas en playas.","modalidad":"RESERVA","precioBase":180000,"direccion":"Entrada principal Parque Tayrona","zona":"Tayrona","accesible":false,"categorias":["PLAYA","NATURALEZA"],"imagenes":[{"url":"https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80","descripcion":"Playa del Caribe"}],"actividad":{"duracionHoras":8.0,"horaInicio":"06:00","dificultad":"ALTA","edadMinima":12}}')"
S_TAGANGA="$(crear_servicio "Almuerzo Caribe en Taganga" '{"tipo":"GASTRONOMICO","nombre":"Almuerzo Caribe en Taganga","descripcion":"Pescado fresco, patacón y coco frente a la bahía de Taganga.","modalidad":"WALK_IN","precioBase":65000,"direccion":"Malecón de Taganga","zona":"Taganga","accesible":true,"categorias":["GASTRONOMIA","PLAYA"],"imagenes":[{"url":"https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80","descripcion":"Plato caribeño"}],"gastronomico":{"tipoComida":"Almuerzo","aforo":40,"horaApertura":"11:00","horaCierre":"16:00","dietas":["VEGETARIANA"]}}')"
S_SUNSET="$(crear_servicio "Sunset Centro Histórico" '{"tipo":"ACTIVIDAD","nombre":"Sunset Centro Histórico","descripcion":"Recorrido por la historia de Santa Marta y atardecer en el malecón.","modalidad":"RESERVA","precioBase":80000,"direccion":"Parque de los Novios","zona":"Centro Histórico","accesible":true,"categorias":["CULTURA","DESCANSO"],"imagenes":[{"url":"https://images.unsplash.com/photo-1519501025264-65ba15a82390?auto=format&fit=crop&w=1200&q=80","descripcion":"Centro histórico al atardecer"}],"actividad":{"duracionHoras":2.5,"horaInicio":"17:00","dificultad":"BAJA"}}')"

paso "Disponibilidad"
HOY="$(date +%F)"
FIN="$(date -d "+30 days" +%F)"
abrir_fechas "$S_PASEO" "$HOY" "$FIN" 20 "08:00"
abrir_fechas "$S_TOURMINCA" "$HOY" "$FIN" 15 "07:30"
abrir_fechas "$S_HOTEL" "$HOY" "$FIN" 3 ""
abrir_fechas "$S_TAYRONA" "$HOY" "$FIN" 20 "06:00"
abrir_fechas "$S_SUNSET" "$HOY" "$FIN" 25 "17:00"

paso "Reservas de ejemplo"
http GET /api/reservas/mis "$TT_ANA" ""
YA_HAY_ANA="$(campo "len(d)")"
http GET /api/reservas/mis "$TT_LAURA" ""
YA_HAY_LAURA="$(campo "len(d)")"
if [ "${YA_HAY_ANA:-0}" != "0" ] || [ "${YA_HAY_LAURA:-0}" != "0" ]; then
  aviso "ya existen reservas de las turistas demo; se omite la creación de reservas para no duplicar"
else
  D7="$(date -d "+7 days" +%F)"
  D10="$(date -d "+10 days" +%F)"
  D5="$(date -d "+5 days" +%F)"
  D3="$(date -d "+3 days" +%F)"

  DISP_PASEO="$(id_disponibilidad "$S_PASEO" "$D7")"
  [ -n "$DISP_PASEO" ] || fallar "no hay disponibilidad del Paseo a Minca para $D7"
  R_PENDIENTE="$(reservar "$TT_ANA" "$DISP_PASEO" 2)"
  ok "reserva PENDIENTE #$R_PENDIENTE (Paseo a Minca, $D7)"

  DISP_HOTEL="$(id_disponibilidad "$S_HOTEL" "$D10")"
  [ -n "$DISP_HOTEL" ] || fallar "no hay disponibilidad del hotel para $D10"
  R_CONFIRMADA="$(reservar "$TT_ANA" "$DISP_HOTEL" 2)"
  pagar "$TT_ANA" "$R_CONFIRMADA"
  confirmar "$R_CONFIRMADA"
  ok "reserva CONFIRMADA #$R_CONFIRMADA (Hotel Boutique Rodadero, $D10)"

  DISP_SUNSET="$(id_disponibilidad "$S_SUNSET" "$D5")"
  [ -n "$DISP_SUNSET" ] || fallar "no hay disponibilidad del sunset para $D5"
  R_CANCELADA="$(reservar "$TT_LAURA" "$DISP_SUNSET" 1)"
  pagar "$TT_LAURA" "$R_CANCELADA"
  confirmar "$R_CANCELADA"
  cancelar "$TT_LAURA" "$R_CANCELADA" "Cambio de planes"
  ok "reserva CANCELADA #$R_CANCELADA (Sunset Centro Histórico, $D5)"

  if [ "$SEED_BD" = 1 ] && docker exec "$DB_CONTENEDOR" psql -U postgres -d samar_reservas -tAc "select 1" >/dev/null 2>&1; then
    DISP_TOUR="$(id_disponibilidad "$S_TOURMINCA" "$D3")"
    [ -n "$DISP_TOUR" ] || fallar "no hay disponibilidad del Tour Minca para $D3"
    R_ANA="$(reservar "$TT_ANA" "$DISP_TOUR" 1)"
    pagar "$TT_ANA" "$R_ANA"
    confirmar "$R_ANA"
    R_LAURA="$(reservar "$TT_LAURA" "$DISP_TOUR" 1)"
    pagar "$TT_LAURA" "$R_LAURA"
    confirmar "$R_LAURA"
    ok "reservas #$R_ANA y #$R_LAURA listas para completar"

    # La tarea automática (RF049) completa las reservas cuando la fecha ya pasó.
    # Se usa el id de la disponibilidad como días hacia atrás para no chocar con corridas anteriores.
    AYER="$(date -d "-$((DISP_TOUR + 1)) days" +%F)"
    sql_reservas "update disponibilidad set fecha = '$AYER' where id = $DISP_TOUR" >/dev/null
    ok "fecha de la disponibilidad movida a $AYER; esperando la tarea automática"

    if esperar_estado "$TT_ANA" "$R_ANA" COMPLETADA && esperar_estado "$TT_LAURA" "$R_LAURA" COMPLETADA; then
      ok "las reservas pasaron a COMPLETADA"
    else
      fallar "las reservas no se completaron; revisá TAREA_MS y los logs de reservas-service"
    fi

    http POST "/api/reservas/$R_ANA/calificacion" "$TT_ANA" '{"puntuacion":5,"comentario":"Excelente experiencia, guías muy amables."}'
    [ "$COD" = 201 ] || fallar "no se pudo calificar la reserva $R_ANA ($COD): $RESP"
    http POST "/api/reservas/$R_LAURA/calificacion" "$TT_LAURA" '{"puntuacion":4,"comentario":"Muy buen tour, aunque llovió un poco."}'
    [ "$COD" = 201 ] || fallar "no se pudo calificar la reserva $R_LAURA ($COD): $RESP"
    ok "dos reseñas creadas (5 y 4 estrellas)"

    http GET "/api/oferta/prestador/calificaciones?sinResponder=true" "$TP_TOURS" ""
    CAL_ID="$(printf '%s' "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(next((r['id'] for r in d['resenas']['contenido'] if (r.get('comentario') or '').startswith('Excelente experiencia')), ''))")"
    if [ -n "$CAL_ID" ]; then
      http PUT "/api/oferta/prestador/calificaciones/$CAL_ID/respuesta" "$TP_TOURS" '{"respuesta":"¡Gracias Ana! Nos alegra que hayas disfrutado Minca."}'
      if [ "$COD" = 200 ]; then
        ok "respuesta pública del prestador"
      else
        aviso "no se pudo responder la reseña ($COD): $RESP"
      fi
    else
      aviso "no se encontró la reseña de Ana para responder"
    fi
  else
    aviso "sin acceso a la base: se omite la reserva completada y las reseñas"
  fi
fi

# Se hace al final para no bloquear la reserva de Laura en una actividad con edad mínima.
if [ "$SEED_BD" = 1 ]; then
  if docker exec "$DB_CONTENEDOR" psql -U postgres -d samar_cuentas -tAc "select 1" >/dev/null 2>&1; then
    sql_cuentas "update turista set fecha_nacimiento = null where id_usuario = (select id from usuario where correo = 'laura@demo.co')" >/dev/null
    ok "Laura queda sin fecha de nacimiento (para las pruebas de edad mínima)"
  else
    aviso "sin acceso a $DB_CONTENEDOR; no se pudo dejar a Laura sin fecha de nacimiento"
  fi
else
  aviso "SEED_BD=0: no se pudo dejar a Laura sin fecha de nacimiento"
fi

paso "Listo"
cat <<RESUMEN
  URL:            $BASE_URL
  Admin:          $ADMIN_CORREO
  Turista:        ana@demo.co / $CLAVE_DEMO
  Turista:        laura@demo.co / $CLAVE_DEMO
  Prestador:      tours@demo.co / $CLAVE_DEMO (validado)
  Prestador:      hostal@demo.co / $CLAVE_DEMO (pendiente)
  Servicios:      Paseo a Minca, Tour Minca Café y Cascadas, Hotel Boutique Rodadero,
                  Cabo San Juan Tayrona, Almuerzo Caribe en Taganga, Sunset Centro Histórico
RESUMEN
