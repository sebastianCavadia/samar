import { useEffect, useState } from 'react'
import { MessageSquareText, Star, Store, TriangleAlert } from 'lucide-react'
import { ofertaApi } from '../api/cliente'
import { fechaDia, iniciales } from '../formato'
import { Estrellas } from './Estrellas'
import Paginacion from './Paginacion'

/** Una reseña pública con la respuesta del prestador, si la hay (RF030). */
export function Resena({ resena: r, servicio }) {
  return (
    <article className="resena">
      <div className="cabecera-resena">
        <span className="avatar" aria-hidden="true">{iniciales(r.autor)}</span>
        <div className="pila" style={{ gap: 0, flex: 1, minWidth: 0 }}>
          <strong>{r.autor}</strong>
          <span className="suave pequeno">{servicio ? `${servicio} · ` : ''}{fechaDia(r.fecha)}</span>
        </div>
        <Estrellas valor={r.puntuacion} />
      </div>
      {r.comentario
        ? <p className="texto-resena">{r.comentario}</p>
        : <p className="suave pequeno">Calificó sin dejar comentario.</p>}
      {r.respuesta && (
        <div className="respuesta-prestador">
          <strong><Store size={15} aria-hidden="true" /> Respuesta del prestador</strong>
          <p>{r.respuesta}</p>
        </div>
      )}
    </article>
  )
}

/**
 * RF080 y RF081: promedio, cuántas reseñas hay de cada puntuación y la lista. Cada barra es un
 * botón que filtra la lista por esa puntuación; volver a pulsarla quita el filtro.
 */
export default function Resenas({ idServicio }) {
  const [estrellas, setEstrellas] = useState(null)
  const [pagina, setPagina] = useState(0)
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let vigente = true
    ofertaApi.calificaciones(idServicio, { estrellas, pagina })
      .then((d) => { if (vigente) { setDatos(d); setError(null) } })
      .catch((e) => { if (vigente) setError(e.message) })
    return () => { vigente = false }
  }, [idServicio, estrellas, pagina])

  function filtrar(n) {
    setPagina(0)
    setEstrellas((actual) => (actual === n ? null : n))
  }

  if (error) {
    return <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>
  }
  if (!datos) return <div className="esqueleto" style={{ height: 150, borderRadius: 14 }} aria-hidden="true" />

  if (datos.total === 0) {
    return (
      <div className="vacio" style={{ padding: '24px 16px' }}>
        <div className="icono-vacio"><MessageSquareText size={26} aria-hidden="true" /></div>
        <h3>Aún no hay reseñas</h3>
        <p className="pequeno">Solo pueden opinar turistas con una reserva completada de este servicio.</p>
      </div>
    )
  }

  const { contenido, totalPaginas } = datos.resenas
  return (
    <div className="pila">
      <div className="resumen-resenas">
        <div className="promedio-grande">
          <strong>{Number(datos.promedio).toFixed(1)}</strong>
          <Estrellas valor={datos.promedio} tamano={18} />
          <span className="suave pequeno">{datos.total} {datos.total === 1 ? 'reseña' : 'reseñas'}</span>
        </div>
        <div className="distribucion" role="group" aria-label="Filtrar reseñas por estrellas">
          {datos.distribucion.map((c) => (
            <button key={c.estrellas} className="barra-estrellas" aria-pressed={estrellas === c.estrellas}
              disabled={c.total === 0 && estrellas !== c.estrellas} onClick={() => filtrar(c.estrellas)}
              aria-label={`${c.estrellas} ${c.estrellas === 1 ? 'estrella' : 'estrellas'}: ${c.total} ${c.total === 1 ? 'reseña' : 'reseñas'}`}>
              <span className="etiqueta">{c.estrellas}<Star size={13} aria-hidden="true" /></span>
              <span className="pista" aria-hidden="true"><span style={{ width: `${(c.total / datos.total) * 100}%` }} /></span>
              <span className="cantidad" aria-hidden="true">{c.total}</span>
            </button>
          ))}
        </div>
      </div>

      {estrellas && (
        <div className="filtro-activo" role="status">
          <span>Solo reseñas de {estrellas} {estrellas === 1 ? 'estrella' : 'estrellas'} ({datos.resenas.totalElementos})</span>
          <button className="btn-enlace" onClick={() => filtrar(estrellas)}>Ver todas</button>
        </div>
      )}

      <div className="lista-resenas">
        {contenido.map((r) => <Resena key={r.id} resena={r} />)}
      </div>
      <Paginacion pagina={datos.resenas.pagina} totalPaginas={totalPaginas} alCambiar={setPagina} />
    </div>
  )
}
