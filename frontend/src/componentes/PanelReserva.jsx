import { useCallback, useEffect, useState } from 'react'
import { ArrowRight, CalendarX2, LogIn, Minus, Plus, Store, TriangleAlert, Users } from 'lucide-react'
import { reservasApi } from '../api/cliente'
import { UNIDAD_PRECIO, fechaCorta, hora, precio } from '../formato'

const FECHAS_VISIBLES = 6

// RF034 + RF036: elegir fecha y personas, ver el desglose (RF046) y reservar.
export default function PanelReserva({ servicio: s, usuario, alIrALogin, alReservar }) {
  const [fechas, setFechas] = useState(null)
  const [error, setError] = useState(null)
  const [elegida, setElegida] = useState(null)
  const [personas, setPersonas] = useState(1)
  const [enviando, setEnviando] = useState(false)
  const [verTodas, setVerTodas] = useState(false)

  const esHospedaje = s.tipo === 'HOSPEDAJE'
  const admiteReserva = s.modalidad === 'RESERVA'

  const cargar = useCallback(() => {
    if (!admiteReserva) return
    reservasApi.disponibilidad(s.id).then(setFechas).catch((e) => setError(e.message))
  }, [s.id, admiteReserva])

  useEffect(cargar, [cargar])

  // Hospedaje: el límite es la capacidad de la habitación. Resto: los cupos libres de la fecha.
  const maxPersonas = esHospedaje ? (s.hospedaje?.capacidadPersonas ?? 10) : Math.min(elegida?.cuposLibres ?? 20, 20)
  const unidades = esHospedaje ? 1 : personas
  const total = Number(s.precioBase) * unidades
  const unidad = (n) => (esHospedaje ? (n === 1 ? 'habitación' : 'habitaciones') : (n === 1 ? 'cupo' : 'cupos'))

  function elegir(f) {
    setElegida(f)
    setError(null)
    if (!esHospedaje && personas > f.cuposLibres) setPersonas(Math.max(1, f.cuposLibres))
  }

  async function reservar() {
    setEnviando(true)
    setError(null)
    try {
      alReservar(await reservasApi.crear(elegida.id, personas))
    } catch (e) {
      setError(e.message)
      setElegida(null)
      cargar() // los cupos pudieron cambiar: se muestran actualizados
    } finally {
      setEnviando(false)
    }
  }

  const visibles = fechas && (verTodas ? fechas : fechas.slice(0, FECHAS_VISIBLES))

  return (
    <div className="tarjeta tarjeta-precio pila">
      <div>
        <span className="desde">Precio desde</span>
        <div className="monto">{precio(s.precioBase, s.moneda)}</div>
        <div className="unidad">{s.moneda} · {UNIDAD_PRECIO[s.tipo]}</div>
      </div>

      {!admiteReserva && (
        <div className="mensaje info">
          <Store size={18} aria-hidden="true" />
          <span>No necesita reserva: llega directamente en su horario de atención.</span>
        </div>
      )}

      {admiteReserva && (
        <>
          <div className="pila" style={{ gap: 8 }}>
            <span className="etiqueta-paso">1. Elige la fecha</span>
            {fechas === null && !error && <div className="esqueleto" style={{ height: 56 }} />}
            {fechas?.length === 0 && (
              <div className="mensaje info">
                <CalendarX2 size={18} aria-hidden="true" />
                <span>El prestador todavía no ha publicado fechas disponibles.</span>
              </div>
            )}
            {visibles?.length > 0 && (
              <div className="opciones-fecha" role="radiogroup" aria-label="Fechas disponibles">
                {visibles.map((f) => (
                  <button key={f.id} type="button" role="radio" className="opcion-fecha"
                    aria-checked={elegida?.id === f.id} disabled={f.cuposLibres === 0} onClick={() => elegir(f)}>
                    <strong>{fechaCorta(f.fecha)}</strong>
                    <span>
                      {f.hora && `${hora(f.hora)} · `}
                      {f.cuposLibres === 0 ? 'Agotado' : `${f.cuposLibres} ${unidad(f.cuposLibres)}`}
                    </span>
                  </button>
                ))}
              </div>
            )}
            {fechas?.length > FECHAS_VISIBLES && (
              <button type="button" className="btn-enlace" style={{ alignSelf: 'flex-start' }} onClick={() => setVerTodas(!verTodas)}>
                {verTodas ? 'Ver menos fechas' : `Ver las ${fechas.length} fechas`}
              </button>
            )}
          </div>

          <div className="pila" style={{ gap: 8 }}>
            <span className="etiqueta-paso">2. ¿Cuántas personas?</span>
            <div className="contador-personas">
              <button type="button" className="btn btn-secundario btn-icono" aria-label="Una persona menos"
                disabled={personas <= 1} onClick={() => setPersonas(personas - 1)}>
                <Minus size={16} aria-hidden="true" />
              </button>
              <span aria-live="polite"><Users size={16} aria-hidden="true" /> {personas} {personas === 1 ? 'persona' : 'personas'}</span>
              <button type="button" className="btn btn-secundario btn-icono" aria-label="Una persona más"
                disabled={personas >= maxPersonas} onClick={() => setPersonas(personas + 1)}>
                <Plus size={16} aria-hidden="true" />
              </button>
            </div>
            {esHospedaje && <p className="suave pequeno">1 habitación para hasta {maxPersonas} personas · 1 noche</p>}
          </div>

          {elegida && (
            <dl className="desglose">
              <dt>{precio(s.precioBase, s.moneda)} × {esHospedaje ? '1 noche' : `${personas} ${personas === 1 ? 'persona' : 'personas'}`}</dt>
              <dd>{precio(total, s.moneda)}</dd>
              <dt className="total">Total</dt>
              <dd className="total">{precio(total, s.moneda)}</dd>
            </dl>
          )}

          {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}

          {!usuario && (
            <button className="btn btn-primario btn-grande btn-bloque" onClick={alIrALogin}>
              <LogIn size={18} aria-hidden="true" /> Inicia sesión para reservar
            </button>
          )}
          {usuario && usuario.rol !== 'TURISTA' && (
            <p className="mensaje info">Solo las cuentas de turista pueden hacer reservas.</p>
          )}
          {usuario?.rol === 'TURISTA' && (
            <button className="btn btn-primario btn-grande btn-bloque" disabled={!elegida || enviando} onClick={reservar}>
              {enviando ? 'Reservando…' : elegida ? 'Reservar' : 'Elige una fecha'} <ArrowRight size={18} aria-hidden="true" />
            </button>
          )}
          <p className="suave pequeno">Después de reservar tienes 15 minutos para pagar; si no, los cupos se liberan.</p>
        </>
      )}
    </div>
  )
}
