import { useEffect, useState } from 'react'
import {
  Accessibility, ArrowLeft, ArrowRight, BedDouble, CalendarCheck, Check, ChefHat, ChevronRight, Clock,
  ExternalLink, Gauge, Leaf, MapPin, MessageSquareText, Salad, ShieldCheck, Sparkles, Store, Timer,
  TriangleAlert, UserRound, Users,
} from 'lucide-react'
import { ofertaApi } from '../api/cliente'
import Calificacion from '../componentes/Calificacion'
import { CATEGORIAS, DIETAS, DIFICULTADES, TIPOS, UNIDAD_PRECIO, hora, precio } from '../formato'
import { ICONO_CATEGORIA, ICONO_TIPO } from '../iconos'

// RF061: consejos de turismo responsable que acompañan cada servicio.
const CONSEJOS = [
  'Llévate tu basura de regreso: en playas y senderos no siempre hay canecas.',
  'Usa el agua con moderación; en temporada alta escasea en la ciudad.',
  'Respeta los territorios de las comunidades de la Sierra Nevada y sus normas.',
]

function datosDelTipo(s) {
  const lista = []
  if (s.hospedaje) {
    const h = s.hospedaje
    lista.push([Users, 'Capacidad', `${h.capacidadPersonas} ${h.capacidadPersonas === 1 ? 'persona' : 'personas'}`])
    lista.push([BedDouble, 'Habitación', h.tipoHabitacion])
    lista.push([Clock, 'Check-in', hora(h.horaCheckIn)])
    lista.push([Clock, 'Check-out', hora(h.horaCheckOut)])
  }
  if (s.actividad) {
    const a = s.actividad
    lista.push([Timer, 'Duración', a.duracionHoras && `${Number(a.duracionHoras)} horas`])
    lista.push([Gauge, 'Dificultad', DIFICULTADES[a.dificultad]])
    lista.push([UserRound, 'Edad mínima', a.edadMinima != null && `${a.edadMinima} años`])
    lista.push([Clock, 'Hora de inicio', hora(a.horaInicio)])
  }
  if (s.gastronomico) {
    const g = s.gastronomico
    lista.push([ChefHat, 'Cocina', g.tipoComida])
    lista.push([Users, 'Aforo', g.aforo && `${g.aforo} personas`])
    lista.push([Clock, 'Horario', g.horaApertura && `${hora(g.horaApertura)} - ${hora(g.horaCierre) ?? '?'}`])
    lista.push([Salad, 'Dietas', g.dietas.length > 0 && g.dietas.map((d) => DIETAS[d]).join(', ')])
  }
  return lista.filter(([, , valor]) => valor)
}

