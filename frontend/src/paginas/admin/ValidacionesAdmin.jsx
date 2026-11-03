import { useEffect, useState } from 'react'
import { ArrowLeft, BadgeCheck, Ban, Building2, Inbox, TriangleAlert } from 'lucide-react'
import { adminApi } from '../../api/cliente'
import { ESTADOS_VALIDACION, TIPOS_PRESTADOR, fecha } from '../../formato'
import HistorialAdmin from './HistorialAdmin'

const FILTROS = [
  { id: 'PENDIENTE', texto: 'Pendientes' },
  { id: 'VALIDADO', texto: 'Validados' },
  { id: 'RECHAZADO', texto: 'Rechazados' },
]

function Dato({ etiqueta, children }) {
  return <div className="dato-ficha"><small>{etiqueta}</small><span>{children || '-'}</span></div>
}

// RF095: revisar NIT y RNT y aprobar o rechazar con motivo (mockup 2).
export default function ValidacionesAdmin({ alCambiar }) {
  const [filtro, setFiltro] = useState('PENDIENTE')
  const [lista, setLista] = useState(null)
  const [elegido, setElegido] = useState(null)
  const [historial, setHistorial] = useState([])
  const [motivo, setMotivo] = useState('')
  const [rechazando, setRechazando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let vigente = true
    setElegido(null)
    adminApi.prestadores(filtro)
      .then((l) => { if (vigente) { setLista(l); setError(null) } })
      .catch((e) => { if (vigente) setError(e.message) })
    return () => { vigente = false }
  }, [filtro])

  useEffect(() => {
    if (!elegido) return
    setMotivo('')
    setRechazando(false)
    adminApi.usuario(elegido.id).then((f) => setHistorial(f.historial)).catch(() => setHistorial([]))
  }, [elegido])

  async function decidir(accion) {
    setEnviando(true)
    setError(null)
    try {
      const actualizado = accion === 'validar' ? await adminApi.validar(elegido.id) : await adminApi.rechazar(elegido.id, motivo)
      // Sale de la lista actual (p. ej. de "Pendientes") porque cambió su estado de validación.
      setLista((l) => l.filter((p) => p.id !== actualizado.id))
      setElegido(null)
      alCambiar(accion === 'validar' ? `${actualizado.nombre} quedó validado: ya puede publicar` : `Solicitud de ${actualizado.nombre} rechazada`)
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pila">
      <div className="chips" role="group" aria-label="Estado de validación">
        {FILTROS.map((f) => (
          <button key={f.id} className="chip" aria-pressed={filtro === f.id} onClick={() => setFiltro(f.id)}>{f.texto}</button>
        ))}
      </div>
      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}

      <div className={`admin-layout ${elegido ? 'con-seleccion' : ''}`}>
        <section className="tarjeta lista-admin" aria-label="Prestadores">
          {lista?.length === 0 && (
            <div className="vacio"><div className="icono-vacio"><Inbox size={26} aria-hidden="true" /></div>
              <h3>No hay prestadores {FILTROS.find((f) => f.id === filtro).texto.toLowerCase()}</h3></div>
          )}
          <ul>
            {lista?.map((p) => (
              <li key={p.id}>
                <button className="fila-admin" aria-pressed={elegido?.id === p.id} onClick={() => setElegido(p)}>
                  <span className="icono-reserva" aria-hidden="true"><Building2 size={20} /></span>
                  <span className="info">
                    <strong>{p.nombre}</strong>
                    <span className="suave pequeno">{TIPOS_PRESTADOR[p.tipoPrestador]} · NIT {p.nit} · RNT {p.rnt}</span>
                    <span className="suave pequeno">Registro: {fecha(p.registradoEn)}</span>
                  </span>
                  <span className={`insignia ${ESTADOS_VALIDACION[p.estadoValidacion].clase}`}>{ESTADOS_VALIDACION[p.estadoValidacion].texto}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>

        {elegido && (
          <aside className="tarjeta ficha-admin" aria-label={`Detalle de ${elegido.nombre}`}>
            <button className="btn-enlace solo-movil" onClick={() => setElegido(null)}><ArrowLeft size={16} aria-hidden="true" /> Volver a la lista</button>
            <div>
              <h2>{elegido.nombre}</h2>
              <p className="suave">{TIPOS_PRESTADOR[elegido.tipoPrestador]} · {elegido.direccion}{elegido.zona && `, ${elegido.zona}`}</p>
            </div>
            <div className="datos-ficha">
              <Dato etiqueta="NIT">{elegido.nit}</Dato>
              <Dato etiqueta="RNT">{elegido.rnt}</Dato>
              <Dato etiqueta="Correo">{elegido.correo}</Dato>
              <Dato etiqueta="Teléfono">{elegido.telefono}</Dato>
              <Dato etiqueta="Registro">{fecha(elegido.registradoEn)}</Dato>
              {elegido.validadoEn && <Dato etiqueta="Validado">{fecha(elegido.validadoEn)}</Dato>}
              {elegido.idiomasGuia?.length > 0 && <Dato etiqueta="Idiomas de guías">{elegido.idiomasGuia.join(', ')}</Dato>}
            </div>
            {elegido.motivoRechazo && (
              <div className="mensaje error"><Ban size={18} aria-hidden="true" /><span><strong>Motivo del rechazo</strong>{elegido.motivoRechazo}</span></div>
            )}

            {elegido.estadoValidacion !== 'VALIDADO' && !rechazando && (
              <div className="fila">
                <button className="btn btn-primario" disabled={enviando} onClick={() => decidir('validar')}>
                  <BadgeCheck size={18} aria-hidden="true" /> {elegido.estadoValidacion === 'RECHAZADO' ? 'Validar de todos modos' : 'Aprobar'}
                </button>
                {elegido.estadoValidacion === 'PENDIENTE' && (
                  <button className="btn btn-secundario" onClick={() => setRechazando(true)}><Ban size={18} aria-hidden="true" /> Rechazar</button>
                )}
              </div>
            )}
            {rechazando && (
              <form className="pila" onSubmit={(e) => { e.preventDefault(); decidir('rechazar') }}>
                <label className="campo">
                  <span>Motivo del rechazo <span className="ayuda">· lo recibirá el prestador por correo</span></span>
                  <textarea className="control" rows={3} value={motivo} onChange={(e) => setMotivo(e.target.value)} required maxLength={300} autoFocus
                    placeholder="Ej.: El RNT no corresponde al NIT registrado" />
                </label>
                <div className="fila">
                  <button className="btn btn-peligro" disabled={enviando || !motivo.trim()}>Confirmar rechazo</button>
                  <button type="button" className="btn btn-fantasma" onClick={() => setRechazando(false)}>Cancelar</button>
                </div>
              </form>
            )}
            <HistorialAdmin eventos={historial} />
          </aside>
        )}
      </div>
    </div>
  )
}
