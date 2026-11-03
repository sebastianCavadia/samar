import { useCallback, useEffect, useState } from 'react'
import { MessageSquareReply, MessageSquareText, Pencil, TriangleAlert } from 'lucide-react'
import { ofertaApi } from '../api/cliente'
import Paginacion from '../componentes/Paginacion'
import { Resena } from '../componentes/Resenas'
import Toast from '../componentes/Toast'
import { fecha } from '../formato'

const PESTANAS = [
  { id: 'pendientes', texto: 'Sin responder', sinResponder: true },
  { id: 'todas', texto: 'Todas', sinResponder: false },
]

function Responder({ resena, alGuardar }) {
  const [abierto, setAbierto] = useState(!resena.respuesta)
  const [texto, setTexto] = useState(resena.respuesta ?? '')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)

  if (!abierto) {
    return (
      <div className="fila-botones">
        <span className="suave pequeno">Respondiste el {fecha(resena.fechaRespuesta)}</span>
        <button className="btn btn-fantasma" onClick={() => setAbierto(true)}><Pencil size={15} aria-hidden="true" /> Editar respuesta</button>
      </div>
    )
  }

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      alGuardar(await ofertaApi.responder(resena.id, texto), resena.respuesta ? 'Respuesta actualizada' : 'Respuesta publicada')
      setAbierto(false)
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="pila" style={{ gap: 10 }} onSubmit={guardar}>
      <label className="campo">
        <span>Tu respuesta pública</span>
        <textarea className="control" rows={3} maxLength={1000} required value={texto} onChange={(e) => setTexto(e.target.value)}
          aria-label={`Tu respuesta pública a ${resena.autor} sobre ${resena.nombreServicio}`}
          placeholder="Agradece la visita o explica qué vas a mejorar. La verán todos los turistas." />
      </label>
      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}
      <div className="fila-botones">
        <button className="btn btn-primario" disabled={enviando || !texto.trim()}>
          <MessageSquareReply size={16} aria-hidden="true" /> {resena.respuesta ? 'Guardar cambios' : 'Publicar respuesta'}
        </button>
        {resena.respuesta && <button type="button" className="btn btn-fantasma" onClick={() => { setTexto(resena.respuesta); setAbierto(false) }}>Cancelar</button>}
      </div>
    </form>
  )
}

// RF030: el prestador lee las reseñas de todos sus servicios y responde en público.
export default function ResenasRecibidas({ alCambiarPendientes }) {
  const [pestana, setPestana] = useState('pendientes')
  const [pagina, setPagina] = useState(0)
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)
  const cerrarToast = useCallback(() => setToast(null), [])

  const actual = PESTANAS.find((p) => p.id === pestana)

  const cargar = useCallback(() => {
    ofertaApi.resenasRecibidas({ sinResponder: actual.sinResponder, pagina })
      .then((d) => { setDatos(d); setError(null); alCambiarPendientes(d.sinResponder) })
      .catch((e) => setError(e.message))
  }, [actual.sinResponder, pagina, alCambiarPendientes])

  useEffect(cargar, [cargar])

  function cambiarPestana(id) {
    setPestana(id)
    setPagina(0)
    setDatos(null)
  }

  // En "Sin responder" la reseña respondida sale de la lista; en "Todas" se actualiza en su lugar.
  function guardada(actualizada, mensaje) {
    setToast(mensaje)
    if (actual.sinResponder) cargar()
    else setDatos((d) => ({ ...d, resenas: { ...d.resenas, contenido: d.resenas.contenido.map((r) => (r.id === actualizada.id ? actualizada : r)) } }))
  }

  return (
    <div className="pila-lg" style={{ maxWidth: 880, margin: '0 auto' }}>
      <div className="encabezado-pagina">
        <div>
          <h1>Reseñas</h1>
          <p>Lo que dicen los turistas de tus servicios. Responder muestra que te importa su experiencia.</p>
        </div>
      </div>

      <div className="pestanas" role="tablist" aria-label="Reseñas">
        {PESTANAS.map((p) => (
          <button key={p.id} role="tab" aria-selected={pestana === p.id} onClick={() => cambiarPestana(p.id)}>
            {p.texto} {p.sinResponder && datos && <span className="conteo">{datos.sinResponder}</span>}
          </button>
        ))}
      </div>

      {error && <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>}

      {!datos && !error && <div className="esqueleto" style={{ height: 180, borderRadius: 14 }} aria-hidden="true" />}

      {datos && datos.resenas.contenido.length === 0 && (
        <div className="tarjeta vacio">
          <div className="icono-vacio"><MessageSquareText size={28} aria-hidden="true" /></div>
          <h3>{actual.sinResponder ? 'Estás al día' : 'Aún no tienes reseñas'}</h3>
          <p>{actual.sinResponder
            ? 'No hay reseñas esperando tu respuesta.'
            : 'Cuando un turista complete una reserva podrá calificar tu servicio.'}</p>
        </div>
      )}

      <div className="pila" role="tabpanel">
        {datos?.resenas.contenido.map((r) => (
          <div key={r.id} className="tarjeta pila" style={{ gap: 14 }}>
            <Resena resena={{ ...r, respuesta: null }} servicio={r.nombreServicio} />
            {r.respuesta && (
              <div className="respuesta-prestador">
                <strong>Tu respuesta</strong>
                <p>{r.respuesta}</p>
              </div>
            )}
            <Responder key={`${r.id}-${r.fechaRespuesta}`} resena={r} alGuardar={guardada} />
          </div>
        ))}
      </div>

      {datos && <Paginacion pagina={datos.resenas.pagina} totalPaginas={datos.resenas.totalPaginas} alCambiar={setPagina} />}

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
