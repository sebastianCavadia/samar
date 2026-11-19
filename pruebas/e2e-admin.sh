#!/usr/bin/env bash
# Prueba de punta a punta del panel del administrador (RF091-RF100) y de la suspensión de ofertas (ADR-13).
# Uso: bash pruebas/e2e-admin.sh
# Para que la sincronización se vea rápido: SINCRONIZACION_MS=5000 docker compose up -d oferta-service
export PYTHONIOENCODING=utf-8
G=localhost:8080
J='Content-Type: application/json'
RUN=$(date +%s)
TMP=$(mktemp -d)
PASA=0; FALLA=0

llamar() {
  local args=(-s -o "$TMP/r" -w "%{http_code}" -X "$1" "$G$2" -H "$J")
  [ -n "$3" ] && args+=(-H "Authorization: Bearer $3")
  [ -n "$4" ] && args+=(--data-binary "$4")
  COD=$(curl "${args[@]}"); RESP=$(cat "$TMP/r")
}
campo() { echo "$RESP" | python -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1" 2>/dev/null; }
verificar() {
  local ok=1
  [ "$COD" != "$2" ] && ok=0
  if [ $ok = 1 ] && [ -n "$3" ]; then [ "$(campo "$3")" = "True" ] || ok=0; fi
  if [ $ok = 1 ]; then PASA=$((PASA+1)); printf "  OK     %-68s %s\n" "$1" "$COD"
  else FALLA=$((FALLA+1)); printf "  FALLO  %-68s %s  %s\n" "$1" "$COD" "$(echo "$RESP" | head -c 220)"; fi
}
tok() { llamar POST /api/cuentas/auth/login "" "{\"correo\":\"$1\",\"password\":\"$2\"}"; campo "d.get('token','')"; }
esperar_catalogo() { # esperar_catalogo EXPRESION  (hasta 30 s)
  for i in $(seq 1 15); do llamar GET "/api/oferta/servicios?q=tour%20minca" ""; [ "$(campo "$1")" = "True" ] && return; sleep 2; done
}

TA=$(tok admin@samar.co Admin12345)
TP=$(tok tours@demo.co ClaveSegura1)
TT=$(tok ana@demo.co ClaveSegura1)

echo "=== Permisos ==="
llamar GET /api/cuentas/admin/usuarios "$TP"
verificar "un prestador no entra al panel" 403
llamar GET /api/cuentas/admin/usuarios ""
verificar "sin sesion tampoco" 401
llamar GET /api/cuentas/admin/resumen "$TA"
verificar "resumen para el administrador" 200 "d['usuarios'] >= 4 and d['prestadoresPendientes'] >= 1"

echo "=== Validacion de prestadores (RF095, RF096) ==="
llamar POST /api/cuentas/auth/registro/prestador "" "{\"nombre\":\"Eco Lodge $RUN\",\"correo\":\"eco$RUN@demo.co\",\"password\":\"ClaveSegura1\",\"tipoPrestador\":\"HOTEL\",\"nit\":\"N$RUN\",\"rnt\":\"R$RUN\",\"direccion\":\"Via a Minca km 5\",\"zona\":\"Minca\"}"
verificar "registro de prestador desde el frontend (RF003)" 201 "d['estadoValidacion']=='PENDIENTE' and d['prestadorValidado']==False"
PE=$(campo "d['id']")
llamar GET "/api/cuentas/admin/prestadores?validacion=PENDIENTE" "$TA"
verificar "aparece en pendientes" 200 "any(p['id']==$PE for p in d)"
llamar POST "/api/cuentas/admin/prestadores/$PE/rechazar" "$TA" '{"motivo":""}'
verificar "rechazar sin motivo" 400
llamar POST "/api/cuentas/admin/prestadores/$PE/rechazar" "$TA" '{"motivo":"El RNT no corresponde al NIT"}'
verificar "rechazar con motivo" 200 "d['estadoValidacion']=='RECHAZADO' and d['motivoRechazo']=='El RNT no corresponde al NIT'"
llamar GET "/api/cuentas/admin/prestadores?validacion=PENDIENTE" "$TA"
verificar "ya no esta en pendientes" 200 "not any(p['id']==$PE for p in d)"
TE=$(tok "eco$RUN@demo.co" ClaveSegura1)
llamar GET /api/cuentas/perfil "$TE"
verificar "el prestador ve el rechazo y el motivo en su perfil" 200 "d['estadoValidacion']=='RECHAZADO' and 'RNT' in d['motivoRechazo']"
llamar POST "/api/cuentas/admin/prestadores/$PE/rechazar" "$TA" '{"motivo":"otra vez"}'
verificar "no se rechaza dos veces" 409
llamar POST "/api/cuentas/admin/prestadores/$PE/validar" "$TA"
verificar "el administrador reconsidera y lo valida" 200 "d['estadoValidacion']=='VALIDADO' and d['motivoRechazo'] is None"

echo "=== Buscar y ficha (RF091, RF092, RF013) ==="
llamar GET "/api/cuentas/admin/usuarios?q=ANA" "$TA"
verificar "buscar por nombre sin importar mayusculas" 200 "any(u['correo']=='ana@demo.co' for u in d['contenido'])"
llamar GET "/api/cuentas/admin/usuarios?rol=PRESTADOR" "$TA"
verificar "filtrar por rol" 200 "len(d['contenido'])>0 and all(u['rol']=='PRESTADOR' for u in d['contenido'])"
llamar GET "/api/cuentas/admin/usuarios" "$TA"
verificar "paginado de 10 en 10" 200 "d['tamano']==10 and len(d['contenido'])<=10"
llamar GET "/api/cuentas/admin/usuarios?rol=JEFE" "$TA"
verificar "rol inexistente" 400
llamar GET "/api/cuentas/admin/usuarios?q=ana@demo.co" "$TA"; ANA=$(campo "d['contenido'][0]['id']")
llamar GET "/api/cuentas/admin/usuarios/$ANA" "$TA"
verificar "ficha del turista con documento y ultimo acceso" 200 "d['turista']['numeroDocumento']=='1098765432' and d['ultimoAcceso'] is not None"

echo "=== Estado y edicion (RF093, RF094, RF100) ==="
llamar GET /api/cuentas/perfil "$TA"; YO=$(campo "d['id']")
llamar POST "/api/cuentas/admin/usuarios/$YO/estado" "$TA" '{"estado":"INACTIVO","motivo":"prueba"}'
verificar "no puede desactivarse a si mismo" 409
llamar POST "/api/cuentas/admin/usuarios/$ANA/estado" "$TA" '{"estado":"BLOQUEADO"}'
verificar "bloquear sin motivo" 400
llamar POST "/api/cuentas/admin/usuarios/$ANA/estado" "$TA" '{"estado":"BLOQUEADO","motivo":"Publico resenas falsas"}'
verificar "bloquear con motivo" 200 "d['estado']=='BLOQUEADO' and d['historial'][0]['accion']=='CAMBIAR_ESTADO'"
llamar POST /api/cuentas/auth/login "" '{"correo":"ana@demo.co","password":"ClaveSegura1"}'
verificar "la cuenta bloqueada no inicia sesion (RF006)" 403
llamar POST "/api/cuentas/admin/usuarios/$ANA/estado" "$TA" '{"estado":"ACTIVO","motivo":"Revisado con la usuaria"}'
verificar "reactivar" 200 "d['estado']=='ACTIVO'"
llamar POST /api/cuentas/auth/login "" '{"correo":"ana@demo.co","password":"ClaveSegura1"}'
verificar "vuelve a iniciar sesion" 200
llamar PUT "/api/cuentas/admin/usuarios/$ANA" "$TA" '{"nombre":"Ana Maria Turista","telefono":"3001234567"}'
verificar "editar datos queda en la bitacora (RF094)" 200 "d['nombre']=='Ana Maria Turista' and d['historial'][0]['accion']=='EDITAR_DATOS'"
llamar PUT "/api/cuentas/admin/usuarios/$ANA" "$TA" '{"nombre":"Ana Turista","telefono":"3001234567"}'

echo "=== Nuevos administradores y bitacora (RF093, RF097, RF099) ==="
llamar POST /api/cuentas/admin/administradores "$TA" "{\"nombre\":\"Admin Soporte\",\"correo\":\"soporte$RUN@samar.co\",\"password\":\"ClaveSegura1\",\"areaResponsable\":\"Soporte\"}"
verificar "crear administrador con area" 201 "d['rol']=='ADMINISTRADOR' and d['areaResponsable']=='Soporte'"
TS=$(tok "soporte$RUN@samar.co" ClaveSegura1)
llamar GET /api/cuentas/perfil "$TS"
verificar "el nuevo administrador ve su area en el perfil" 200 "d['areaResponsable']=='Soporte'"
llamar POST /api/cuentas/admin/administradores "$TA" '{"nombre":"X","correo":"ana@demo.co","password":"ClaveSegura1","areaResponsable":"X"}'
verificar "correo repetido" 409
llamar GET /api/cuentas/admin/bitacora "$TA"
verificar "bitacora general con area y motivo" 200 "len(d['contenido'])>=5 and any('resenas falsas' in (e['detalle'] or '') for e in d['contenido']) and all(e['areaResponsable'] for e in d['contenido'])"

echo "=== Prestador desactivado: sus ofertas salen del catalogo (ADR-13) ==="
llamar GET /api/cuentas/admin/usuarios?q=tours@demo.co "$TA"; TOURS=$(campo "d['contenido'][0]['id']")
llamar GET "/api/oferta/servicios?q=tour%20minca" ""; ANTES=$(campo "d['totalElementos']")
# Se abre una fecha propia: las del seed pueden haber pasado, y sin fecha no habría qué intentar reservar.
FECHA=$(date -d "+6 day" +%F)
llamar POST /api/reservas/prestador/servicios/2/disponibilidad "$TP" "{\"desde\":\"$FECHA\",\"hasta\":\"$FECHA\",\"cupos\":5}"
DISP=$(llamar GET "/api/reservas/disponibilidad?servicio=2&desde=$FECHA&hasta=$FECHA" ""; campo "d[0]['id']")
llamar POST "/api/cuentas/admin/usuarios/$TOURS/estado" "$TA" '{"estado":"INACTIVO","motivo":"Documentacion vencida"}'
verificar "desactivar al prestador tours" 200
esperar_catalogo "d['totalElementos']==0"
verificar "en segundos sus $ANTES servicios desaparecen del catalogo" 200 "d['totalElementos']==0"
llamar GET /api/oferta/servicios/2 ""
verificar "el detalle publico ya no existe" 404
llamar POST /api/reservas "$TT" "{\"idDisponibilidad\":$DISP,\"personas\":1}"
verificar "y no se puede reservar" 409
llamar POST "/api/cuentas/admin/usuarios/$TOURS/estado" "$TA" '{"estado":"ACTIVO","motivo":"Documentacion al dia"}'
esperar_catalogo "d['totalElementos']==$ANTES"
verificar "al reactivarlo sus servicios vuelven" 200 "d['totalElementos']==$ANTES"

echo
echo "RESULTADO: $PASA correctas, $FALLA con fallas"
rm -rf "$TMP"
