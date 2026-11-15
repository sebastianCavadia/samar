#!/usr/bin/env bash
# Prueba de punta a punta de reservas-service a través del gateway.
# Uso: bash e2e-reservas.sh [fase]   (fase: todo | basico | concurrencia | resiliencia | tiempo)
export PYTHONIOENCODING=utf-8
G=localhost:8080
J='Content-Type: application/json'
COMPOSE="$(cd "$(dirname "$0")/.." && pwd)/docker-compose.yml"
FASE=${1:-todo}
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
  if [ $ok = 1 ]; then PASA=$((PASA+1)); printf "  OK     %-66s %s\n" "$1" "$COD"
  else FALLA=$((FALLA+1)); printf "  FALLO  %-66s %s  %s\n" "$1" "$COD" "$(echo "$RESP" | head -c 220)"; fi
}
tok() { llamar POST /api/cuentas/auth/login "" "{\"correo\":\"$1\",\"password\":\"$2\"}"; campo "d['token']"; }
sql() { docker exec samar-db psql -U postgres -d samar_reservas -tAc "$1"; }
dia() { date -d "+$1 day" +%F; }
idDisp() { llamar GET "/api/reservas/disponibilidad?servicio=$1&desde=$2&hasta=$2" ""; campo "d[0]['id']"; }

D1=$(dia 1); D2=$(dia 2); D3=$(dia 3); D5=$(dia 5); D6=$(dia 6)
TP=$(tok tours@demo.co ClaveSegura1)
TN=$(tok hostal@demo.co ClaveSegura1)
TL=$(tok laura@demo.co ClaveSegura1)

# Turistas nuevos (RF002 ahora exige nacionalidad, país y fecha de nacimiento)
llamar POST /api/cuentas/auth/registro/turista "" "{\"nombre\":\"Carlos Ruiz\",\"correo\":\"carlos$RUN@demo.co\",\"password\":\"ClaveSegura1\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"C$RUN\",\"nacionalidad\":\"Colombiana\",\"paisResidencia\":\"Colombia\",\"fechaNacimiento\":\"1995-04-10\"}"
verificar "registro de turista con datos completos (RF002)" 201
llamar POST /api/cuentas/auth/registro/turista "" "{\"nombre\":\"Sofia Menor\",\"correo\":\"sofia$RUN@demo.co\",\"password\":\"ClaveSegura1\",\"tipoDocumento\":\"PASAPORTE\",\"numeroDocumento\":\"S$RUN\",\"nacionalidad\":\"Colombiana\",\"paisResidencia\":\"Colombia\",\"fechaNacimiento\":\"2016-06-01\"}"
llamar POST /api/cuentas/auth/registro/turista "" "{\"nombre\":\"Sin Fecha\",\"correo\":\"x$RUN@demo.co\",\"password\":\"ClaveSegura1\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"X$RUN\"}"
verificar "registro sin nacionalidad/pais/fecha se rechaza (RF002)" 400
TC=$(tok "carlos$RUN@demo.co" ClaveSegura1)
TS=$(tok "sofia$RUN@demo.co" ClaveSegura1)

