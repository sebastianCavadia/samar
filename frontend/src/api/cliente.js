// Cliente HTTP único de la SPA: agrega el token y traduce errores del backend.
const CLAVE_TOKEN = 'samar.token'

export function guardarToken(token) {
  try {
    if (token) localStorage.setItem(CLAVE_TOKEN, token)
    else localStorage.removeItem(CLAVE_TOKEN)
  } catch {
    // almacenamiento no disponible (modo privado): la sesión dura lo que dure la pestaña
  }
}

function leerToken() {
  try {
    return localStorage.getItem(CLAVE_TOKEN)
  } catch {
    return null
  }
}

export async function api(ruta, { metodo = 'GET', cuerpo } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  const token = leerToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(ruta, {
    method: metodo,
    headers,
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  })
  const datos = res.status === 204 ? null : await res.json().catch(() => null)
  if (!res.ok) {
    // ProblemDetail (servicios) usa "detail"; el gateway usa "mensaje"
    throw new Error(datos?.detail || datos?.mensaje || `Error ${res.status}`)
  }
  return datos
}

export const cuentasApi = {
  login: (correo, password) =>
    api('/api/cuentas/auth/login', { metodo: 'POST', cuerpo: { correo, password } }),
  registrarTurista: (datos) =>
    api('/api/cuentas/auth/registro/turista', { metodo: 'POST', cuerpo: datos }),
  perfil: () => api('/api/cuentas/perfil'),
}

// Omite filtros vacíos para no mandar "?tipo=&zona=" al backend.
function aQuery(params) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor !== '' && valor !== null && valor !== undefined && valor !== false) q.set(clave, valor)
  })
  return q.toString()
}

export const ofertaApi = {
  buscar: (filtros) => api(`/api/oferta/servicios?${aQuery(filtros)}`),
  detalle: (id) => api(`/api/oferta/servicios/${id}`),
  misServicios: () => api('/api/oferta/prestador/servicios'),
  detallePropio: (id) => api(`/api/oferta/prestador/servicios/${id}`),
  crear: (datos) => api('/api/oferta/prestador/servicios', { metodo: 'POST', cuerpo: datos }),
  editar: (id, datos) => api(`/api/oferta/prestador/servicios/${id}`, { metodo: 'PUT', cuerpo: datos }),
  cambiarEstado: (id, activo) =>
    api(`/api/oferta/prestador/servicios/${id}/estado`, { metodo: 'PATCH', cuerpo: { activo } }),
}
