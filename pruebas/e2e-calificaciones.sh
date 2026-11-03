#!/usr/bin/env bash
# Prueba de punta a punta de las calificaciones (RF030, RF075, RF076, RF080, RF081 y ADR-11).
# Uso: bash pruebas/e2e-calificaciones.sh
# Las reservas pasan a COMPLETADA con la tarea automática (RF049). Para no esperar un minuto:
#   TAREA_MS=5000 docker compose up -d reservas-service
export PYTHONIOENCODING=utf-8
G=localhost:8080
J='Content-Type: application/json'
COMPOSE="$(cd "$(dirname "$0")/.." && pwd)/docker-compose.yml"
RUN=$(date +%s)
TMP=$(mktemp -d)
PASA=0; FALLA=0

llamar() { # METODO RUTA TOKEN [CUERPO]  → deja $COD y $RESP
  local args=(-s -o "$TMP/r" -w "%{http_code}" -X "$1" "$G$2" -H "$J")
  [ -n "$3" ] && args+=(-H "Authorization: Bearer $3")
  [ -n "$4" ] && args+=(--data-binary "$4")
  COD=$(curl "${args[@]}"); RESP=$(cat "$TMP/r")
}
campo() { echo "$RESP" | python -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1" 2>/dev/null; }
verificar() { # DESCRIPCION CODIGO_ESPERADO [EXPRESION_PYTHON_SOBRE_d]
  local ok=1
  [ "$COD" != "$2" ] && ok=0
  if [ $ok = 1 ] && [ -n "$3" ]; then [ "$(campo "$3")" = "True" ] || ok=0; fi
  if [ $ok = 1 ]; then PASA=$((PASA+1)); printf "  OK     %-70s %s\n" "$1" "$COD"
  else FALLA=$((FALLA+1)); printf "  FALLO  %-70s %s  %s\n" "$1" "$COD" "$(echo "$RESP" | head -c 220)"; fi
}
comprobar() { # DESCRIPCION CONDICION_BASH (para lo que se mide en la base, no en una respuesta)
  if eval "$2"; then PASA=$((PASA+1)); printf "  OK     %s\n" "$1"
  else FALLA=$((FALLA+1)); printf "  FALLO  %s\n" "$1"; fi
}
tok() { llamar POST /api/cuentas/auth/login "" "{\"correo\":\"$1\",\"password\":\"$2\"}"; campo "d['token']"; }
sqlr() { docker exec samar-db psql -U postgres -d samar_reservas -tAc "$1"; }
sqlo() { docker exec samar-db psql -U postgres -d samar_oferta -tAc "$1"; }
dia() { date -d "+$1 day" +%F; }
idDisp() { llamar GET "/api/reservas/disponibilidad?servicio=$1&desde=$2&hasta=$2" ""; campo "d[0]['id']"; }
calificar() { llamar POST "/api/reservas/$2/calificacion" "$1" "$3"; }
resenas() { llamar GET "/api/oferta/servicios/$S/calificaciones$1" ""; }

# Si oferta o reservas se acaban de reiniciar, Spring tarda unos segundos en aceptar peticiones.
for i in $(seq 1 60); do
  llamar GET /api/oferta/servicios ""; A=$COD
  llamar GET "/api/reservas/disponibilidad?servicio=1" ""; B=$COD
  [ "$A" = 200 ] && [ "$B" = 200 ] && break; sleep 2
done

TP=$(tok tours@demo.co ClaveSegura1)
TN=$(tok hostal@demo.co ClaveSegura1)
TA=$(tok ana@demo.co ClaveSegura1)
llamar POST /api/cuentas/auth/registro/turista "" "{\"nombre\":\"Marta Lopez\",\"correo\":\"marta$RUN@demo.co\",\"password\":\"ClaveSegura1\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"M$RUN\",\"nacionalidad\":\"Colombiana\",\"paisResidencia\":\"Colombia\",\"fechaNacimiento\":\"1990-02-14\"}"
TM=$(tok "marta$RUN@demo.co" ClaveSegura1)

# Servicio propio de esta corrida (al final se desactiva para no ensuciar el catálogo de la demo).
llamar POST /api/oferta/prestador/servicios "$TP" "{\"tipo\":\"ACTIVIDAD\",\"nombre\":\"Avistamiento Calificaciones E2E $RUN\",\"modalidad\":\"RESERVA\",\"precioBase\":120000,\"direccion\":\"Minca\",\"zona\":\"Minca\",\"categorias\":[\"NATURALEZA\"],\"actividad\":{}}"
S=$(campo "d['id']")
[ -z "$S" ] && { echo "No se pudo crear el servicio de prueba ($COD): $RESP"; exit 1; }
D=$(dia 3); D2=$(dia 5)
llamar POST "/api/reservas/prestador/servicios/$S/disponibilidad" "$TP" "{\"desde\":\"$D\",\"hasta\":\"$D2\",\"cupos\":30}"
DISP=$(idDisp $S $D); DISP2=$(idDisp $S $D2)

confirmada() { # TOKEN ID_DISPONIBILIDAD → id de una reserva pagada y confirmada
  llamar POST /api/reservas "$1" "{\"idDisponibilidad\":$2,\"personas\":1}"; local id; id=$(campo "d['id']")
  llamar POST "/api/reservas/$id/pagos" "$1" '{"medio":"PASARELA"}'
  llamar POST "/api/reservas/prestador/reservas/$id/confirmar" "$TP"
  echo "$id"
}
R1=$(confirmada "$TA" $DISP)
CONC=(); for i in $(seq 1 8); do CONC+=("$(confirmada "$TA" $DISP)"); done
RM=$(confirmada "$TM" $DISP)
RCAIDA=$(confirmada "$TA" $DISP)
RFUTURA=$(confirmada "$TA" $DISP2)
echo "  servicio $S; reservas: R1=$R1, concurrentes=${CONC[*]}, Marta=$RM, oferta-caido=$RCAIDA, futura=$RFUTURA"

echo "=== Solo con la reserva COMPLETADA (RF076) ==="
calificar "$TA" $R1 '{"puntuacion":5}'
verificar "una reserva CONFIRMADA todavia no se califica" 409 "'COMPLETADA' in d['detail']"
llamar GET "/api/reservas/$R1" "$TA"
verificar "y la reserva no ofrece calificar" 200 "d['puedeCalificar']==False and d['calificacion'] is None"

# El servicio "ya se prestó": la fecha se mueve al pasado y la tarea automática completa las reservas (RF049).
sqlr "update disponibilidad set fecha = current_date - 2 where id = $DISP" >/dev/null
echo "  fecha movida al pasado; esperando a que la tarea automatica complete las 11 reservas..."
for i in $(seq 1 45); do
  N=$(sqlr "select count(*) from reserva r join detalle_reserva d on d.id_reserva=r.id where d.id_disponibilidad=$DISP and r.estado='COMPLETADA'")
  [ "$N" = 11 ] && break; sleep 2
done
comprobar "las 11 reservas pasaron a COMPLETADA" '[ "$N" = 11 ]'

echo "=== Calificar (RF075) ==="
calificar "" $R1 '{"puntuacion":5}'
verificar "sin sesion no se califica" 401
calificar "$TA" $R1 '{"puntuacion":0}'
verificar "0 estrellas no es valido" 400
calificar "$TA" $R1 '{"puntuacion":6}'
verificar "6 estrellas tampoco" 400
calificar "$TA" $R1 '{"comentario":"sin estrellas"}'
verificar "las estrellas son obligatorias" 400
calificar "$TM" $R1 '{"puntuacion":1}'
verificar "otra turista no califica una reserva ajena" 403
calificar "$TP" $R1 '{"puntuacion":5}'
verificar "el prestador no califica su propio servicio" 403
calificar "$TA" $RFUTURA '{"puntuacion":5}'
verificar "una reserva con fecha futura sigue sin poder calificarse" 409
calificar "$TA" $R1 '{"puntuacion":5,"comentario":"  Excelente: el guia sabia muchisimo de aves  "}'
verificar "calificar con 5 estrellas y resena" 201 "d['calificacion']==5 and d['puedeCalificar']==False"
calificar "$TA" $R1 '{"puntuacion":1}'
verificar "una sola vez por reserva (RF076)" 409 "'Ya calificaste' in d['detail']"
llamar GET /api/reservas/mis "$TA"
verificar "en Mis reservas: la calificada muestra sus estrellas y las otras ofrecen calificar" 200 \
  "[r['calificacion'] for r in d if r['id']==$R1]==[5] and [r['puedeCalificar'] for r in d if r['id']==${CONC[0]}]==[True]"
resenas ""
verificar "promedio 5.0 con 1 resena; el autor sale abreviado y sin correo" 200 \
  "d['promedio']==5.0 and d['total']==1 and d['resenas']['contenido'][0]['autor']=='Ana T.' and d['resenas']['contenido'][0]['comentario'].startswith('Excelente')"

echo "=== Concurrencia: 8 calificaciones simultaneas del mismo servicio (RF080) ==="
PUNTOS=(5 4 4 3 5 2 1 4)
for i in "${!CONC[@]}"; do printf '%s %s\n' "${CONC[$i]}" "${PUNTOS[$i]}"; done > "$TMP/lote"
xargs -P 8 -L 1 sh -c 'curl -s -o /dev/null -w "%{http_code}\n" -X POST "'"$G"'/api/reservas/$0/calificacion" -H "'"$J"'" -H "Authorization: Bearer '"$TA"'" --data-binary "{\"puntuacion\":$1}"' < "$TMP/lote" > "$TMP/conc"
OK201=$(grep -c '^201$' "$TMP/conc")
resenas ""
echo "  respuestas: $OK201 de 8 con 201 | total=$(campo "d['total']") promedio=$(campo "d['promedio']")"
verificar "las 8 se registran y el promedio cuenta las 9: (5+5+4+4+3+5+2+1+4)/9 = 3.7" 200 \
  "$OK201==8 and d['total']==9 and d['promedio']==3.7"

echo "=== Doble clic: 5 envios simultaneos de la misma reserva ==="
seq 1 5 | xargs -P 5 -I{} curl -s -o /dev/null -w "%{http_code}\n" -X POST "$G/api/reservas/$RM/calificacion" -H "$J" \
  -H "Authorization: Bearer $TM" --data-binary '{"puntuacion":3,"comentario":"Bien, aunque empezo tarde"}' > "$TMP/doble"
D201=$(grep -c '^201$' "$TMP/doble"); D409=$(grep -c '^409$' "$TMP/doble")
resenas ""
echo "  respuestas: $D201 con 201, $D409 con 409 | total=$(campo "d['total']")"
verificar "exactamente una cuenta: 10 resenas y promedio 36/10 = 3.6" 200 "$D201==1 and $D409==4 and d['total']==10 and d['promedio']==3.6"

echo "=== Resenas publicas y filtro por estrellas (RF081) ==="
verificar "distribucion de 5 a 1 estrellas" 200 "[c['total'] for c in d['distribucion']]==[3,3,2,1,1]"
verificar "de 5 en 5, la mas reciente primero" 200 "d['resenas']['tamano']==5 and d['resenas']['totalPaginas']==2 and d['resenas']['contenido'][0]['autor']=='Marta L.'"
resenas "?estrellas=4"
verificar "clic en 4 estrellas: solo las de 4" 200 "d['resenas']['totalElementos']==3 and all(r['puntuacion']==4 for r in d['resenas']['contenido'])"
resenas "?estrellas=7"
verificar "filtro fuera de rango" 400
llamar GET "/api/oferta/servicios?q=Calificaciones%20E2E%20$RUN&calificacionMin=3.5" ""
verificar "el catalogo filtra por calificacion minima con el promedio real" 200 "d['totalElementos']==1 and d['contenido'][0]['calificacionPromedio']==3.6"
llamar GET "/api/oferta/servicios?q=Calificaciones%20E2E%20$RUN&calificacionMin=4" ""
verificar "y lo excluye si se pide 4 o mas" 200 "d['totalElementos']==0"

echo "=== Respuesta del prestador (RF030) ==="
llamar GET "/api/oferta/prestador/calificaciones?sinResponder=true" "$TP"
verificar "el prestador ve las resenas sin responder de sus servicios" 200 "d['sinResponder']>=10 and d['resenas']['contenido'][0]['nombreServicio'].endswith('$RUN')"
PEND=$(campo "d['sinResponder']"); CAL=$(campo "d['resenas']['contenido'][0]['id']")
llamar PUT "/api/oferta/prestador/calificaciones/$CAL/respuesta" "$TN" '{"respuesta":"No es mio"}'
verificar "otro prestador no responde resenas ajenas" 403
llamar PUT "/api/oferta/prestador/calificaciones/$CAL/respuesta" "$TA" '{"respuesta":"Hola"}'
verificar "un turista tampoco" 403
llamar PUT "/api/oferta/prestador/calificaciones/$CAL/respuesta" "$TP" '{"respuesta":"   "}'
verificar "respuesta vacia" 400
llamar PUT "/api/oferta/prestador/calificaciones/$CAL/respuesta" "$TP" '{"respuesta":"Gracias Marta, ya ajustamos la hora de salida"}'
verificar "el prestador responde" 200 "d['respuesta'].startswith('Gracias') and d['fechaRespuesta'] is not None"
resenas ""
verificar "la respuesta es publica" 200 "d['resenas']['contenido'][0]['respuesta'].startswith('Gracias')"
llamar GET "/api/oferta/prestador/calificaciones?sinResponder=true" "$TP"
verificar "y sale de las pendientes" 200 "d['sinResponder']==$PEND-1"

echo "=== Resiliencia e idempotencia (ADR-11) ==="
# Con oferta congelado la petición queda en su cola: reservas se rinde a los 3 s (503), pero al
# descongelarse oferta SÍ la procesa. Un timeout no es un fallo: por eso el registro es idempotente.
docker compose -f "$COMPOSE" pause oferta-service >/dev/null 2>&1
calificar "$TA" $RCAIDA '{"puntuacion":4}'
verificar "oferta congelado: 503 y la reserva no se marca como calificada" 503 "'dos veces' in d['detail']"
docker compose -f "$COMPOSE" unpause oferta-service >/dev/null 2>&1
llamar GET "/api/reservas/$RCAIDA" "$TA"
verificar "la reserva sigue ofreciendo calificar" 200 "d['puedeCalificar']==True"
sleep 2
ANTES=$(sqlo "select count(*) from calificacion where id_reserva=$RCAIDA")
calificar "$TA" $RCAIDA '{"puntuacion":4}'
echo "  calificaciones de esa reserva en oferta antes del reintento: $ANTES (1 = oferta la proceso al descongelarse)"
if [ "$ANTES" = 1 ]; then
  verificar "reintento: oferta ya la tenia, responde que ya habia quedado registrada" 409 "'quedado registrada con 4' in d['detail']"
else
  verificar "reintento: se registra ahora" 201 "d['calificacion']==4"
fi
llamar GET "/api/reservas/$RCAIDA" "$TA"
verificar "en ambos casos queda calificada una sola vez, con 4 estrellas" 200 "d['calificacion']==4 and d['puedeCalificar']==False"
UNA=$(sqlo "select count(*) from calificacion where id_reserva=$RCAIDA")
comprobar "y oferta tiene exactamente una calificacion de esa reserva" '[ "$UNA" = 1 ]'
# Reservas pierde su copia (como si se hubiera caído justo después de que oferta guardó).
sqlr "update reserva set calificacion = null, calificada_en = null where id = $R1" >/dev/null
calificar "$TA" $R1 '{"puntuacion":1}'
verificar "reintento tras perder la copia: 409, no se cuenta dos veces" 409 "'quedado registrada con 5' in d['detail']"
llamar GET "/api/reservas/$R1" "$TA"
verificar "y la copia se repara con la puntuacion original (5, no 1)" 200 "d['calificacion']==5"

echo "=== Invariantes en la base ==="
TOTAL=$(sqlo "select total_calificaciones || ' ' || calificacion_promedio from servicio_turistico where id=$S")
comprobar "oferta: 11 calificaciones y promedio 4/11 mas = 3.6 ($TOTAL)" '[ "$TOTAL" = "11 3.6" ]'
DESC_O=$(sqlo "select count(*) from servicio_turistico s where s.total_calificaciones <> (select count(*) from calificacion c where c.id_servicio=s.id) or s.calificacion_promedio is distinct from (select round(avg(puntuacion),1) from calificacion c where c.id_servicio=s.id)")
comprobar "en todos los servicios, total y promedio coinciden con las calificaciones (0 descuadres)" '[ "$DESC_O" = 0 ]'
CALIF_R=$(sqlr "select count(*) from reserva r join detalle_reserva d on d.id_reserva=r.id where d.id_servicio=$S and r.calificacion is not null")
comprobar "reservas: 11 reservas con su copia de la calificacion" '[ "$CALIF_R" = 11 ]'

llamar PATCH "/api/oferta/prestador/servicios/$S/estado" "$TP" '{"activo":false}'
resenas ""
verificar "al desactivar el servicio sus resenas dejan de ser publicas" 404
echo
echo "RESULTADO: $PASA correctas, $FALLA con fallas"
rm -rf "$TMP"
