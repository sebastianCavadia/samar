import { ChevronLeft, ChevronRight } from 'lucide-react'

// Números visibles: primera, última y las vecinas de la actual; el resto se resume con "…".
function paginasVisibles(total, actual) {
  const visibles = new Set([0, total - 1, actual - 1, actual, actual + 1])
  const lista = [...visibles].filter((p) => p >= 0 && p < total).sort((a, b) => a - b)
  return lista.flatMap((p, i) => (i > 0 && p - lista[i - 1] > 1 ? ['…', p] : [p]))
}

export default function Paginacion({ pagina, totalPaginas, alCambiar }) {
  if (totalPaginas <= 1) return null
  return (
    <nav className="paginacion" aria-label="Paginación de resultados">
      <button onClick={() => alCambiar(pagina - 1)} disabled={pagina === 0} aria-label="Página anterior">
        <ChevronLeft size={18} aria-hidden="true" />
      </button>
      {paginasVisibles(totalPaginas, pagina).map((p, i) => (p === '…'
        ? <span key={`e${i}`} className="elipsis" aria-hidden="true">…</span>
        : (
          <button key={p} onClick={() => alCambiar(p)} aria-current={p === pagina ? 'page' : undefined}
            aria-label={`Página ${p + 1}`}>
            {p + 1}
          </button>
        )))}
      <button onClick={() => alCambiar(pagina + 1)} disabled={pagina + 1 >= totalPaginas} aria-label="Página siguiente">
        <ChevronRight size={18} aria-hidden="true" />
      </button>
    </nav>
  )
}
