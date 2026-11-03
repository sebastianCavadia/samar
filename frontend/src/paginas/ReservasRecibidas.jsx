import { useCallback, useEffect, useState } from 'react'
import { BadgeCheck, Ban, Check, Inbox, TriangleAlert, X } from 'lucide-react'
import { reservasApi } from '../api/cliente'
import TarjetaReserva from '../componentes/TarjetaReserva'
import Toast from '../componentes/Toast'
import { MOTIVOS_PRESTADOR, hoyIso } from '../formato'

const FILTROS = [
  { id: '', texto: 'Todas' },
  { id: 'PENDIENTE', texto: 'Pendientes' },
  { id: 'CONFIRMADA', texto: 'Confirmadas' },
  { id: 'COMPLETADA', texto: 'Completadas' },
  { id: 'CANCELADA', texto: 'Canceladas' },
  { id: 'RECHAZADA', texto: 'Rechazadas' },
]

// RF038 confirmar/rechazar, RF044 revisar comprobante, RF040 cancelar.
function AccionesPrestador({ reserva: r, alActualizar, alError }) {
  const [modo, setModo] = useState(null) // null | 'rechazar' | 'cancelar'
  const [motivo, setMotivo] = useState(MOTIVOS_PRESTADOR[0])
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

  if (modo) {
    const cancelar = modo === 'cancelar'
    return (
      <form className="formulario-linea" onSubmit={(e) => {
        e.preventDefault()
        ejecutar(() => (cancelar ? reservasApi.cancelar(r.id, motivo) : reservasApi.rechazar(r.id, motivo)),
          cancelar ? 'Reserva cancelada' : 'Reserva rechazada')
      }}>
        <label className="campo" style={{ flex: '1 1 240px' }}>
          <span>Motivo (lo verá el turista)</span>
          <select className="control" value={motivo} onChange={(e) => setMotivo(e.target.value)}>
            {MOTIVOS_PRESTADOR.map((m) => <option key={m}>{m}</option>)}
          </select>
        </label>
        <button className="btn btn-peligro" disabled={enviando}>{cancelar ? 'Confirmar cancelación' : 'Confirmar rechazo'}</button>
        <button type="button" className="btn btn-fantasma" onClick={() => setModo(null)}>Volver</button>
        {r.pago?.estado === 'APROBADO' && <p className="suave pequeno" style={{ flexBasis: '100%' }}>Se le reembolsará el pago al turista.</p>}
      </form>
    )
  }

  const comprobantePorRevisar = r.estado === 'PENDIENTE' && r.pago?.estado === 'PENDIENTE'
  const listaParaConfirmar = r.estado === 'PENDIENTE' && r.pago?.estado === 'APROBADO'

  return (
    <>
      {comprobantePorRevisar && (
        <>
          <button className="btn btn-primario" disabled={enviando}
            onClick={() => ejecutar(() => reservasApi.validarPago(r.id, true), 'Comprobante aprobado. Ya puedes confirmar')}>
            <BadgeCheck size={16} aria-hidden="true" /> Aprobar comprobante
          </button>
          <button className="btn btn-secundario" disabled={enviando}
            onClick={() => ejecutar(() => reservasApi.validarPago(r.id, false), 'Comprobante rechazado')}>
            <Ban size={16} aria-hidden="true" /> No es válido
          </button>
        </>
      )}
      {listaParaConfirmar && (
        <button className="btn btn-primario" disabled={enviando}
          onClick={() => ejecutar(() => reservasApi.confirmar(r.id), 'Reserva confirmada')}>
          <Check size={16} aria-hidden="true" /> Confirmar reserva
        </button>
      )}
      {r.estado === 'PENDIENTE' && !r.pago && <span className="suave pequeno">Esperando el pago del turista.</span>}
      {r.estado === 'PENDIENTE' && (
        <button className="btn btn-fantasma" onClick={() => setModo('rechazar')}><X size={16} aria-hidden="true" /> Rechazar</button>
      )}
      {r.estado === 'CONFIRMADA' && r.fecha >= hoyIso() && (
        <button className="btn btn-fantasma" onClick={() => setModo('cancelar')}><X size={16} aria-hidden="true" /> Cancelar</button>
      )}
    </>
  )
}

export default function ReservasRecibidas() {
  const [reservas, setReservas] = useState(null)
  const [filtro, setFiltro] = useState('')
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)
  const cerrarToast = useCallback(() => setToast(null), [])

  useEffect(() => {
    let vigente = true
    reservasApi.recibidas(filtro)
      .then((r) => { if (vigente) { setReservas(r); setError(null) } })
      .catch((e) => { if (vigente) setError(e.message) })
    return () => { vigente = false }
  }, [filtro])

  function actualizar(nueva, mensaje) {
    setError(null)
    setReservas((lista) => lista.map((x) => (x.id === nueva.id ? nueva : x)))
    setToast(mensaje)
  }

  // Lo que requiere acción del prestador: comprobantes por revisar y reservas pagadas sin confirmar.
  const porAtender = reservas?.filter((r) => r.estado === 'PENDIENTE' && r.pago && r.pago.estado !== 'RECHAZADO').length ?? 0

  return (
    <div className="pila-lg" style={{ maxWidth: 880, margin: '0 auto' }}>
      <div className="encabezado-pagina">
        <div>
          <h1>Reservas recibidas</h1>
          <p>Revisa los pagos y confirma o rechaza las solicitudes de tus servicios.</p>
        </div>
        {porAtender > 0 && <span className="insignia aviso">{porAtender} por atender</span>}
      </div>

      <div className="chips" role="group" aria-label="Filtrar por estado">
        {FILTROS.map((f) => (
          <button key={f.id} className="chip" aria-pressed={filtro === f.id} onClick={() => setFiltro(f.id)}>{f.texto}</button>
        ))}
      </div>

      {error && <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>}

      {reservas && reservas.length === 0 && (
        <div className="tarjeta vacio">
          <div className="icono-vacio"><Inbox size={28} aria-hidden="true" /></div>
          <h3>No hay reservas {filtro ? 'con este estado' : 'todavía'}</h3>
          <p>Cuando un turista reserve uno de tus servicios aparecerá aquí.</p>
        </div>
      )}

      <div className="pila">
        {reservas?.map((r) => (
          <TarjetaReserva key={r.id} reserva={r} paraPrestador>
            <AccionesPrestador reserva={r} alActualizar={actualizar} alError={setError} />
          </TarjetaReserva>
        ))}
      </div>

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
