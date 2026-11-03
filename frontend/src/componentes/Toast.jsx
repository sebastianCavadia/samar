import { useEffect } from 'react'
import { CircleCheck } from 'lucide-react'

// Confirmación breve que se anuncia a lectores de pantalla y desaparece sola.
export default function Toast({ mensaje, alCerrar }) {
  useEffect(() => {
    if (!mensaje) return undefined
    const t = setTimeout(alCerrar, 3500)
    return () => clearTimeout(t)
  }, [mensaje, alCerrar])

  return (
    <div role="status" aria-live="polite">
      {mensaje && <div className="toast"><CircleCheck size={18} aria-hidden="true" />{mensaje}</div>}
    </div>
  )
}
