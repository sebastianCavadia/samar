import { useCallback, useEffect, useState } from 'react'
import { CalendarDays, Eye, EyeOff, LayoutGrid, MapPin, Pencil, Plus, ShieldAlert, Store, TriangleAlert } from 'lucide-react'
import { ofertaApi } from '../api/cliente'
import Toast from '../componentes/Toast'
import { TIPOS, UNIDAD_PRECIO, precio } from '../formato'
import { ICONO_TIPO } from '../iconos'
import FormServicio from './FormServicio'
import GestionFechas from './GestionFechas'

// Panel del prestador: sus servicios (incluidos inactivos), crear, editar y publicar/ocultar (RF032).
export default function MisServicios({ usuario }) {
  const [servicios, setServicios] = useState(null)
  const [editando, setEditando] = useState(null) // null = lista, 'nuevo' o el detalle a editar
  const [gestionandoFechas, setGestionandoFechas] = useState(null) // servicio cuyas fechas y cupos se gestionan
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)
  const cerrarToast = useCallback(() => setToast(null), [])

  const cargar = useCallback(() => {
    ofertaApi.misServicios().then(setServicios).catch((e) => setError(e.message))
  }, [])

  useEffect(cargar, [cargar])

  async function abrirEdicion(id) {
    setError(null)
    try {
      setEditando(await ofertaApi.detallePropio(id))
      window.scrollTo({ top: 0 })
    } catch (e) {
      setError(e.message)
    }
  }

  async function alternarEstado(s) {
    setError(null)
    // Actualización optimista: el interruptor responde al instante y se revierte si falla.
    setServicios((lista) => lista.map((x) => (x.id === s.id ? { ...x, activo: !s.activo } : x)))
    try {
      await ofertaApi.cambiarEstado(s.id, !s.activo)
      setToast(s.activo ? `"${s.nombre}" ya no aparece en el catálogo` : `"${s.nombre}" está publicado`)
    } catch (e) {
      setServicios((lista) => lista.map((x) => (x.id === s.id ? { ...x, activo: s.activo } : x)))
      setError(e.message)
    }
  }

  if (gestionandoFechas) {
    return <GestionFechas servicio={gestionandoFechas} volver={() => { setGestionandoFechas(null); window.scrollTo({ top: 0 }) }} />
  }

  if (editando) {
    return (
      <FormServicio
        detalle={editando === 'nuevo' ? null : editando}
        alGuardar={(guardado) => {
          setToast(editando === 'nuevo' ? `"${guardado.nombre}" se publicó en el catálogo` : 'Cambios guardados')
          setEditando(null)
          cargar()
        }}
        cancelar={() => setEditando(null)}
      />
    )
  }

  const publicados = servicios?.filter((s) => s.activo).length ?? 0
  const total = servicios?.length ?? 0

  return (
    <div className="pila-lg">
      <div className="encabezado-pagina">
        <div>
          <h1>Mis servicios</h1>
          <p>Gestiona lo que los turistas ven de tu negocio en SAMAR.</p>
        </div>
        <button className="btn btn-primario" onClick={() => { setError(null); setEditando('nuevo') }}
          disabled={usuario.prestadorValidado === false}>
          <Plus size={18} aria-hidden="true" /> Nuevo servicio
        </button>
      </div>

      {usuario.prestadorValidado === false && (
        <div className="mensaje aviso">
          <ShieldAlert size={20} aria-hidden="true" />
          <span><strong>Tu cuenta está en revisión</strong>Un administrador está validando tu NIT y RNT. Podrás publicar servicios cuando termine.</span>
        </div>
      )}
      {error && <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>}

      <div className="kpis">
        <div className="tarjeta kpi">
          <span className="icono-kpi"><LayoutGrid size={22} aria-hidden="true" /></span>
          <div><strong>{total}</strong><span>Servicios registrados</span></div>
        </div>
        <div className="tarjeta kpi">
          <span className="icono-kpi"><Eye size={22} aria-hidden="true" /></span>
          <div><strong>{publicados}</strong><span>Publicados</span></div>
        </div>
        <div className="tarjeta kpi">
          <span className="icono-kpi"><EyeOff size={22} aria-hidden="true" /></span>
          <div><strong>{total - publicados}</strong><span>Ocultos</span></div>
        </div>
      </div>

      {servicios && servicios.length === 0 && (
        <div className="tarjeta vacio">
          <div className="icono-vacio"><Store size={30} aria-hidden="true" /></div>
          <h3>Aún no tienes servicios</h3>
          <p>Registra tu primer hospedaje, actividad o restaurante para que aparezca en el catálogo.</p>
        </div>
      )}

      {servicios && servicios.length > 0 && (
        <section className="tarjeta" style={{ padding: 0 }} aria-label="Lista de servicios">
          <ul className="lista-servicios">
            {servicios.map((s) => {
              const IconoTipo = ICONO_TIPO[s.tipo]
              return (
                <li key={s.id} className={`fila-servicio ${s.activo ? '' : 'inactivo'}`}>
                  <div className="miniatura">
                    {s.imagenPrincipal ? <img src={s.imagenPrincipal.url} alt="" /> : <IconoTipo size={24} aria-hidden="true" />}
                  </div>
                  <div className="info">
                    <strong>{s.nombre}</strong>
                    <div className="meta">
                      <span>{TIPOS[s.tipo]}</span>
                      <span className="dato-icono" style={{ fontSize: 13.5 }}><MapPin size={14} aria-hidden="true" />{s.zona}</span>
                      <span>{precio(s.precioBase, s.moneda)} {UNIDAD_PRECIO[s.tipo]}</span>
                    </div>
                  </div>
                  <div className="control-estado">
                    <button className="switch" role="switch" aria-checked={s.activo} onClick={() => alternarEstado(s)}
                      aria-label={`Publicar ${s.nombre} en el catálogo`} />
                    <span>{s.activo ? 'Publicado' : 'Oculto'}</span>
                  </div>
                  <div className="acciones">
                    {s.modalidad === 'RESERVA' && (
                      <button className="btn btn-secundario" onClick={() => { setGestionandoFechas(s); window.scrollTo({ top: 0 }) }}>
                        <CalendarDays size={16} aria-hidden="true" /> Fechas y cupos
                      </button>
                    )}
                    <button className="btn btn-secundario" onClick={() => abrirEdicion(s.id)}>
                      <Pencil size={16} aria-hidden="true" /> Editar
                    </button>
                  </div>
                </li>
              )
            })}
          </ul>
        </section>
      )}

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
