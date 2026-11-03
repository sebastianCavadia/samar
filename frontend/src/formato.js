// Etiquetas y formatos compartidos por las páginas.
export const TIPOS = { HOSPEDAJE: 'Hospedaje', ACTIVIDAD: 'Actividad', GASTRONOMICO: 'Gastronomía' }

export const DESCRIPCION_TIPO = {
  HOSPEDAJE: 'Hoteles, hostales y apartamentos',
  ACTIVIDAD: 'Tours, caminatas y experiencias',
  GASTRONOMICO: 'Restaurantes y comida local',
}

export const CATEGORIAS = {
  PLAYA: 'Playa', NATURALEZA: 'Naturaleza', CULTURA: 'Cultura',
  AVENTURA: 'Aventura', GASTRONOMIA: 'Gastronomía', DESCANSO: 'Descanso',
}

export const DIETAS = { VEGETARIANA: 'Vegetariana', VEGANA: 'Vegana', CELIACA: 'Celíaca', KOSHER: 'Kosher', HALAL: 'Halal' }

export const DIFICULTADES = { BAJA: 'Baja', MEDIA: 'Media', ALTA: 'Alta' }

export const ROLES = { TURISTA: 'Turista', PRESTADOR: 'Prestador de servicio', ADMINISTRADOR: 'Administrador' }

// Cómo se cobra cada tipo (texto bajo el precio, como en los mockups).
export const UNIDAD_PRECIO = { HOSPEDAJE: 'por noche', ACTIVIDAD: 'por persona', GASTRONOMICO: 'precio promedio' }

export function precio(valor, moneda = 'COP') {
  return new Intl.NumberFormat('es-CO', { style: 'currency', currency: moneda, maximumFractionDigits: 0 })
    .format(valor)
}

export function hora(valor) {
  return valor ? valor.slice(0, 5) : null
}

export function iniciales(nombre = '') {
  return nombre.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('')
}

// RF048: estado → texto y estilo de la insignia
export const ESTADOS_RESERVA = {
  PENDIENTE: { texto: 'Pendiente', clase: 'aviso' },
  CONFIRMADA: { texto: 'Confirmada', clase: 'ok' },
  COMPLETADA: { texto: 'Completada', clase: 'tipo' },
  RECHAZADA: { texto: 'Rechazada', clase: 'error' },
  CANCELADA: { texto: 'Cancelada', clase: 'apagada' },
}

export const TIPOS_PRESTADOR = { HOTEL: 'Hotel u hospedaje', RESTAURANTE: 'Restaurante', AGENCIA_TURISTICA: 'Agencia turística' }

export const ESTADOS_CUENTA = {
  ACTIVO: { texto: 'Activa', clase: 'ok' },
  INACTIVO: { texto: 'Inactiva', clase: 'apagada' },
  BLOQUEADO: { texto: 'Bloqueada', clase: 'error' },
}

export const ESTADOS_VALIDACION = {
  PENDIENTE: { texto: 'Pendiente', clase: 'aviso' },
  VALIDADO: { texto: 'Validado', clase: 'ok' },
  RECHAZADO: { texto: 'Rechazado', clase: 'error' },
}

export const ACCIONES_ADMIN = {
  VALIDAR_PRESTADOR: 'Validó al prestador',
  RECHAZAR_PRESTADOR: 'Rechazó la solicitud',
  CAMBIAR_ESTADO: 'Cambió el estado',
  EDITAR_DATOS: 'Editó los datos',
  CREAR_ADMINISTRADOR: 'Creó la cuenta de administrador',
}

// RF040: el motivo se elige de una lista
export const MOTIVOS_TURISTA = ['Cambio de planes', 'Problemas de salud', 'Condiciones del clima', 'Encontré otra opción', 'Otro motivo']
export const MOTIVOS_PRESTADOR = ['Cierre por clima o fuerza mayor', 'Cierre del parque o zona', 'No hay personal disponible', 'Error en los cupos publicados', 'Otro motivo']

/** "2026-11-12" → "Mié, 12 nov 2026". Se arma en hora local para que no se corra un día por la zona horaria. */
export function fechaCorta(isoFecha) {
  const [a, m, d] = isoFecha.split('-').map(Number)
  const texto = new Intl.DateTimeFormat('es-CO', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(a, m - 1, d))
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

export function hoyIso() {
  const h = new Date()
  return `${h.getFullYear()}-${String(h.getMonth() + 1).padStart(2, '0')}-${String(h.getDate()).padStart(2, '0')}`
}

export function sumarDias(isoFecha, dias) {
  const [a, m, d] = isoFecha.split('-').map(Number)
  const f = new Date(a, m - 1, d + dias)
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-${String(f.getDate()).padStart(2, '0')}`
}

export function fecha(iso) {
  return iso ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso)) : '-'
}

/** Instante → "12 oct 2026", sin la hora (para reseñas). */
export function fechaDia(iso) {
  return new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(iso))
}

// RF075
export const TEXTO_ESTRELLAS = { 1: 'Muy malo', 2: 'Malo', 3: 'Regular', 4: 'Bueno', 5: 'Excelente' }
