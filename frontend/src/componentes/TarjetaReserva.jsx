import { useEffect, useState } from 'react'
import { CalendarDays, Clock, CreditCard, History, Hourglass, UserRound, Users } from 'lucide-react'
import { ESTADOS_RESERVA, TIPOS, fecha, fechaCorta, hora, precio } from '../formato'
import { ICONO_TIPO } from '../iconos'

const TEXTO_PAGO = {
  APROBADO: (p) => (p.medio === 'PASARELA' ? 'Pagado en línea (simulado)' : 'Transferencia aprobada'),
  PENDIENTE: () => 'Comprobante en revisión',
  RECHAZADO: () => 'Comprobante rechazado',
  REEMBOLSADO: () => 'Pago reembolsado',
}

// RF056: cuánto falta para que venza el plazo de pago. Se refresca cada 15 s.
function PlazoPago({ hasta }) {
  const [ahora, setAhora] = useState(() => Date.now())
  useEffect(() => {
    const t = setInterval(() => setAhora(Date.now()), 15000)
    return () => clearInterval(t)
  }, [])
  const restante = Math.ceil((new Date(hasta).getTime() - ahora) / 60000)
  const horaLimite = new Intl.DateTimeFormat('es-CO', { timeStyle: 'short' }).format(new Date(hasta))
  return restante > 0
    ? <span className="plazo"><Hourglass size={15} aria-hidden="true" /> Paga antes de las {horaLimite} · quedan {restante} min</span>
    : <span className="plazo vencido"><Hourglass size={15} aria-hidden="true" /> El plazo venció: los cupos se liberarán en breve</span>
}

/**
 * Tarjeta común de reserva: la usa el turista (Mis reservas) y el prestador (Reservas recibidas).
 * Las acciones de cada rol llegan como children.
 */
export default function TarjetaReserva({ reserva: r, paraPrestador, resaltada, children }) {
  const estado = ESTADOS_RESERVA[r.estado]
  const IconoTipo = ICONO_TIPO[r.tipoServicio] ?? CalendarDays
  const unidadCupo = r.tipoServicio === 'HOSPEDAJE' ? '1 habitación' : null

  return (
    <article className={`tarjeta tarjeta-reserva ${resaltada ? 'resaltada' : ''}`} aria-labelledby={`reserva-${r.id}`}>
      <div className="cabecera-reserva">
        <span className="icono-reserva" aria-hidden="true"><IconoTipo size={22} /></span>
        <div className="pila" style={{ gap: 2, flex: 1, minWidth: 0 }}>
          <h3 id={`reserva-${r.id}`}>{paraPrestador ? (r.nombreTurista || `Turista #${r.idTurista}`) : r.nombreServicio}</h3>
          <span className="suave pequeno">
            {paraPrestador ? r.nombreServicio : TIPOS[r.tipoServicio]} · Reserva #{r.id}
          </span>
        </div>
        <span className={`insignia ${estado.clase}`}>{estado.texto}</span>
      </div>

      <div className="meta-reserva">
        <span className="dato-icono"><CalendarDays size={16} aria-hidden="true" />{fechaCorta(r.fecha)}</span>
        {r.hora && <span className="dato-icono"><Clock size={16} aria-hidden="true" />{hora(r.hora)}</span>}
        <span className="dato-icono"><Users size={16} aria-hidden="true" />{r.personas} {r.personas === 1 ? 'persona' : 'personas'}{unidadCupo && ` · ${unidadCupo}`}</span>
        <strong className="total-reserva">{precio(r.total, r.moneda)}</strong>
      </div>

      <div className="estado-pago">
        <CreditCard size={16} aria-hidden="true" />
        {r.pago ? (
          <span>{TEXTO_PAGO[r.pago.estado](r.pago)} · ref. {r.pago.referencia}</span>
        ) : (
          <span>{r.estado === 'PENDIENTE' ? 'Sin pagar' : 'Sin pago'}</span>
        )}
        {r.expiraEn && <PlazoPago hasta={r.expiraEn} />}
      </div>

      {r.motivo && (r.estado === 'CANCELADA' || r.estado === 'RECHAZADA') && (
        <p className="suave pequeno">Motivo: {r.motivo}</p>
      )}

      {children && <div className="acciones-reserva">{children}</div>}

      <details className="historial">
        <summary><History size={15} aria-hidden="true" /> Historial</summary>
        <ol>
          {r.historial.map((h) => (
            <li key={`${h.fecha}-${h.estadoNuevo}`}>
              <strong>{ESTADOS_RESERVA[h.estadoNuevo].texto}</strong> · {fecha(h.fecha)}
              <span className="suave"> · {h.idUsuario == null ? 'Sistema' : (h.idUsuario === r.idTurista ? 'Turista' : 'Prestador')}</span>
              {h.detalle && <div className="suave pequeno"><UserRound size={12} aria-hidden="true" /> {h.detalle}</div>}
            </li>
          ))}
        </ol>
      </details>
    </article>
  )
}