# Servicios propios de esta corrida: así la prueba se puede repetir sin arrastrar reservas de corridas
# anteriores. Al final se desactivan para que no aparezcan en el catálogo de la demo.
crear() { llamar POST /api/oferta/prestador/servicios "$TP" "$1"; campo "d['id']"; }
S_MINCA1=$(crear "{\"tipo\":\"ACTIVIDAD\",\"nombre\":\"Tour E2E $RUN\",\"modalidad\":\"RESERVA\",\"precioBase\":91000,\"direccion\":\"Minca\",\"zona\":\"Minca\",\"categorias\":[\"NATURALEZA\"],\"actividad\":{}}")
S_MINCA2=$(crear "{\"tipo\":\"ACTIVIDAD\",\"nombre\":\"Tour Concurrencia E2E $RUN\",\"modalidad\":\"RESERVA\",\"precioBase\":90000,\"direccion\":\"Minca\",\"zona\":\"Minca\",\"categorias\":[\"NATURALEZA\"],\"actividad\":{}}")
S_CAMINATA=$(crear "{\"tipo\":\"ACTIVIDAD\",\"nombre\":\"Caminata E2E $RUN\",\"modalidad\":\"RESERVA\",\"precioBase\":180000,\"direccion\":\"Tayrona\",\"zona\":\"Tayrona\",\"categorias\":[\"NATURALEZA\"],\"actividad\":{\"edadMinima\":12,\"horaInicio\":\"07:00\"}}")
S_HOTEL=$(crear "{\"tipo\":\"HOSPEDAJE\",\"nombre\":\"Hotel E2E $RUN\",\"modalidad\":\"RESERVA\",\"precioBase\":380000,\"direccion\":\"Cra 1\",\"zona\":\"Rodadero\",\"categorias\":[\"PLAYA\"],\"hospedaje\":{\"capacidadPersonas\":3}}")
S_MARISCOS=$(crear "{\"tipo\":\"GASTRONOMICO\",\"nombre\":\"Sin reserva E2E $RUN\",\"modalidad\":\"WALK_IN\",\"precioBase\":50000,\"direccion\":\"Taganga\",\"zona\":\"Taganga\",\"categorias\":[\"GASTRONOMIA\"],\"gastronomico\":{}}")
echo "  servicios de esta corrida: actividad=$S_MINCA1 concurrencia=$S_MINCA2 caminata=$S_CAMINATA hotel=$S_HOTEL sin-reserva=$S_MARISCOS"
# Fechas base para las fases que no pasan por la sección A (concurrencia, resiliencia, tiempo).
preparar_fechas() {
  llamar POST "/api/reservas/prestador/servicios/$S_MINCA1/disponibilidad" "$TP" "{\"desde\":\"$D1\",\"hasta\":\"$(dia 14)\",\"cupos\":8}"
  llamar POST "/api/reservas/prestador/servicios/$S_CAMINATA/disponibilidad" "$TP" "{\"desde\":\"$D1\",\"hasta\":\"$(dia 30)\",\"cupos\":10,\"hora\":\"07:00\",\"diasSemana\":[\"SATURDAY\",\"SUNDAY\"]}"
}
[ "$FASE" != todo ] && [ "$FASE" != basico ] && preparar_fechas

if [ "$FASE" = todo ] || [ "$FASE" = basico ]; then
echo "=== A. Disponibilidad (RF018, RF034) ==="
llamar POST "/api/reservas/prestador/servicios/$S_MINCA1/disponibilidad" "$TP" "{\"desde\":\"$D1\",\"hasta\":\"$(dia 14)\",\"cupos\":5}"
verificar "prestador abre 14 dias con 5 cupos" 200 "d['creadas']+d['actualizadas']==14"
llamar POST "/api/reservas/prestador/servicios/$S_MINCA1/disponibilidad" "$TP" "{\"desde\":\"$D1\",\"hasta\":\"$(dia 14)\",\"cupos\":8}"
verificar "reabrir el mismo rango actualiza a 8 cupos" 200 "d['actualizadas']==14 and d['creadas']==0"
llamar POST "/api/reservas/prestador/servicios/$S_CAMINATA/disponibilidad" "$TP" "{\"desde\":\"$D1\",\"hasta\":\"$(dia 30)\",\"cupos\":10,\"hora\":\"07:00\",\"diasSemana\":[\"SATURDAY\",\"SUNDAY\"]}"
verificar "abrir solo fines de semana a las 07:00" 200 "0 < d['creadas']+d['actualizadas'] <= 10"
llamar POST "/api/reservas/prestador/servicios/$S_HOTEL/disponibilidad" "$TP" "{\"desde\":\"$D1\",\"hasta\":\"$(dia 10)\",\"cupos\":2}"
verificar "hotel abre 2 habitaciones por noche" 200
llamar POST "/api/reservas/prestador/servicios/$S_MARISCOS/disponibilidad" "$TP" "{\"desde\":\"$D1\",\"hasta\":\"$D2\",\"cupos\":5}"
verificar "servicio sin reserva (WALK_IN) no maneja cupos" 409
llamar POST "/api/reservas/prestador/servicios/$S_MINCA1/disponibilidad" "$TN" "{\"desde\":\"$D1\",\"hasta\":\"$D2\",\"cupos\":5}"
verificar "otro prestador no abre fechas de un servicio ajeno" 403
llamar POST "/api/reservas/prestador/servicios/$S_MINCA1/disponibilidad" "$TC" "{\"desde\":\"$D1\",\"hasta\":\"$D2\",\"cupos\":5}"
verificar "un turista no gestiona disponibilidad" 403
llamar POST "/api/reservas/prestador/servicios/$S_MINCA1/disponibilidad" "$TP" "{\"desde\":\"$(date -d '-2 day' +%F)\",\"hasta\":\"$D1\",\"cupos\":5}"
verificar "no se abren fechas pasadas (RF066)" 400
llamar POST "/api/reservas/prestador/servicios/$S_MINCA1/disponibilidad" "$TP" "{\"desde\":\"$D1\",\"hasta\":\"$(dia 200)\",\"cupos\":5}"
verificar "rango mayor a 120 dias se rechaza" 400
llamar GET "/api/reservas/disponibilidad?servicio=$S_MINCA1" ""
verificar "consulta publica sin token: 14 fechas con 8 libres" 200 "len(d)==14 and all(x['cuposLibres']==8 for x in d)"

