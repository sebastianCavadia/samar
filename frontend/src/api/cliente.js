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

function cabeceras(extra = {}) {
  const token = leerToken()
  return token ? { ...extra, Authorization: `Bearer ${token}` } : extra
}

// Un FormData (archivos) viaja tal cual: el navegador pone el Content-Type multipart con su límite.
export async function api(ruta, { metodo = 'GET', cuerpo } = {}) {
  const esArchivo = cuerpo instanceof FormData
  const res = await fetch(ruta, {
    method: metodo,
    headers: cabeceras(esArchivo ? {} : { 'Content-Type': 'application/json' }),
    body: esArchivo ? cuerpo : cuerpo ? JSON.stringify(cuerpo) : undefined,
  })
  const datos = res.status === 204 ? null : await res.json().catch(() => null)
  if (!res.ok) {
    // ProblemDetail (servicios) usa "detail"; el gateway usa "mensaje"
    throw new Error(datos?.detail || datos?.mensaje || `Error ${res.status}`)
  }
  return datos
}

/** Descarga un archivo protegido (la foto de perfil) como blob; null si no existe. */
async function apiBlob(ruta) {
  const res = await fetch(ruta, { headers: cabeceras() })
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`Error ${res.status}`)
  return res.blob()
}

function conArchivo(archivo) {
  const datos = new FormData()
  datos.append('archivo', archivo)
  return datos
}

export const cuentasApi = {
  login: (correo, password) =>
    api('/api/cuentas/auth/login', { metodo: 'POST', cuerpo: { correo, password } }),
  registrarTurista: (datos) =>
    api('/api/cuentas/auth/registro/turista', { metodo: 'POST', cuerpo: datos }),
  registrarPrestador: (datos) =>
    api('/api/cuentas/auth/registro/prestador', { metodo: 'POST', cuerpo: datos }),
  perfil: () => api('/api/cuentas/perfil'),
  // RF008 y RF009
  editarPerfil: (datos) => api('/api/cuentas/perfil', { metodo: 'PUT', cuerpo: datos }),
  cambiarPassword: (actual, nueva) => api('/api/cuentas/perfil/password', { metodo: 'PUT', cuerpo: { actual, nueva } }),
  foto: () => apiBlob('/api/cuentas/perfil/foto'),
  subirFoto: (archivo) => api('/api/cuentas/perfil/foto', { metodo: 'PUT', cuerpo: conArchivo(archivo) }),
  eliminarFoto: () => api('/api/cuentas/perfil/foto', { metodo: 'DELETE' }),
}

// Panel del administrador (RF091-RF100)
export const adminApi = {
  resumen: () => api('/api/cuentas/admin/resumen'),
  prestadores: (validacion) => api(`/api/cuentas/admin/prestadores?${aQuery({ validacion })}`),
  validar: (id) => api(`/api/cuentas/admin/prestadores/${id}/validar`, { metodo: 'POST' }),
  rechazar: (id, motivo) => api(`/api/cuentas/admin/prestadores/${id}/rechazar`, { metodo: 'POST', cuerpo: { motivo } }),
  usuarios: (filtros) => api(`/api/cuentas/admin/usuarios?${aQuery(filtros)}`),
  usuario: (id) => api(`/api/cuentas/admin/usuarios/${id}`),
  cambiarEstado: (id, estado, motivo) =>
    api(`/api/cuentas/admin/usuarios/${id}/estado`, { metodo: 'POST', cuerpo: { estado, motivo } }),
  editar: (id, datos) => api(`/api/cuentas/admin/usuarios/${id}`, { metodo: 'PUT', cuerpo: datos }),
  crearAdministrador: (datos) => api('/api/cuentas/admin/administradores', { metodo: 'POST', cuerpo: datos }),
  bitacora: (pagina) => api(`/api/cuentas/admin/bitacora?pagina=${pagina}`),
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
  // RF019: devuelve { url } para agregarla a la galería del servicio
  subirImagen: (archivo) => api('/api/oferta/prestador/imagenes', { metodo: 'POST', cuerpo: conArchivo(archivo) }),

  // Reseñas: públicas (RF080, RF081) y las que recibe el prestador (RF030)
  calificaciones: (id, filtros) => api(`/api/oferta/servicios/${id}/calificaciones?${aQuery(filtros)}`),
  resenasRecibidas: (filtros) => api(`/api/oferta/prestador/calificaciones?${aQuery(filtros)}`),
  responder: (idCalificacion, respuesta) =>
    api(`/api/oferta/prestador/calificaciones/${idCalificacion}/respuesta`, { metodo: 'PUT', cuerpo: { respuesta } }),
}

export const reservasApi = {
  // Turista (la consulta de cupos es pública)
  disponibilidad: (idServicio, desde, hasta) =>
    api(`/api/reservas/disponibilidad?${aQuery({ servicio: idServicio, desde, hasta })}`),
  crear: (idDisponibilidad, personas) => api('/api/reservas', { metodo: 'POST', cuerpo: { idDisponibilidad, personas } }),
  mis: () => api('/api/reservas/mis'),
  pagar: (id, medio, referencia) => api(`/api/reservas/${id}/pagos`, { metodo: 'POST', cuerpo: { medio, referencia } }),
  cancelar: (id, motivo) => api(`/api/reservas/${id}/cancelar`, { metodo: 'POST', cuerpo: { motivo } }),
  // RF075/RF076: se califica a través de reservas, que comprueba que esté COMPLETADA (ADR-11)
  calificar: (id, puntuacion, comentario) =>
    api(`/api/reservas/${id}/calificacion`, { metodo: 'POST', cuerpo: { puntuacion, comentario } }),

  // Prestador
  recibidas: (estado) => api(`/api/reservas/prestador/reservas?${aQuery({ estado })}`),
  confirmar: (id) => api(`/api/reservas/prestador/reservas/${id}/confirmar`, { metodo: 'POST' }),
  rechazar: (id, motivo) => api(`/api/reservas/prestador/reservas/${id}/rechazar`, { metodo: 'POST', cuerpo: { motivo } }),
  validarPago: (id, aprobado) => api(`/api/reservas/prestador/reservas/${id}/pago`, { metodo: 'POST', cuerpo: { aprobado } }),
  fechasPrestador: (idServicio, desde, hasta) =>
    api(`/api/reservas/prestador/servicios/${idServicio}/disponibilidad?${aQuery({ desde, hasta })}`),
  abrirFechas: (idServicio, datos) =>
    api(`/api/reservas/prestador/servicios/${idServicio}/disponibilidad`, { metodo: 'POST', cuerpo: datos }),
  cambiarFecha: (id, activo) => api(`/api/reservas/prestador/disponibilidad/${id}`, { metodo: 'PATCH', cuerpo: { activo } }),

  // RF037 y RF039: alertas internas (la campana)
  notificaciones: () => api('/api/reservas/notificaciones'),
  marcarLeidas: () => api('/api/reservas/notificaciones/leidas', { metodo: 'POST' }),
  marcarLeida: (id) => api(`/api/reservas/notificaciones/${id}/leida`, { metodo: 'POST' }),
}
