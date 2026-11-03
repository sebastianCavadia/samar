import { Accessibility, ArrowRight, MapPin, Sparkles } from 'lucide-react'
import { CATEGORIAS, TIPOS, UNIDAD_PRECIO, precio } from '../formato'
import { ICONO_TIPO } from '../iconos'
import Calificacion from './Calificacion'

// Tarjeta del catálogo (mockup 4): foto con insignia "Nuevo", datos clave y precio.
export default function TarjetaServicio({ servicio: s, alAbrir }) {
  const IconoTipo = ICONO_TIPO[s.tipo]
  // En restaurantes "Gastronomía" repetiría la etiqueta del tipo: se muestra otra categoría si la hay.
  const categoria = s.categorias.find((c) => !(s.tipo === 'GASTRONOMICO' && c === 'GASTRONOMIA'))

  return (
    <a className="tarjeta-servicio" href={`#servicio-${s.id}`}
      onClick={(e) => { e.preventDefault(); alAbrir(s.id) }}>
      <div className="foto">
        {s.imagenPrincipal
          ? <img src={s.imagenPrincipal.url} alt={s.imagenPrincipal.descripcion} loading="lazy" />
          : <div className="sin-foto"><IconoTipo size={44} strokeWidth={1.5} aria-hidden="true" /></div>}
        {s.nuevo && (
          <div className="insignias-foto">
            <span className="insignia nuevo"><Sparkles size={12} aria-hidden="true" /> Nuevo</span>
          </div>
        )}
      </div>

      <div className="cuerpo-servicio">
        <div className="titulo-servicio">
          <h3>{s.nombre}</h3>
          <span className="insignia tipo">{TIPOS[s.tipo]}</span>
        </div>
        <Calificacion promedio={s.calificacionPromedio} total={s.totalCalificaciones} />
        <div className="caracteristicas">
          <span className="dato-icono"><MapPin size={15} aria-hidden="true" />{s.zona}, Santa Marta</span>
          {s.accesible && <span className="dato-icono"><Accessibility size={15} aria-hidden="true" />Accesible</span>}
          {categoria && <span className="dato-icono">{CATEGORIAS[categoria]}</span>}
        </div>
        <div className="pie-servicio">
          <span className="ver-detalles">Ver detalles <ArrowRight size={15} aria-hidden="true" /></span>
          <div className="precio">
            <strong>{precio(s.precioBase, s.moneda)}</strong>
            <small>{UNIDAD_PRECIO[s.tipo]}</small>
          </div>
        </div>
      </div>
    </a>
  )
}

export function TarjetaEsqueleto() {
  return (
    <div className="tarjeta-servicio" aria-hidden="true">
      <div className="foto esqueleto" />
      <div className="cuerpo-servicio">
        <div className="esqueleto" style={{ height: 18, width: '75%' }} />
        <div className="esqueleto" style={{ height: 14, width: '45%' }} />
        <div className="esqueleto" style={{ height: 14, width: '60%' }} />
        <div className="pie-servicio">
          <div className="esqueleto" style={{ height: 14, width: 90 }} />
          <div className="esqueleto" style={{ height: 22, width: 100 }} />
        </div>
      </div>
    </div>
  )
}
