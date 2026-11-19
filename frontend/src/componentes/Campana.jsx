import { useCallback, useEffect, useRef, useState } from 'react'
import { Bell, CheckCheck } from 'lucide-react'
import { reservasApi } from '../api/cliente'
import { fecha } from '../formato'

const CADA_MS = 60_000

// RF037 y RF039: alertas internas del turista y del prestador sobre sus reservas.
export default function Campana() {
  const [datos, setDatos] = useState({ noLeidas: 0, recientes: [] })
  const [abierta, setAbierta] = useState(false)
  const caja = useRef(null)

  const cargar = useCallback(() => {
    reservasApi.notificaciones().then(setDatos).catch(() => {})
  }, [])

  // Se consulta al entrar y cada minuto: así la campana se entera de lo que pasa sin recargar.
  useEffect(() => {
    cargar()
    const t = setInterval(cargar, CADA_MS)
    return () => clearInterval(t)
  }, [cargar])

  useEffect(() => {
    if (!abierta) return undefined
    const fuera = (e) => { if (!caja.current?.contains(e.target)) setAbierta(false) }
    const escape = (e) => { if (e.key === 'Escape') setAbierta(false) }
    document.addEventListener('mousedown', fuera)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('mousedown', fuera)
      document.removeEventListener('keydown', escape)
    }
  }, [abierta])

  function alternar() {
    if (!abierta) cargar()
    setAbierta(!abierta)
  }

  const marcar = (promesa) => promesa.then(setDatos).catch(() => {})
  const n = datos.noLeidas

  return (
    <div className="campana" ref={caja}>
      <button className="interruptor-tema" aria-expanded={abierta} aria-controls="panel-notificaciones" onClick={alternar}
        aria-label={n > 0 ? `Notificaciones, ${n} sin leer` : 'Notificaciones'} title="Notificaciones">
        <Bell size={19} aria-hidden="true" />
        {n > 0 && <span className="conteo aviso globo" aria-hidden="true">{n > 9 ? '9+' : n}</span>}
      </button>

      {abierta && (
        <div id="panel-notificaciones" className="panel-notificaciones" role="region" aria-label="Notificaciones">
          <div className="panel-notificaciones-cabecera">
            <strong>Notificaciones</strong>
            {n > 0 && (
              <button className="btn-enlace" onClick={() => marcar(reservasApi.marcarLeidas())}>
                <CheckCheck size={15} aria-hidden="true" /> Marcar todas como leídas
              </button>
            )}
          </div>
          {datos.recientes.length === 0
            ? <p className="suave pequeno" style={{ padding: '18px 16px' }}>No tienes notificaciones todavía.</p>
            : (
              <ul className="lista-notificaciones">
                {datos.recientes.map((x) => (
                  <li key={x.id}>
                    <button className="notificacion" data-leida={x.leida}
                      onClick={() => !x.leida && marcar(reservasApi.marcarLeida(x.id))}
                      aria-label={`${x.leida ? '' : 'Sin leer: '}${x.asunto}. ${x.mensaje}`}>
                      <strong>{x.asunto}</strong>
                      <span>{x.mensaje}</span>
                      <small className="suave">{fecha(x.creadoEn)}</small>
                    </button>
                  </li>
                ))}
              </ul>
            )}
        </div>
      )}
    </div>
  )
}
