import { Leaf, ShieldCheck, Sparkles } from 'lucide-react'
import Logo from './Logo'

// Marco de login y registro: panel visual a la izquierda (solo escritorio) y formulario a la derecha.
export default function PanelAcceso({ children }) {
  return (
    <div className="acceso">
      <div className="acceso-visual">
        <Logo tamano={52} />
        <div>
          <h2>Santa Marta, a tu manera</h2>
          <p>Un solo lugar para encontrar hospedajes, tours y restaurantes locales.</p>
        </div>
        <ul>
          <li><ShieldCheck size={20} aria-hidden="true" />Prestadores con RNT verificado por SAMAR.</li>
          <li><Sparkles size={20} aria-hidden="true" />Recomendaciones que te explican por qué te las sugerimos.</li>
          <li><Leaf size={20} aria-hidden="true" />Alternativas lejos de las zonas más congestionadas.</li>
        </ul>
      </div>
      <div className="acceso-form">{children}</div>
    </div>
  )
}