echo "=== B. Reservar (RF036, RF047, RF064-RF066) ==="
DISP1=$(idDisp $S_MINCA1 $D1)
llamar POST /api/reservas "" "{\"idDisponibilidad\":$DISP1,\"personas\":1}"
verificar "reservar sin sesion" 401
llamar POST /api/reservas "$TP" "{\"idDisponibilidad\":$DISP1,\"personas\":1}"
verificar "un prestador no reserva" 403
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISP1,\"personas\":3}"
verificar "turista reserva 3 personas: PENDIENTE, total 3 x 91.000" 201 "d['estado']=='PENDIENTE' and float(d['total'])==273000 and d['expiraEn'] is not None and d['nombreTurista']=='Carlos Ruiz'"
R1=$(campo "d['id']")
llamar GET "/api/reservas/disponibilidad?servicio=$S_MINCA1&desde=$D1&hasta=$D1" ""
verificar "los cupos bajan de 8 a 5 (RF035)" 200 "d[0]['cuposLibres']==5"
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISP1,\"personas\":20}"
verificar "mas personas que cupos libres" 409
DISPC=$(llamar GET "/api/reservas/disponibilidad?servicio=$S_CAMINATA" ""; campo "d[0]['id']")
llamar POST /api/reservas "$TL" "{\"idDisponibilidad\":$DISPC,\"personas\":1}"
verificar "edad minima: perfil sin fecha de nacimiento" 409 "'fecha de nacimiento' in d['detail']"
llamar POST /api/reservas "$TS" "{\"idDisponibilidad\":$DISPC,\"personas\":1}"
verificar "edad minima: turista de 10 anios en actividad de 12+ (RF065)" 409 "'RF065' in d['detail']"
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISPC,\"personas\":2}"
verificar "adulto reserva la caminata con edad minima" 201
DISPH=$(idDisp $S_HOTEL $D2)
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISPH,\"personas\":4}"
verificar "hotel: mas personas que la capacidad (RF064)" 409 "'RF064' in d['detail']"
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISPH,\"personas\":3}"
verificar "hotel: 3 personas ocupan 1 habitacion, precio por noche" 201 "d['cantidad']==1 and d['personas']==3 and float(d['total'])==380000"
DISP2=$(idDisp $S_MINCA1 $D2)
llamar PATCH "/api/reservas/prestador/disponibilidad/$DISP2" "$TP" '{"activo":false}'
verificar "prestador bloquea una fecha" 200 "d['activo']==False"
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISP2,\"personas\":1}"
verificar "no se reserva una fecha bloqueada" 409
llamar GET "/api/reservas/disponibilidad?servicio=$S_MINCA1&desde=$D2&hasta=$D2" ""
verificar "la fecha bloqueada no aparece en la consulta publica" 200 "len(d)==0"
llamar PATCH "/api/reservas/prestador/disponibilidad/$DISP2" "$TP" '{"activo":true}'

