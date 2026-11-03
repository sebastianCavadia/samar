import { History } from 'lucide-react'
import { ACCIONES_ADMIN, fecha } from '../../formato'

// RF097: acciones administrativas sobre un usuario, con quién, cuándo, área y motivo.
export default function HistorialAdmin({ eventos }) {
  return (
    <section className="pila" style={{ gap: 8 }} aria-label="Bitácora del usuario">
      <h3 className="titulo-seccion"><History size={16} aria-hidden="true" /> Bitácora</h3>
      {eventos.length === 0 && <p className="suave pequeno">Ningún administrador ha hecho cambios sobre esta cuenta.</p>}
      <ol className="linea-tiempo">
        {eventos.map((e) => (
          <li key={e.id}>
            <strong>{ACCIONES_ADMIN[e.accion]}</strong>
            <span className="suave pequeno">{fecha(e.fecha)} · {e.administrador}{e.areaResponsable && ` (${e.areaResponsable})`}</span>
            {e.detalle && <span className="pequeno">{e.detalle}</span>}
          </li>
        ))}
      </ol>
    </section>
  )
}