function Galeria({ imagenes, tipo }) {
  const [actual, setActual] = useState(0)
  const IconoTipo = ICONO_TIPO[tipo]
  if (imagenes.length === 0) {
    return (
      <div className="galeria">
        <div className="principal foto"><div className="sin-foto"><IconoTipo size={64} strokeWidth={1.25} aria-hidden="true" /></div></div>
      </div>
    )
  }
  return (
    <div className={imagenes.length > 1 ? 'galeria con-miniaturas' : 'galeria'}>
      <div className="principal">
        <img src={imagenes[actual].url} alt={imagenes[actual].descripcion} />
        {imagenes.length > 1 && <span className="contador">{actual + 1} / {imagenes.length}</span>}
      </div>
      {imagenes.length > 1 && (
        <div className="miniaturas">
          {imagenes.slice(0, 4).map((img, i) => (
            <button key={img.url} aria-pressed={i === actual} aria-label={`Ver foto ${i + 1}: ${img.descripcion}`} onClick={() => setActual(i)}>
              <img src={img.url} alt="" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

// RF074: información completa del servicio (mockup 5).
export default function DetalleServicio({ id, volver }) {
  const [s, setS] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    window.scrollTo({ top: 0 })
    ofertaApi.detalle(id).then(setS).catch((e) => setError(e.message))
  }, [id])

  const mapa = s && `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${s.direccion}, ${s.zona}, Santa Marta`)}`

  return (
    <div className="pila-lg">
      <nav className="migas" aria-label="Ruta de navegación">
        <button onClick={volver}><ArrowLeft size={16} aria-hidden="true" /> Explorar</button>
        {s && (
          <>
            <ChevronRight size={14} aria-hidden="true" />
            <span>{TIPOS[s.tipo]}</span>
            <ChevronRight size={14} aria-hidden="true" />
            <span aria-current="page">{s.nombre}</span>
          </>
        )}
      </nav>

      {error && (
        <div className="mensaje error" role="alert">
          <TriangleAlert size={20} aria-hidden="true" />
          <span><strong>No pudimos mostrar este servicio</strong>{error}</span>
        </div>
      )}

      {!s && !error && (
        <div className="pila" aria-hidden="true">
          <div className="esqueleto" style={{ aspectRatio: '16 / 6', borderRadius: 14 }} />
          <div className="esqueleto" style={{ height: 32, width: '50%' }} />
          <div className="esqueleto" style={{ height: 18, width: '30%' }} />
        </div>
      )}

      {s && (
        <>
          <Galeria imagenes={s.imagenes} tipo={s.tipo} />

          <div className="layout-detalle">
            <div className="pila-lg">
              <div className="titulo-detalle pila" style={{ gap: 10 }}>
                <div className="chips">
                  <span className="insignia tipo">{TIPOS[s.tipo]}</span>
                  {s.nuevo && <span className="insignia nuevo"><Sparkles size={12} aria-hidden="true" /> Nuevo</span>}
                </div>
                <h1>{s.nombre}</h1>
                <Calificacion promedio={s.calificacionPromedio} total={s.totalCalificaciones} />
              </div>

              <div className="fila-caracteristicas">
                <span className="dato-icono"><MapPin size={18} aria-hidden="true" />{s.zona}, Santa Marta</span>
                <span className="dato-icono">
                  {s.modalidad === 'RESERVA'
                    ? <><CalendarCheck size={18} aria-hidden="true" />Con reserva previa</>
                    : <><Store size={18} aria-hidden="true" />Sin reserva</>}
                </span>
                {s.accesible && <span className="dato-icono"><Accessibility size={18} aria-hidden="true" />Accesible</span>}
                {s.categorias.map((c) => {
                  const Icono = ICONO_CATEGORIA[c]
                  return <span key={c} className="dato-icono"><Icono size={18} aria-hidden="true" />{CATEGORIAS[c]}</span>
                })}
              </div>

              {s.descripcion && (
                <section className="seccion-detalle">
                  <h2>Descripción</h2>
                  <p style={{ whiteSpace: 'pre-line' }}>{s.descripcion}</p>
                </section>
              )}

              {datosDelTipo(s).length > 0 && (
                <section className="seccion-detalle">
                  <h2>Detalles</h2>
                  <div className="datos">
                    {datosDelTipo(s).map(([Icono, etiqueta, valor]) => (
                      <div key={etiqueta} className="dato">
                        <Icono size={20} aria-hidden="true" />
                        <div><small>{etiqueta}</small><span>{valor}</span></div>
                      </div>
                    ))}
                  </div>
                </section>
              )}

              <section className="seccion-detalle">
                <h2>Ubicación</h2>
                <div className="dato" style={{ alignItems: 'center', flexWrap: 'wrap' }}>
                  <MapPin size={20} aria-hidden="true" />
                  <div style={{ flex: 1, minWidth: 180 }}><small>{s.zona}</small><span>{s.direccion}</span></div>
                  <a className="btn btn-secundario" href={mapa} target="_blank" rel="noopener noreferrer">
                    Ver en el mapa <ExternalLink size={15} aria-hidden="true" />
                  </a>
                </div>
              </section>

              <section className="seccion-detalle">
                <h2>Reseñas</h2>
                {s.totalCalificaciones > 0
                  ? <Calificacion promedio={s.calificacionPromedio} total={s.totalCalificaciones} />
                  : (
                    <div className="vacio" style={{ padding: '24px 16px' }}>
                      <div className="icono-vacio"><MessageSquareText size={26} aria-hidden="true" /></div>
                      <h3>Aún no hay reseñas</h3>
                      <p className="pequeno">Solo pueden opinar turistas con una reserva completada de este servicio.</p>
                    </div>
                  )}
              </section>
            </div>

            <aside className="lateral" aria-label="Precio y reserva">
              <div className="tarjeta tarjeta-precio pila">
                <div>
                  <span className="desde">Precio desde</span>
                  <div className="monto">{precio(s.precioBase, s.moneda)}</div>
                  <div className="unidad">{s.moneda} · {UNIDAD_PRECIO[s.tipo]}</div>
                </div>
                {s.modalidad === 'RESERVA' ? (
                  <>
                    <button className="btn btn-primario btn-grande btn-bloque" disabled aria-describedby="nota-reserva">
                      Reservar <ArrowRight size={18} aria-hidden="true" />
                    </button>
                    <p id="nota-reserva" className="suave pequeno">La reserva en línea estará disponible muy pronto.</p>
                  </>
                ) : (
                  <div className="mensaje info">
                    <Store size={18} aria-hidden="true" />
                    <span>No necesita reserva: llega directamente en su horario de atención.</span>
                  </div>
                )}
              </div>

              <div className="tarjeta bloque-info">
                <ShieldCheck size={24} aria-hidden="true" />
                <div>
                  <h3>Prestador verificado</h3>
                  <p>SAMAR revisó su NIT y su Registro Nacional de Turismo antes de permitirle publicar.</p>
                </div>
              </div>

              <div className="tarjeta pila" style={{ gap: 12 }}>
                <div className="bloque-info">
                  <Leaf size={24} aria-hidden="true" style={{ color: 'var(--exito)' }} />
                  <h3 style={{ marginTop: 2 }}>Viaja de forma responsable</h3>
                </div>
                <ul className="lista-consejos">
                  {CONSEJOS.map((c) => <li key={c}><Check size={16} aria-hidden="true" />{c}</li>)}
                </ul>
              </div>
            </aside>
          </div>
        </>
      )}
    </div>
  )
}
