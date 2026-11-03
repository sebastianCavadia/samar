import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, CalendarPlus, CalendarX2, TriangleAlert } from 'lucide-react'
import { reservasApi } from '../api/cliente'
import Toast from '../componentes/Toast'
import { fechaCorta, hora, hoyIso, sumarDias } from '../formato'

const DIAS = [
  ['MONDAY', 'L'], ['TUESDAY', 'M'], ['WEDNESDAY', 'X'], ['THURSDAY', 'J'],
  ['FRIDAY', 'V'], ['SATURDAY', 'S'], ['SUNDAY', 'D'],
]
const NOMBRE_DIA = { MONDAY: 'lunes', TUESDAY: 'martes', WEDNESDAY: 'miércoles', THURSDAY: 'jueves', FRIDAY: 'viernes', SATURDAY: 'sábado', SUNDAY: 'domingo' }

// RF018: el prestador abre cupos por rango de fechas y bloquea días puntuales.
export default function GestionFechas({ servicio, volver }) {
  const hoy = hoyIso()
  const esHospedaje = servicio.tipo === 'HOSPEDAJE'
  const unidad = esHospedaje ? 'habitaciones' : 'cupos'
  const [form, setForm] = useState({ desde: hoy, hasta: sumarDias(hoy, 30), hora: '', cupos: '', dias: [] })
  const [fechas, setFechas] = useState(null)
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [toast, setToast] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const cerrarToast = useCallback(() => setToast(null), [])

  const cargar = useCallback(() => {
    reservasApi.fechasPrestador(servicio.id, hoy, sumarDias(hoy, 90)).then(setFechas).catch((e) => setError(e.message))
  }, [servicio.id, hoy])

  useEffect(cargar, [cargar])

  const cambiar = (campo) => (e) => setForm({ ...form, [campo]: e.target.value })
  const alternarDia = (d) => setForm({ ...form, dias: form.dias.includes(d) ? form.dias.filter((x) => x !== d) : [...form.dias, d] })

  async function abrir(e) {
    e.preventDefault()
    setError(null)
    setAviso(null)
    setEnviando(true)
    try {
      const r = await reservasApi.abrirFechas(servicio.id, {
        desde: form.desde, hasta: form.hasta, hora: form.hora || null, cupos: Number(form.cupos),
        diasSemana: form.dias.length ? form.dias : null,
      })
      setToast(`${r.creadas} fecha(s) nuevas y ${r.actualizadas} actualizada(s)`)
      if (r.omitidas.length) setAviso(r.omitidas)
      cargar()
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  async function alternarFecha(f) {
    setError(null)
    try {
      const nueva = await reservasApi.cambiarFecha(f.id, !f.activo)
      setFechas((lista) => lista.map((x) => (x.id === nueva.id ? nueva : x)))
      setToast(nueva.activo ? 'Fecha abierta de nuevo' : 'Fecha bloqueada: no se aceptarán nuevas reservas')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="pila-lg" style={{ maxWidth: 880, margin: '0 auto' }}>
      <div className="encabezado-pagina">
        <div>
          <button type="button" className="btn-enlace" onClick={volver} style={{ display: 'inline-flex', gap: 6, alignItems: 'center' }}>
            <ArrowLeft size={16} aria-hidden="true" /> Mis servicios
          </button>
          <h1 style={{ marginTop: 6 }}>Fechas y {unidad}</h1>
          <p>{servicio.nombre}: define cuántas {esHospedaje ? 'habitaciones' : 'plazas'} ofreces cada día.</p>
        </div>
      </div>

      <form className="tarjeta pila" onSubmit={abrir}>
        <h2 style={{ display: 'flex', gap: 10, alignItems: 'center' }}><CalendarPlus size={20} aria-hidden="true" /> Abrir fechas</h2>
        <div className="fila">
          <label className="campo"><span>Desde</span>
            <input className="control" type="date" min={hoy} value={form.desde} onChange={cambiar('desde')} required />
          </label>
          <label className="campo"><span>Hasta</span>
            <input className="control" type="date" min={form.desde} value={form.hasta} onChange={cambiar('hasta')} required />
          </label>
          <label className="campo"><span>{esHospedaje ? 'Habitaciones por noche' : 'Cupos por día'}</span>
            <input className="control" type="number" min="0" max="10000" value={form.cupos} onChange={cambiar('cupos')} required />
          </label>
          {!esHospedaje && (
            <label className="campo"><span>Hora <span className="ayuda">· opcional</span></span>
              <input className="control" type="time" value={form.hora} onChange={cambiar('hora')} />
            </label>
          )}
        </div>
        <div className="pila" style={{ gap: 8 }}>
          <span style={{ fontSize: 14, fontWeight: 600 }}>Solo estos días <span className="ayuda" style={{ fontWeight: 400, color: 'var(--texto-2)' }}>· si no eliges ninguno, se abren todos</span></span>
          <div className="chips" role="group" aria-label="Días de la semana">
            {DIAS.map(([d, letra]) => (
              <button key={d} type="button" className="chip chip-dia" aria-pressed={form.dias.includes(d)}
                aria-label={NOMBRE_DIA[d]} onClick={() => alternarDia(d)}>{letra}</button>
            ))}
          </div>
        </div>
        <p className="suave pequeno">Si una fecha ya existe se actualizan sus {unidad}; nunca quedan menos de los ya reservados.</p>
        {aviso && (
          <div className="mensaje aviso"><TriangleAlert size={18} aria-hidden="true" />
            <span><strong>Algunas fechas no se cambiaron</strong>{aviso.join(' · ')}</span>
          </div>
        )}
        {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}
        <div><button className="btn btn-primario" disabled={enviando}>{enviando ? 'Guardando…' : 'Abrir fechas'}</button></div>
      </form>

      <section className="tarjeta" style={{ padding: 0 }} aria-labelledby="titulo-fechas">
        <h2 id="titulo-fechas" style={{ padding: '20px 20px 8px' }}>Próximos 90 días</h2>
        {fechas?.length === 0 && (
          <div className="vacio"><div className="icono-vacio"><CalendarX2 size={26} aria-hidden="true" /></div>
            <h3>Aún no hay fechas abiertas</h3><p>Los turistas no pueden reservar hasta que abras al menos una fecha.</p></div>
        )}
        <ul className="lista-fechas">
          {fechas?.map((f) => {
            const ocupacion = f.cuposTotales ? Math.round((f.cuposReservados / f.cuposTotales) * 100) : 0
            return (
              <li key={f.id} className={f.activo ? '' : 'inactivo'}>
                <div className="info">
                  <strong>{fechaCorta(f.fecha)}{f.hora && ` · ${hora(f.hora)}`}</strong>
                  <span className="suave pequeno">{f.cuposReservados} de {f.cuposTotales} {unidad} reservados · {f.cuposLibres} libres</span>
                </div>
                <div className="barra-ocupacion" role="img" aria-label={`${ocupacion}% ocupado`}>
                  <span style={{ width: `${ocupacion}%` }} className={ocupacion >= 80 ? 'alta' : ''} />
                </div>
                <div className="control-estado">
                  <button className="switch" role="switch" aria-checked={f.activo} onClick={() => alternarFecha(f)}
                    aria-label={`Aceptar reservas el ${fechaCorta(f.fecha)}`} />
                  <span>{f.activo ? 'Abierta' : 'Bloqueada'}</span>
                </div>
              </li>
            )
          })}
        </ul>
      </section>

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