echo "=== C. Pago en linea y confirmacion (RF043, RF044, RF038) ==="
llamar POST "/api/reservas/prestador/reservas/$R1/confirmar" "$TP"
verificar "confirmar sin pago aprobado (RF044)" 409 "'RF044' in d['detail']"
llamar POST "/api/reservas/$R1/pagos" "$TL" '{"medio":"PASARELA"}'
verificar "otro turista no paga una reserva ajena" 403
llamar POST "/api/reservas/$R1/pagos" "$TC" '{"medio":"PASARELA"}'
verificar "pago simulado aprobado al instante, sin plazo" 200 "d['pago']['estado']=='APROBADO' and d['pago']['referencia'].startswith('SIM-') and d['expiraEn'] is None"
llamar POST "/api/reservas/$R1/pagos" "$TC" '{"medio":"PASARELA"}'
verificar "no se paga dos veces" 409
llamar GET "/api/reservas/prestador/reservas?estado=PENDIENTE" "$TP"
verificar "el prestador la ve en recibidas pendientes" 200 "any(r['id']==$R1 for r in d)"
llamar POST "/api/reservas/prestador/reservas/$R1/confirmar" "$TN"
verificar "otro prestador no confirma" 403
llamar POST "/api/reservas/prestador/reservas/$R1/confirmar" "$TP"
verificar "prestador confirma la reserva pagada" 200 "d['estado']=='CONFIRMADA'"
llamar POST "/api/reservas/prestador/reservas/$R1/rechazar" "$TP" '{"motivo":"x"}'
verificar "lo confirmado ya no se rechaza" 409
llamar GET "/api/reservas/$R1" "$TL"
verificar "un tercero no ve la reserva" 403
llamar GET "/api/reservas/$R1" "$TC"
verificar "bitacora: creada y confirmada (RF057)" 200 "[h['estadoNuevo'] for h in d['historial']]==['PENDIENTE','CONFIRMADA']"

echo "=== D. Comprobante de transferencia (RF044) ==="
DISP3=$(idDisp $S_MINCA1 $D3)
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISP3,\"personas\":2}"; R3=$(campo "d['id']")
llamar POST "/api/reservas/$R3/pagos" "$TC" '{"medio":"COMPROBANTE"}'
verificar "comprobante sin numero" 400
llamar POST "/api/reservas/$R3/pagos" "$TC" "{\"medio\":\"COMPROBANTE\",\"referencia\":\"TRANSF-A$RUN\"}"
verificar "comprobante queda en revision" 200 "d['pago']['estado']=='PENDIENTE'"
llamar POST "/api/reservas/prestador/reservas/$R3/pago" "$TP" '{"aprobado":false}'
verificar "prestador rechaza el comprobante" 200 "d['pago']['estado']=='RECHAZADO' and d['estado']=='PENDIENTE'"
llamar POST "/api/reservas/$R3/pagos" "$TC" "{\"medio\":\"COMPROBANTE\",\"referencia\":\"TRANSF-A$RUN\"}"
verificar "no se reutiliza el numero de un comprobante" 409
llamar POST "/api/reservas/$R3/pagos" "$TC" "{\"medio\":\"COMPROBANTE\",\"referencia\":\"TRANSF-B$RUN\"}"
verificar "turista envia un comprobante nuevo" 200
llamar POST "/api/reservas/prestador/reservas/$R3/pago" "$TP" '{"aprobado":true}'
verificar "prestador lo aprueba" 200 "d['pago']['estado']=='APROBADO'"
llamar POST "/api/reservas/prestador/reservas/$R3/confirmar" "$TP"
verificar "y confirma" 200 "d['estado']=='CONFIRMADA'"

echo "=== E. Cancelar y rechazar (RF040, RF038, RF045) ==="
llamar POST "/api/reservas/$R3/cancelar" "$TC" '{"motivo":""}'
verificar "cancelar sin motivo" 400
llamar POST "/api/reservas/$R3/cancelar" "$TC" '{"motivo":"Cambio de planes"}'
verificar "turista cancela la confirmada: reembolso" 200 "d['estado']=='CANCELADA' and d['pago']['estado']=='REEMBOLSADO' and d['motivo']=='Cambio de planes'"
llamar GET "/api/reservas/disponibilidad?servicio=$S_MINCA1&desde=$D3&hasta=$D3" ""
verificar "los cupos vuelven a 8" 200 "d[0]['cuposLibres']==8"
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISP3,\"personas\":4}"; R4=$(campo "d['id']")
llamar POST "/api/reservas/prestador/reservas/$R4/rechazar" "$TP" '{"motivo":"No hay personal disponible"}'
verificar "prestador rechaza con motivo" 200 "d['estado']=='RECHAZADA' and d['motivo']=='No hay personal disponible'"
llamar POST "/api/reservas/$R4/cancelar" "$TC" '{"motivo":"x"}'
verificar "una rechazada ya no se cancela" 409
llamar GET "/api/reservas/disponibilidad?servicio=$S_MINCA1&desde=$D3&hasta=$D3" ""
verificar "los cupos vuelven a 8 tras el rechazo" 200 "d[0]['cuposLibres']==8"
llamar GET /api/reservas/mis "$TC"
verificar "historial del turista (RF041)" 200 "len(d)>=5 and {'PENDIENTE','CONFIRMADA','CANCELADA','RECHAZADA'} <= {r['estado'] for r in d}"
fi

