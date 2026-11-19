// Prueba de carga (RNF-001, RNF-002 y RNF-004) con k6. No hace falta instalar nada:
//   docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6:0.54.0 run - < pruebas/carga-k6.js
// PAUSA (segundos entre clics de cada usuario, por defecto 1) permite probar un usuario más realista: -e PAUSA=3
// Dos escenarios al mismo tiempo durante ~1,5 minutos:
//   navegar:  200 usuarios simultáneos buscando, filtrando y viendo servicios
//   reservar:  20 usuarios simultáneos creando reservas sobre un servicio de prueba con muchos cupos
// Los umbrales son los de los RNF: si alguno no se cumple, k6 termina con error.
import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter } from 'k6/metrics'

const BASE = __ENV.BASE_URL || 'http://localhost:8080'
const PAUSA = Number(__ENV.PAUSA || 1)
const RESPUESTAS_RESERVA = new Counter('reservas_por_codigo')
const JSON_H = { headers: { 'Content-Type': 'application/json' } }
const RUN = Date.now()

export const options = {
  scenarios: {
    navegar: {
      executor: 'ramping-vus', exec: 'navegar', startVUs: 0,
      stages: [{ duration: '20s', target: 200 }, { duration: '60s', target: 200 }, { duration: '10s', target: 0 }],
    },
    reservar: {
      executor: 'constant-vus', exec: 'reservar', vus: 20, duration: '60s', startTime: '20s',
    },
  },
  thresholds: {
    'http_req_duration{escenario:navegar}': ['p(95)<3000'], // RNF-001: búsqueda y filtrado en máx. 3 s
    'http_req_duration{escenario:reservar}': ['p(95)<5000'], // RNF-002: crear reserva en máx. 5 s
    'http_req_failed{escenario:navegar}': ['rate<0.01'], // RNF-004: sin degradación con 200 usuarios
    'checks{escenario:reservar}': ['rate>0.99'],
    // Solo para que el resumen muestre cuántas reservas terminaron con cada código HTTP.
    'reservas_por_codigo{codigo:201}': ['count>=0'],
    'reservas_por_codigo{codigo:409}': ['count>=0'],
    'reservas_por_codigo{codigo:503}': ['count>=0'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
}

function login(correo, password) {
  const r = http.post(`${BASE}/api/cuentas/auth/login`, JSON.stringify({ correo, password }), JSON_H)
  return r.json('token')
}

function conToken(token, tags) {
  return { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }, tags }
}

// Prepara un servicio con 10.000 cupos y una turista propia de la prueba (para no llenar las cuentas demo).
export function setup() {
  const prestador = login('tours@demo.co', 'ClaveSegura1')
  if (!prestador) throw new Error('Faltan las cuentas demo: corre primero bash pruebas/seed-demo.sh')

  const servicio = http.post(`${BASE}/api/oferta/prestador/servicios`, JSON.stringify({
    tipo: 'ACTIVIDAD', nombre: `Carga k6 ${RUN}`, modalidad: 'RESERVA', precioBase: 10000,
    direccion: 'Rodadero', zona: 'Rodadero', categorias: ['PLAYA'], actividad: {},
  }), conToken(prestador)).json('id')

  const fecha = new Date(Date.now() + 10 * 86400000).toISOString().slice(0, 10)
  http.post(`${BASE}/api/reservas/prestador/servicios/${servicio}/disponibilidad`,
    JSON.stringify({ desde: fecha, hasta: fecha, cupos: 10000 }), conToken(prestador))
  const disponibilidad = http.get(`${BASE}/api/reservas/disponibilidad?servicio=${servicio}&desde=${fecha}&hasta=${fecha}`)
    .json('0.id')

  const correo = `carga${RUN}@demo.co`
  http.post(`${BASE}/api/cuentas/auth/registro/turista`, JSON.stringify({
    nombre: 'Turista Carga', correo, password: 'ClaveSegura1', tipoDocumento: 'CC', numeroDocumento: `K${RUN}`,
    nacionalidad: 'Colombiana', paisResidencia: 'Colombia', fechaNacimiento: '1990-01-01',
  }), JSON_H)

  return { prestador, servicio, disponibilidad, turista: login(correo, 'ClaveSegura1') }
}

const BUSQUEDAS = ['', 'playa', 'minca', 'tour', 'tayrona', 'cafe']
const TIPOS = ['', 'ACTIVIDAD', 'HOSPEDAJE', 'GASTRONOMICO']
const elegir = (lista) => lista[Math.floor(Math.random() * lista.length)]

// Un visitante: busca, filtra, abre un servicio y mira sus cupos y reseñas, con PAUSA segundos entre clics.
export function navegar() {
  const tags = { escenario: 'navegar' }
  const lista = http.get(`${BASE}/api/oferta/servicios?q=${elegir(BUSQUEDAS)}&tipo=${elegir(TIPOS)}&pagina=0`,
    { tags: { ...tags, nombre: 'buscar' } })
  check(lista, { 'catálogo 200': (r) => r.status === 200 })
  sleep(PAUSA)

  const servicios = lista.status === 200 ? lista.json('contenido') : []
  if (servicios && servicios.length > 0) {
    const id = elegir(servicios).id
    check(http.get(`${BASE}/api/oferta/servicios/${id}`, { tags: { ...tags, nombre: 'detalle' } }),
      { 'detalle 200': (r) => r.status === 200 })
    http.get(`${BASE}/api/reservas/disponibilidad?servicio=${id}`, { tags: { ...tags, nombre: 'cupos' } })
    http.get(`${BASE}/api/oferta/servicios/${id}/calificaciones`, { tags: { ...tags, nombre: 'resenas' } })
  }
  sleep(PAUSA)
}

// Una turista reservando una y otra vez (cada reserva ocupa un cupo de forma atómica, ADR-07).
export function reservar(datos) {
  const r = http.post(`${BASE}/api/reservas`, JSON.stringify({ idDisponibilidad: datos.disponibilidad, personas: 1 }),
    conToken(datos.turista, { escenario: 'reservar', nombre: 'crear reserva' }))
  check(r, { 'reserva creada 201': (x) => x.status === 201 }, { escenario: 'reservar' })
  RESPUESTAS_RESERVA.add(1, { codigo: String(r.status) })
  sleep(PAUSA / 2)
}

// El servicio de prueba sale del catálogo; sus reservas pendientes expiran solas (RF056).
export function teardown(datos) {
  http.patch(`${BASE}/api/oferta/prestador/servicios/${datos.servicio}/estado`, JSON.stringify({ activo: false }),
    conToken(datos.prestador))
}
