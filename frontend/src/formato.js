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

export function fecha(iso) {
  return iso ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso)) : '-'
}
