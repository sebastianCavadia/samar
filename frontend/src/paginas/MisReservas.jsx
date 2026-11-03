import { useCallback, useEffect, useState } from 'react'
import { CalendarCheck, CreditCard, Landmark, Send, Star, TriangleAlert, X } from 'lucide-react'
import { reservasApi } from '../api/cliente'
import { Estrellas, SelectorEstrellas } from '../componentes/Estrellas'
import TarjetaReserva from '../componentes/TarjetaReserva'
import Toast from '../componentes/Toast'
import { MOTIVOS_TURISTA, TEXTO_ESTRELLAS, hoyIso } from '../formato'

// RF041: historial clasificado
const PESTANAS = [
  { id: 'activas', texto: 'Activas', incluye: ['PENDIENTE', 'CONFIRMADA'] },
  { id: 'completadas', texto: 'Completadas', incluye: ['COMPLETADA'] },
  { id: 'canceladas', texto: 'Canceladas', incluye: ['CANCELADA'] },
  { id: 'rechazadas', texto: 'Rechazadas', incluye: ['RECHAZADA'] },
]

function AccionesTurista({ reserva: r, alActualizar, alError, alRecargar }) {
  const [modo, setModo] = useState(null) // null | 'transferencia' | 'cancelar' | 'calificar'
  const [referencia, setReferencia] = useState('')
  const [motivo, setMotivo] = useState(MOTIVOS_TURISTA[0])
  const [puntuacion, setPuntuacion] = useState(0)
  const [comentario, setComentario] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function ejecutar(accion, mensaje) {
    setEnviando(true)
    try {
      alActualizar(await accion(), mensaje)
      setModo(null)
    } catch (e) {
      alError(e.message)
      // RF076: si ya estaba calificada (p. ej. desde otra pestaña), se recarga para mostrar la que quedó.
      if (modo === 'calificar') alRecargar()
    } finally {
      setEnviando(false)
    }
  }

  if (r.calificacion) {
    return (
      <span className="tu-calificacion">
        Tu calificación: <Estrellas valor={r.calificacion} /> <strong>{TEXTO_ESTRELLAS[r.calificacion]}</strong>
      </span>
    )
  }

  if (modo === 'calificar') {
    return (
      <form className="formulario-calificar" onSubmit={(e) => {
        e.preventDefault()
        ejecutar(() => reservasApi.calificar(r.id, puntuacion, comentario), '¡Gracias! Tu reseña ya es pública')
      }}>
        <SelectorEstrellas valor={puntuacion} alCambiar={setPuntuacion} nombre={`estrellas-${r.id}`} />
        <label className="campo">
          <span>Cuéntales a otros viajeros <span className="ayuda">(opcional)</span></span>
          <textarea className="control" rows={3} maxLength={1000} value={comentario}
            onChange={(e) => setComentario(e.target.value)}
            placeholder="¿Qué te gustó? ¿Qué se podría mejorar?" />
          <span className="ayuda">Se publica con tu nombre y la inicial de tu apellido. Solo se califica una vez.</span>
        </label>
        <div className="fila-botones">
          <button className="btn btn-primario" disabled={enviando || puntuacion === 0}>
            <Send size={16} aria-hidden="true" /> Publicar reseña
          </button>
          <button type="button" className="btn btn-fantasma" onClick={() => setModo(null)}>Volver</button>
        </div>
      </form>
    )
  }

  const puedePagar = r.estado === 'PENDIENTE' && (!r.pago || r.pago.estado === 'RECHAZADO')
  const puedeCancelar = (r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA') && r.fecha >= hoyIso()

  if (modo === 'transferencia') {
    return (
      <form className="formulario-linea" onSubmit={(e) => { e.preventDefault(); ejecutar(() => reservasApi.pagar(r.id, 'COMPROBANTE', referencia), 'Comprobante enviado al prestador') }}>
        <label className="campo" style={{ flex: '1 1 220px' }}>
          <span>Número del comprobante o de la transferencia</span>
          <input className="control" value={referencia} onChange={(e) => setReferencia(e.target.value)} required maxLength={80} autoFocus />
        </label>
        <button className="btn btn-primario" disabled={enviando}>Enviar</button>
        <button type="button" className="btn btn-fantasma" onClick={() => setModo(null)}>Volver</button>
      </form>
    )
  }

  if (modo === 'cancelar') {
    return (
      <form className="formulario-linea" onSubmit={(e) => { e.preventDefault(); ejecutar(() => reservasApi.cancelar(r.id, motivo), 'Reserva cancelada') }}>
        <label className="campo" style={{ flex: '1 1 220px' }}>
          <span>¿Por qué cancelas?</span>
          <select className="control" value={motivo} onChange={(e) => setMotivo(e.target.value)}>
            {MOTIVOS_TURISTA.map((m) => <option key={m}>{m}</option>)}
          </select>
        </label>
        <button className="btn btn-peligro" disabled={enviando}>Confirmar cancelación</button>
        <button type="button" className="btn btn-fantasma" onClick={() => setModo(null)}>Volver</button>
        {r.pago?.estado === 'APROBADO' && <p className="suave pequeno" style={{ flexBasis: '100%' }}>Se te reembolsará el pago.</p>}
      </form>
    )
  }

  return (
    <>
      {puedePagar && (
        <>
          <button className="btn btn-primario" disabled={enviando}
            onClick={() => ejecutar(() => reservasApi.pagar(r.id, 'PASARELA'), 'Pago aprobado. Falta que el prestador confirme')}>
            <CreditCard size={16} aria-hidden="true" /> Pagar en línea (simulado)
          </button>
          <button className="btn btn-secundario" onClick={() => setModo('transferencia')}>
            <Landmark size={16} aria-hidden="true" /> Pagué por transferencia
          </button>
        </>
      )}
      {r.estado === 'PENDIENTE' && r.pago?.estado === 'APROBADO' && (
        <span className="suave pequeno"><CalendarCheck size={15} aria-hidden="true" /> Pago listo: el prestador debe confirmar tu reserva.</span>
      )}
      {puedeCancelar && (
        <button className="btn btn-fantasma" onClick={() => setModo('cancelar')}><X size={16} aria-hidden="true" /> Cancelar reserva</button>
      )}
      {r.puedeCalificar && (
        <button className="btn btn-primario" onClick={() => setModo('calificar')}>
          <Star size={16} aria-hidden="true" /> Calificar experiencia
        </button>
      )}
    </>
  )
}

export default function MisReservas({ resaltar }) {
  const [reservas, setReservas] = useState(null)
  const [pestana, setPestana] = useState('activas')
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)
  const cerrarToast = useCallback(() => setToast(null), [])

  const cargar = useCallback(() => {
    reservasApi.mis().then(setReservas).catch((e) => setError(e.message))
  }, [])

  useEffect(cargar, [cargar])

  function actualizar(nueva, mensaje) {
    setError(null)
    setReservas((lista) => lista.map((x) => (x.id === nueva.id ? nueva : x)))
    setToast(mensaje)
  }

  const conteo = (p) => reservas?.filter((r) => p.incluye.includes(r.estado)).length ?? 0
  const actual = PESTANAS.find((p) => p.id === pestana)
  const visibles = reservas?.filter((r) => actual.incluye.includes(r.estado)) ?? []
  const porCalificar = reservas?.filter((r) => r.puedeCalificar).length ?? 0

  return (
    <div className="pila-lg" style={{ maxWidth: 880, margin: '0 auto' }}>
      <div className="encabezado-pagina">
        <div>
          <h1>Mis reservas</h1>
          <p>Paga, revisa el estado, cancela o califica tus experiencias.</p>
        </div>
      </div>

      <div className="pestanas" role="tablist" aria-label="Estado de las reservas">
        {PESTANAS.map((p) => (
          <button key={p.id} role="tab" aria-selected={pestana === p.id} onClick={() => setPestana(p.id)}>
            {p.texto} <span className="conteo">{conteo(p)}</span>
          </button>
        ))}
      </div>

      {error && <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>}

      {porCalificar > 0 && (
        <div className="mensaje info">
          <Star size={20} aria-hidden="true" />
          <span>
            <strong>{porCalificar === 1 ? 'Tienes 1 experiencia por calificar' : `Tienes ${porCalificar} experiencias por calificar`}</strong>
            Tu reseña ayuda a otros viajeros y a los prestadores locales.
            {pestana !== 'completadas' && <> <button className="btn-enlace" onClick={() => setPestana('completadas')}>Ver completadas</button></>}
          </span>
        </div>
      )}

      {reservas && visibles.length === 0 && (
        <div className="tarjeta vacio">
          <div className="icono-vacio"><CalendarCheck size={28} aria-hidden="true" /></div>
          <h3>No tienes reservas {actual.texto.toLowerCase()}</h3>
          <p>Explora el catálogo y reserva tu próxima experiencia en Santa Marta.</p>
        </div>
      )}

      <div className="pila" role="tabpanel">
        {visibles.map((r) => (
          <TarjetaReserva key={r.id} reserva={r} resaltada={r.id === resaltar}>
            <AccionesTurista reserva={r} alActualizar={actualizar} alError={setError} alRecargar={cargar} />
          </TarjetaReserva>
        ))}
      </div>

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