if [ "$FASE" = todo ] || [ "$FASE" = concurrencia ]; then
echo "=== G. Concurrencia: 40 reservas al mismo tiempo sobre 5 cupos ==="
llamar POST "/api/reservas/prestador/servicios/$S_MINCA2/disponibilidad" "$TP" "{\"desde\":\"$D5\",\"hasta\":\"$D5\",\"cupos\":5}"
DISPX=$(idDisp $S_MINCA2 $D5)
ANTES=$(sql "select cupos_reservados from disponibilidad where id=$DISPX")
seq 1 40 | xargs -P 40 -I{} curl -s -o /dev/null -w "%{http_code}\n" -X POST "$G/api/reservas" -H "$J" \
  -H "Authorization: Bearer $TC" --data-binary "{\"idDisponibilidad\":$DISPX,\"personas\":1}" > "$TMP/conc"
OK201=$(grep -c '^201$' "$TMP/conc"); R409=$(grep -c '^409$' "$TMP/conc"); OTROS=$(grep -vcE '^(201|409)$' "$TMP/conc")
DESPUES=$(sql "select cupos_reservados || '/' || cupos_totales from disponibilidad where id=$DISPX")
FILAS=$(sql "select count(*) from detalle_reserva d join reserva r on r.id=d.id_reserva where d.id_disponibilidad=$DISPX and r.estado in ('PENDIENTE','CONFIRMADA','COMPLETADA')")
echo "  respuestas: $OK201 creadas (201), $R409 sin cupos (409), $OTROS otras  | cupos antes: $ANTES, despues: $DESPUES | reservas en BD: $FILAS"
COD=x; RESP='{}'
if [ "$OK201" = "$((5-ANTES))" ] && [ "$OTROS" = 0 ] && [ "$DESPUES" = "5/5" ] && [ "$FILAS" = "5" ]; then
  PASA=$((PASA+1)); echo "  OK     sin sobreventa: exactamente 5 reservas para 5 cupos"
else FALLA=$((FALLA+1)); echo "  FALLO  hubo sobreventa o errores inesperados"; fi
fi

if [ "$FASE" = todo ] || [ "$FASE" = resiliencia ]; then
echo "=== F. Resiliencia: servicios caidos ==="
DISP6=$(idDisp $S_MINCA1 $D6); DISPC=$(llamar GET "/api/reservas/disponibilidad?servicio=$S_CAMINATA" ""; campo "d[0]['id']")
docker compose -f "$COMPOSE" pause cuentas-service >/dev/null 2>&1
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISP6,\"personas\":1}"
verificar "cuentas caido: se sigue reservando sin edad minima (degradado)" 201 "d['nombreTurista'] is None"
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISPC,\"personas\":1}"
verificar "cuentas caido: con edad minima no se arriesga" 503
docker compose -f "$COMPOSE" unpause cuentas-service >/dev/null 2>&1
docker compose -f "$COMPOSE" pause oferta-service >/dev/null 2>&1
llamar GET "/api/reservas/disponibilidad?servicio=$S_MINCA1" ""
verificar "oferta caido: la consulta de cupos sigue funcionando" 200
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISP6,\"personas\":1}"
verificar "oferta caido: reservar responde 503 (no hay precio confiable)" 503
docker compose -f "$COMPOSE" unpause oferta-service >/dev/null 2>&1
fi

