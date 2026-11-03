import { useCallback, useEffect, useState } from 'react'
import { CalendarCheck, CreditCard, Landmark, TriangleAlert, X } from 'lucide-react'
import { reservasApi } from '../api/cliente'
import TarjetaReserva from '../componentes/TarjetaReserva'
import Toast from '../componentes/Toast'
import { MOTIVOS_TURISTA, hoyIso } from '../formato'

// RF041: historial clasificado
const PESTANAS = [
  { id: 'activas', texto: 'Activas', incluye: ['PENDIENTE', 'CONFIRMADA'] },
  { id: 'completadas', texto: 'Completadas', incluye: ['COMPLETADA'] },
  { id: 'canceladas', texto: 'Canceladas', incluye: ['CANCELADA'] },
  { id: 'rechazadas', texto: 'Rechazadas', incluye: ['RECHAZADA'] },
]

function AccionesTurista({ reserva: r, alActualizar, alError }) {
  const [modo, setModo] = useState(null) // null | 'transferencia' | 'cancelar'
  const [referencia, setReferencia] = useState('')
  const [motivo, setMotivo] = useState(MOTIVOS_TURISTA[0])
  const [enviando, setEnviando] = useState(false)

  async function ejecutar(accion, mensaje) {
    setEnviando(true)
    try {
      alActualizar(await accion(), mensaje)
      setModo(null)
    } catch (e) {
      alError(e.message)
    } finally {
      setEnviando(false)
    }
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

  return (
    <div className="pila-lg" style={{ maxWidth: 880, margin: '0 auto' }}>
      <div className="encabezado-pagina">
        <div>
          <h1>Mis reservas</h1>
          <p>Paga, revisa el estado o cancela tus reservas.</p>
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
            <AccionesTurista reserva={r} alActualizar={actualizar} alError={setError} />
          </TarjetaReserva>
        ))}
      </div>

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
