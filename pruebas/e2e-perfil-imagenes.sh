#!/usr/bin/env bash
# Prueba de punta a punta de: editar perfil y foto (RF008), cambiar contraseña (RF009), imágenes de la
# galería (RF019), filtro por dieta (RF079), notificaciones internas (RF037, RF039) y Swagger (RNF-034).
# Uso: bash pruebas/e2e-perfil-imagenes.sh   (con el sistema levantado y los datos de pruebas/seed-demo.sh)
export PYTHONIOENCODING=utf-8
G=localhost:8080
J='Content-Type: application/json'
RUN=$(date +%s)
TMP=$(mktemp -d)
PASA=0; FALLA=0

llamar() { # METODO RUTA TOKEN [CUERPO]  → deja $COD y $RESP. El cuerpo va por archivo (tildes en Windows).
  local args=(-s -o "$TMP/r" -w "%{http_code}" -X "$1" "$G$2" -H "$J")
  [ -n "$3" ] && args+=(-H "Authorization: Bearer $3")
  if [ -n "$4" ]; then printf '%s' "$4" > "$TMP/cuerpo"; args+=(--data-binary "@$TMP/cuerpo"); fi
  COD=$(curl "${args[@]}"); RESP=$(cat "$TMP/r")
}
subir() { # METODO RUTA TOKEN ARCHIVO TIPO  → multipart con el campo "archivo"
  COD=$(cd "$TMP" && curl -s -o r -w "%{http_code}" -X "$1" "$G$2" -H "Authorization: Bearer $3" -F "archivo=@$4;type=$5")
  RESP=$(cat "$TMP/r")
}
cabeceras() { # RUTA [TOKEN] → cabeceras de la respuesta
  if [ -n "$2" ]; then curl -s -D - -o /dev/null "$G$1" -H "Authorization: Bearer $2"; else curl -s -D - -o /dev/null "$G$1"; fi
}
campo() { echo "$RESP" | python -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1" 2>/dev/null; }
verificar() { # DESCRIPCION CODIGO_ESPERADO [EXPRESION_PYTHON_SOBRE_d]
  local ok=1
  [ "$COD" != "$2" ] && ok=0
  if [ $ok = 1 ] && [ -n "$3" ]; then [ "$(campo "$3")" = "True" ] || ok=0; fi
  if [ $ok = 1 ]; then PASA=$((PASA+1)); printf "  OK     %-72s %s\n" "$1" "$COD"
  else FALLA=$((FALLA+1)); printf "  FALLO  %-72s %s  %s\n" "$1" "$COD" "$(echo "$RESP" | head -c 220)"; fi
}
comprobar() { # DESCRIPCION CONDICION_BASH
  if eval "$2"; then PASA=$((PASA+1)); printf "  OK     %s\n" "$1"
  else FALLA=$((FALLA+1)); printf "  FALLO  %s\n" "$1"; fi
}
tok() { llamar POST /api/cuentas/auth/login "" "{\"correo\":\"$1\",\"password\":\"$2\"}"; campo "d['token']"; }
dia() { date -d "+$1 day" +%F; }

# Imágenes de prueba: un PNG real de 2x2, un "JPEG" (basta su firma), un GIF y un archivo de más de 2 MB.
python - "$TMP" <<'PY'
import sys, zlib, struct, os
d = sys.argv[1]
def bloque(t, datos):
    return struct.pack('>I', len(datos)) + t + datos + struct.pack('>I', zlib.crc32(t + datos) & 0xffffffff)
filas = b''.join(b'\x00' + b'\x17\x49\x7a' * 2 for _ in range(2))
png = b'\x89PNG\r\n\x1a\n' + bloque(b'IHDR', struct.pack('>IIBBBBB', 2, 2, 8, 2, 0, 0, 0)) \
      + bloque(b'IDAT', zlib.compress(filas)) + bloque(b'IEND', b'')
open(os.path.join(d, 'foto.png'), 'wb').write(png)
open(os.path.join(d, 'foto.jpg'), 'wb').write(b'\xff\xd8\xff\xe0' + b'\x00' * 200)
open(os.path.join(d, 'animada.gif'), 'wb').write(b'GIF89a' + b'\x00' * 50)
open(os.path.join(d, 'grande.jpg'), 'wb').write(b'\xff\xd8\xff\xe0' + b'\x00' * (2 * 1024 * 1024 + 10))
PY

for i in $(seq 1 60); do
  llamar GET /api/oferta/servicios ""; A=$COD
  llamar GET "/api/reservas/disponibilidad?servicio=1" ""; B=$COD
  [ "$A" = 200 ] && [ "$B" = 200 ] && break; sleep 2
done

TA=$(tok ana@demo.co ClaveSegura1)
TP=$(tok tours@demo.co ClaveSegura1)
[ -z "$TA" ] || [ -z "$TP" ] && { echo "Faltan las cuentas demo: corre primero bash pruebas/seed-demo.sh"; exit 1; }

echo "=== Editar perfil (RF008) ==="
llamar GET /api/cuentas/perfil "$TA"; NOMBRE_ANA=$(campo "d['nombre']")
llamar PUT /api/cuentas/perfil "$TA" '{"nombre":"  Ana Torres Prueba ","telefono":"3005550101"}'
verificar "la turista cambia nombre y telefono (sin espacios sobrantes)" 200 "d['nombre']=='Ana Torres Prueba' and d['telefono']=='3005550101'"
llamar PUT /api/cuentas/perfil "$TA" '{"nombre":"","telefono":"1"}'
verificar "el nombre es obligatorio" 400
llamar PUT /api/cuentas/perfil "" '{"nombre":"Nadie"}'
verificar "sin sesion no se edita" 401
llamar GET /api/cuentas/perfil "$TP"; DIR_TOURS=$(campo "d['direccion']"); ZONA_TOURS=$(campo "d['zona']"); NOMBRE_TOURS=$(campo "d['nombre']")
llamar PUT /api/cuentas/perfil "$TP" "{\"nombre\":\"$NOMBRE_TOURS\",\"telefono\":\"3001112233\",\"direccion\":\"\",\"zona\":\"Centro\"}"
verificar "el prestador no puede dejar vacia la direccion del establecimiento" 400 "'direcci' in d['detail']"
llamar PUT /api/cuentas/perfil "$TP" "{\"nombre\":\"$NOMBRE_TOURS\",\"telefono\":\"3001112233\",\"direccion\":\"Calle 22 # 3-10\",\"zona\":\"Centro Historico\"}"
verificar "el prestador actualiza su ubicacion" 200 "d['direccion']=='Calle 22 # 3-10' and d['zona']=='Centro Historico'"
llamar PUT /api/cuentas/perfil "$TP" "{\"nombre\":\"$NOMBRE_TOURS\",\"telefono\":\"3001112233\",\"direccion\":\"$DIR_TOURS\",\"zona\":\"$ZONA_TOURS\"}"
llamar PUT /api/cuentas/perfil "$TA" "{\"nombre\":\"$NOMBRE_ANA\",\"telefono\":\"3001234567\"}"
verificar "y se restauran los datos originales" 200 "d['nombre']=='$NOMBRE_ANA'"

echo "=== Foto de perfil (RF008) ==="
subir PUT /api/cuentas/perfil/foto "$TA" foto.png image/png
verificar "sube una foto PNG" 204
llamar GET /api/cuentas/perfil "$TA"
verificar "el perfil indica que tiene foto" 200 "d['tieneFoto']==True"
H=$(cabeceras /api/cuentas/perfil/foto "$TA")
comprobar "la foto se descarga con sesion y como image/png" 'echo "$H" | grep -qi "^content-type: image/png"'
COD=$(curl -s -o /dev/null -w "%{http_code}" "$G/api/cuentas/perfil/foto")
comprobar "sin sesion la foto no se ve (401)" '[ "$COD" = 401 ]'
subir PUT /api/cuentas/perfil/foto "$TA" animada.gif image/gif
verificar "un GIF se rechaza aunque diga ser imagen" 400 "'JPEG o PNG' in d['detail']"
subir PUT /api/cuentas/perfil/foto "$TA" grande.jpg image/jpeg
verificar "mas de 2 MB se rechaza" 413
llamar DELETE /api/cuentas/perfil/foto "$TA"
verificar "quita la foto" 204
COD=$(curl -s -o /dev/null -w "%{http_code}" "$G/api/cuentas/perfil/foto" -H "Authorization: Bearer $TA")
comprobar "y ya no existe (404)" '[ "$COD" = 404 ]'

echo "=== Cambiar contrasena (RF009) ==="
llamar PUT /api/cuentas/perfil/password "$TA" '{"actual":"Equivocada1","nueva":"OtraClave99"}'
verificar "con la contrasena actual incorrecta no cambia" 400 "'actual' in d['detail']"
llamar PUT /api/cuentas/perfil/password "$TA" '{"actual":"ClaveSegura1","nueva":"corta"}'
verificar "la nueva debe tener al menos 8 caracteres" 400
llamar PUT /api/cuentas/perfil/password "$TA" '{"actual":"ClaveSegura1","nueva":"OtraClave99"}'
verificar "cambia con la actual correcta" 204
comprobar "la clave anterior ya no sirve para entrar" '[ -z "$(tok ana@demo.co ClaveSegura1)" ]'
TA2=$(tok ana@demo.co OtraClave99)
comprobar "la nueva si" '[ -n "$TA2" ]'
llamar PUT /api/cuentas/perfil/password "$TA2" '{"actual":"OtraClave99","nueva":"ClaveSegura1"}'
verificar "se restaura la clave demo" 204

echo "=== Imagenes de la galeria (RF019) ==="
subir POST /api/oferta/prestador/imagenes "$TP" foto.png image/png
verificar "el prestador sube un PNG y recibe su URL" 201 "d['url'].startswith('/api/oferta/imagenes/') and d['tipo']=='image/png'"
URL1=$(campo "d['url']")
subir POST /api/oferta/prestador/imagenes "$TP" foto.jpg image/jpeg
verificar "y un JPEG" 201 "d['tipo']=='image/jpeg'"
URL2=$(campo "d['url']")
subir POST /api/oferta/prestador/imagenes "$TA" foto.png image/png
verificar "una turista no sube imagenes de servicios" 403
subir POST /api/oferta/prestador/imagenes "$TP" animada.gif image/gif
verificar "un GIF se rechaza" 400
subir POST /api/oferta/prestador/imagenes "$TP" grande.jpg image/jpeg
verificar "mas de 2 MB se rechaza" 413
H=$(cabeceras "$URL1")
comprobar "la imagen es publica, sin sesion, con cache de un ano" 'echo "$H" | grep -q "200" && echo "$H" | grep -qi "immutable"'

servicio() { # IMAGENES_JSON → cuerpo del servicio de prueba con esa galería
  echo "{\"tipo\":\"ACTIVIDAD\",\"nombre\":\"Galeria E2E $RUN\",\"modalidad\":\"RESERVA\",\"precioBase\":50000,\"direccion\":\"Taganga\",\"zona\":\"Taganga\",\"categorias\":[\"PLAYA\"],\"actividad\":{},\"imagenes\":$1}"
}
IMG1="{\"url\":\"$URL1\",\"descripcion\":\"Bahia de Taganga\"}"
IMG2="{\"url\":\"$URL2\",\"descripcion\":\"Lancha\"}"
EXT="{\"url\":\"https://images.unsplash.com/photo-1507525428034-b723cf961d3e\",\"descripcion\":\"Playa\"}"
llamar POST /api/oferta/prestador/servicios "$TP" "$(servicio "[$IMG1,$IMG2,$EXT]")"
verificar "registra un servicio con dos imagenes subidas y un enlace externo" 201 "len(d['imagenes'])==3 and d['imagenes'][0]['url']=='$URL1'"
SG=$(campo "d['id']")
llamar POST /api/oferta/prestador/servicios "$TP" "$(servicio '[{"url":"/api/oferta/imagenes/abc","descripcion":"x"}]')"
verificar "una URL que no es http(s) ni una imagen subida se rechaza" 400
llamar PUT "/api/oferta/prestador/servicios/$SG" "$TP" "$(servicio "[$IMG2,$EXT]")"
verificar "al editar se quita la primera imagen de la galeria" 200 "len(d['imagenes'])==2"
COD=$(curl -s -o /dev/null -w "%{http_code}" "$G$URL1")
comprobar "y su archivo se elimina (404)" '[ "$COD" = 404 ]'
COD=$(curl -s -o /dev/null -w "%{http_code}" "$G$URL2")
comprobar "la que sigue en la galeria sigue disponible" '[ "$COD" = 200 ]'

echo "=== Filtro por dieta (RF079) ==="
REST="{\"tipo\":\"GASTRONOMICO\",\"nombre\":\"Cocina Dieta E2E $RUN\",\"modalidad\":\"WALK_IN\",\"precioBase\":40000,\"direccion\":\"Centro\",\"zona\":\"Centro\",\"categorias\":[\"GASTRONOMIA\"],\"gastronomico\":{\"dietas\":[\"KOSHER\",\"CELIACA\"]}}"
llamar POST /api/oferta/prestador/servicios "$TP" "$REST"
verificar "un restaurante declara dietas kosher y celiaca" 201
SR=$(campo "d['id']")
llamar GET "/api/oferta/servicios?q=Dieta%20E2E%20$RUN&dieta=KOSHER" ""
verificar "filtrar por kosher lo encuentra" 200 "d['totalElementos']==1"
llamar GET "/api/oferta/servicios?q=Dieta%20E2E%20$RUN&dieta=HALAL" ""
verificar "filtrar por halal no" 200 "d['totalElementos']==0"
llamar GET "/api/oferta/servicios?q=E2E%20$RUN&dieta=celiaca" ""
verificar "sin importar mayusculas, y excluye lo que no es restaurante" 200 "d['totalElementos']==1 and d['contenido'][0]['id']==$SR"
llamar GET "/api/oferta/servicios?dieta=PALEO" ""
verificar "una dieta que no existe da 400" 400

echo "=== Notificaciones internas (RF037, RF039) ==="
llamar GET /api/reservas/notificaciones "$TP"; ANTES_P=$(campo "d['noLeidas']")
llamar POST "/api/reservas/prestador/servicios/$SG/disponibilidad" "$TP" "{\"desde\":\"$(dia 4)\",\"hasta\":\"$(dia 4)\",\"cupos\":5}"
llamar GET "/api/reservas/disponibilidad?servicio=$SG&desde=$(dia 4)&hasta=$(dia 4)" ""; DISP=$(campo "d[0]['id']")
llamar POST /api/reservas "$TA" "{\"idDisponibilidad\":$DISP,\"personas\":1}"; RID=$(campo "d['id']")
llamar GET /api/reservas/notificaciones "$TP"
verificar "al reservar, el prestador recibe la alerta (RF037)" 200 "d['noLeidas']==$ANTES_P+1 and d['recientes'][0]['asunto']=='Nueva solicitud de reserva' and '#$RID' in d['recientes'][0]['mensaje']"
llamar POST "/api/reservas/$RID/pagos" "$TA" '{"medio":"PASARELA"}'
llamar POST "/api/reservas/prestador/reservas/$RID/confirmar" "$TP"
llamar GET /api/reservas/notificaciones "$TA"
verificar "al confirmar, la turista recibe la alerta (RF039)" 200 "d['recientes'][0]['asunto']=='Reserva confirmada' and d['noLeidas']>=1"
NID=$(campo "d['recientes'][0]['id']")
llamar POST "/api/reservas/notificaciones/$NID/leida" "$TP"
verificar "nadie marca como leida una notificacion ajena" 404
llamar POST "/api/reservas/notificaciones/$NID/leida" "$TA"
verificar "la duena la marca como leida" 200 "[n['leida'] for n in d['recientes'] if n['id']==$NID]==[True]"
llamar POST "/api/reservas/$RID/cancelar" "$TA" '{"motivo":"Cambio de planes"}'
llamar GET /api/reservas/notificaciones "$TP"
verificar "si la turista cancela, el prestador recibe la alerta (RF037)" 200 "d['recientes'][0]['asunto']=='Reserva cancelada'"
llamar POST /api/reservas/notificaciones/leidas "$TP"
verificar "marcar todas como leidas deja el contador en 0" 200 "d['noLeidas']==0"
llamar GET /api/reservas/notificaciones ""
verificar "sin sesion no hay notificaciones" 401

echo "=== Documentacion Swagger (RNF-034) ==="
for s in cuentas oferta reservas; do
  llamar GET "/docs/$s/v3/api-docs" ""
  verificar "OpenAPI de $s publicado por el gateway, solo con rutas /api" 200 "len(d['paths'])>0 and all(p.startswith('/api/') for p in d['paths'])"
done
llamar GET /docs/reservas/v3/api-docs ""
verificar "el parametro interno 'actor' (identidad del gateway) no aparece" 200 "'actor' not in str(d['paths'])"
H=$(cabeceras /docs/oferta/swagger-ui/index.html)
comprobar "la interfaz de Swagger carga" 'echo "$H" | grep -q " 200"'
llamar GET /docs/oferta/v3/api-docs/swagger-config ""
verificar "Swagger arma sus enlaces con el prefijo /docs/oferta" 200 "d['url'].startswith('/docs/oferta/')"
COD=$(curl -s -o /dev/null -w "%{http_code}" "$G/docs/oferta/internal/servicios/1")
comprobar "por /docs no se puede llegar a /internal (404)" '[ "$COD" = 404 ]'

# Limpieza: los servicios de prueba salen del catálogo.
llamar PATCH "/api/oferta/prestador/servicios/$SG/estado" "$TP" '{"activo":false}'
llamar PATCH "/api/oferta/prestador/servicios/$SR/estado" "$TP" '{"activo":false}'
echo
echo "RESULTADO: $PASA correctas, $FALLA con fallas"
rm -rf "$TMP"