if [ "$FASE" = tiempo ]; then
echo "=== H. Procesos automaticos: expiracion (RF056) y completado (RF049) ==="
# Requiere reservas-service con EXPIRACION_SIN_PAGO_MIN=1 y TAREA_MS corto.
DISP6=$(idDisp $S_MINCA1 $D6)
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISP6,\"personas\":2}"; REXP=$(campo "d['id']")
LIBRES_ANTES=$(llamar GET "/api/reservas/disponibilidad?servicio=$S_MINCA1&desde=$D6&hasta=$D6" ""; campo "d[0]['cuposLibres']")
# Una reserva confirmada cuya fecha "ya pasó": se simula moviendo su fecha a ayer en la base de datos.
DP=$(dia $((20 + RUN % 90)))   # fecha propia de esta corrida (la anterior ya se movió a "ayer")
llamar POST "/api/reservas/prestador/servicios/$S_MINCA1/disponibilidad" "$TP" "{\"desde\":\"$DP\",\"hasta\":\"$DP\",\"cupos\":3}"
DISPP=$(idDisp $S_MINCA1 $DP)
llamar POST /api/reservas "$TC" "{\"idDisponibilidad\":$DISPP,\"personas\":1}"; RCOMP=$(campo "d['id']")
llamar POST "/api/reservas/$RCOMP/pagos" "$TC" '{"medio":"PASARELA"}'
llamar POST "/api/reservas/prestador/reservas/$RCOMP/confirmar" "$TP"
sql "update disponibilidad set fecha = current_date - (1 + $DISPP) where id = $DISPP" >/dev/null   # un día pasado distinto por corrida
echo "  reserva sin pagar #$REXP y confirmada con fecha pasada #$RCOMP; esperando a la tarea automatica..."
for i in $(seq 1 30); do
  E1=$(llamar GET "/api/reservas/$REXP" "$TC"; campo "d['estado']"); E2=$(llamar GET "/api/reservas/$RCOMP" "$TC"; campo "d['estado']")
  [ "$E1" = CANCELADA ] && [ "$E2" = COMPLETADA ] && break; sleep 5
done
llamar GET "/api/reservas/$REXP" "$TC"
verificar "sin pago tras el plazo: CANCELADA por expiracion (RF056)" 200 "d['estado']=='CANCELADA' and 'RF056' in d['motivo'] and d['historial'][-1]['idUsuario'] is None"
OCUPA=$(sql "select coalesce(sum(d.cantidad),0) from detalle_reserva d join reserva r on r.id=d.id_reserva where d.id_disponibilidad=$DISP6 and r.estado in ('PENDIENTE','CONFIRMADA','COMPLETADA')")
llamar GET "/api/reservas/disponibilidad?servicio=$S_MINCA1&desde=$D6&hasta=$D6" ""
verificar "sus cupos se liberan (libres = totales - reservas activas)" 200 "d[0]['cuposLibres']==8-$OCUPA and $OCUPA < 8-$LIBRES_ANTES"
llamar GET "/api/reservas/$RCOMP" "$TC"
verificar "confirmada con fecha pasada: COMPLETADA (RF049)" 200 "d['estado']=='COMPLETADA'"
fi

echo "=== Invariante en toda la base: cupos reservados = suma de reservas que ocupan cupo ==="
DESCUADRES=$(sql "select count(*) from disponibilidad disp where disp.cupos_reservados <> (select coalesce(sum(d.cantidad),0) from detalle_reserva d join reserva r on r.id=d.id_reserva where d.id_disponibilidad=disp.id and r.estado in ('PENDIENTE','CONFIRMADA','COMPLETADA'))")
TOTAL=$(sql "select count(*) || ' fechas, ' || coalesce(sum(cupos_reservados),0) || ' cupos reservados' from disponibilidad")
if [ "$DESCUADRES" = 0 ]; then PASA=$((PASA+1)); echo "  OK     $TOTAL, 0 descuadres"
else FALLA=$((FALLA+1)); echo "  FALLO  $DESCUADRES fechas con cupos descuadrados"; fi

echo
for s in $S_MINCA1 $S_MINCA2 $S_CAMINATA $S_HOTEL $S_MARISCOS; do   # no ensuciar el catálogo de la demo
  llamar PATCH "/api/oferta/prestador/servicios/$s/estado" "$TP" '{"activo":false}'
done
echo "RESULTADO: $PASA correctas, $FALLA con fallas"
rm -rf "$TMP"
