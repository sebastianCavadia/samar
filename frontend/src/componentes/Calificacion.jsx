import { Star } from 'lucide-react'

export default function Calificacion({ promedio, total }) {
  if (!promedio) {
    return <span className="calificacion">Sin reseñas todavía</span>
  }
  return (
    <span className="calificacion" aria-label={`Calificación ${promedio} de 5, ${total} reseñas`}>
      <Star size={15} aria-hidden="true" />
      <strong>{Number(promedio).toFixed(1)}</strong>
      <span>({total} {total === 1 ? 'reseña' : 'reseñas'})</span>
    </span>
  )
}
