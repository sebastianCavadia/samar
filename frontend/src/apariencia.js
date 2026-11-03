// Preferencias visuales del visitante: tema oscuro y alto contraste (RF014).
// Se guardan en el navegador; si el almacenamiento no está disponible, solo duran la sesión.
const CLAVE = 'samar.apariencia'

export function leerApariencia() {
  let guardada = {}
  try {
    guardada = JSON.parse(localStorage.getItem(CLAVE)) || {}
  } catch {
    // sin almacenamiento: se usa la preferencia del sistema
  }
  const sistemaOscuro = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
  return {
    oscuro: guardada.oscuro ?? sistemaOscuro,
    altoContraste: guardada.altoContraste ?? false,
  }
}

export function aplicarApariencia({ oscuro, altoContraste }) {
  const raiz = document.documentElement
  raiz.dataset.tema = oscuro ? 'oscuro' : 'claro'
  if (altoContraste) raiz.dataset.contraste = 'alto'
  else delete raiz.dataset.contraste
}

export function guardarApariencia(apariencia) {
  aplicarApariencia(apariencia)
  try {
    localStorage.setItem(CLAVE, JSON.stringify(apariencia))
  } catch {
    // ignorado: la preferencia se pierde al recargar
  }
}
