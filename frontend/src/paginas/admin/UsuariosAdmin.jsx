import { useEffect, useState } from 'react'
import { ArrowLeft, Info, Search, TriangleAlert, UserPlus, X } from 'lucide-react'
import { adminApi } from '../../api/cliente'
import Paginacion from '../../componentes/Paginacion'
import CampoClave from '../../componentes/CampoClave'
import { ESTADOS_CUENTA, ESTADOS_VALIDACION, ROLES, TIPOS_PRESTADOR, fecha, iniciales } from '../../formato'
import HistorialAdmin from './HistorialAdmin'

function Dato({ etiqueta, children }) {
  return <div className="dato-ficha"><small>{etiqueta}</small><span>{children || '-'}</span></div>
}

const fechaSimple = (iso) => (iso ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(`${iso}T12:00:00`)) : '-')

// RF092 ficha + RF093 estado (motivo obligatorio, aviso por correo RF100) + RF094 editar (mockup 3).
function FichaUsuario({ ficha, yo, alActualizar, alCerrar }) {
  const opciones = Object.keys(ESTADOS_CUENTA).filter((e) => e !== ficha.estado)
  const [estado, setEstado] = useState(opciones[0])
  const [motivo, setMotivo] = useState('')
  const [datos, setDatos] = useState({ nombre: ficha.nombre, telefono: ficha.telefono ?? '' })
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const esYo = ficha.id === yo.id
  const est = ESTADOS_CUENTA[ficha.estado]

  async function ejecutar(accion, mensaje) {
    setEnviando(true)
    setError(null)
    try {
      alActualizar(await accion(), mensaje)
      setMotivo('')
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <aside className="tarjeta ficha-admin" aria-label={`Ficha de ${ficha.nombre}`}>
      <button className="btn-enlace solo-movil" onClick={alCerrar}><ArrowLeft size={16} aria-hidden="true" /> Volver a la lista</button>
      <div className="perfil-cabecera">
        <span className="avatar grande" aria-hidden="true">{iniciales(ficha.nombre)}</span>
        <div className="pila" style={{ gap: 4, minWidth: 0 }}>
          <h2>{ficha.nombre}</h2>
          <span className="suave pequeno" style={{ overflowWrap: 'anywhere' }}>{ficha.correo}</span>
          <div className="chips">
            <span className="insignia tipo">{ROLES[ficha.rol]}</span>
            <span className={`insignia ${est.clase}`}>{est.texto}</span>
          </div>
        </div>
      </div>

      <div className="datos-ficha">
        <Dato etiqueta="Último acceso">{ficha.ultimoAcceso ? fecha(ficha.ultimoAcceso) : 'Nunca'}</Dato>
        <Dato etiqueta="Registro">{fecha(ficha.creadoEn)}</Dato>
        <Dato etiqueta="Teléfono">{ficha.telefono}</Dato>
        {ficha.areaResponsable && <Dato etiqueta="Área responsable">{ficha.areaResponsable}</Dato>}
        {ficha.turista && (
          <>
            <Dato etiqueta="Documento">{ficha.turista.tipoDocumento} {ficha.turista.numeroDocumento}</Dato>
            <Dato etiqueta="Nacionalidad">{ficha.turista.nacionalidad}</Dato>
            <Dato etiqueta="País de residencia">{ficha.turista.paisResidencia}</Dato>
            <Dato etiqueta="Nacimiento">{fechaSimple(ficha.turista.fechaNacimiento)}</Dato>
          </>
        )}
        {ficha.prestador && (
          <>
            <Dato etiqueta="Tipo">{TIPOS_PRESTADOR[ficha.prestador.tipoPrestador]}</Dato>
            <Dato etiqueta="NIT / RNT">{ficha.prestador.nit} / {ficha.prestador.rnt}</Dato>
            <Dato etiqueta="Validación">{ESTADOS_VALIDACION[ficha.prestador.estadoValidacion].texto}</Dato>
          </>
        )}
      </div>

      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}

      {esYo ? (
        <p className="mensaje info"><Info size={18} aria-hidden="true" /> Es tu propia cuenta: otro administrador debe cambiar su estado.</p>
      ) : (
        <form className="pila seccion-ficha" onSubmit={(e) => {
          e.preventDefault()
          ejecutar(() => adminApi.cambiarEstado(ficha.id, estado, motivo), `Cuenta de ${ficha.nombre}: ${ESTADOS_CUENTA[estado].texto.toLowerCase()}`)
        }}>
          <h3 className="titulo-seccion">Cambiar estado</h3>
          <div className="chips" role="radiogroup" aria-label="Nuevo estado">
            {opciones.map((o) => (
              <button key={o} type="button" role="radio" className="chip" aria-checked={estado === o}
                onClick={() => setEstado(o)}>{o === 'ACTIVO' ? 'Activar' : o === 'INACTIVO' ? 'Desactivar' : 'Bloquear'}</button>
            ))}
          </div>
          <label className="campo">
            <span>Motivo <span className="ayuda">· obligatorio, queda en la bitácora</span></span>
            <textarea className="control" rows={2} value={motivo} onChange={(e) => setMotivo(e.target.value)} required maxLength={300}
              placeholder="Describe el motivo de la acción…" />
          </label>
          <p className="suave pequeno"><Info size={13} aria-hidden="true" /> Se le enviará un correo al usuario informando el cambio.</p>
          <div><button className={estado === 'ACTIVO' ? 'btn btn-primario' : 'btn btn-peligro'} disabled={enviando || !motivo.trim()}>
            {estado === 'ACTIVO' ? 'Activar cuenta' : estado === 'INACTIVO' ? 'Desactivar cuenta' : 'Bloquear cuenta'}
          </button></div>
          {ficha.rol === 'PRESTADOR' && estado !== 'ACTIVO' && (
            <p className="suave pequeno">Sus servicios dejarán de aparecer en el catálogo en menos de un minuto.</p>
          )}
        </form>
      )}

      <form className="pila seccion-ficha" onSubmit={(e) => {
        e.preventDefault()
        ejecutar(() => adminApi.editar(ficha.id, { nombre: datos.nombre, telefono: datos.telefono || null }), 'Datos actualizados')
      }}>
        <h3 className="titulo-seccion">Editar datos</h3>
        <div className="fila">
          <label className="campo"><span>Nombre</span>
            <input className="control" value={datos.nombre} onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} required maxLength={120} />
          </label>
          <label className="campo"><span>Teléfono</span>
            <input className="control" type="tel" value={datos.telefono} onChange={(e) => setDatos({ ...datos, telefono: e.target.value })} maxLength={40} />
          </label>
        </div>
        <div><button className="btn btn-secundario" disabled={enviando}>Guardar datos</button></div>
      </form>

      <HistorialAdmin eventos={ficha.historial} />
    </aside>
  )
}

function NuevoAdministrador({ alCrear, alCancelar }) {
  const [datos, setDatos] = useState({ nombre: '', correo: '', password: '', areaResponsable: '' })
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const cambiar = (c) => (e) => setDatos({ ...datos, [c]: e.target.value })

  async function enviar(e) {
    e.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      alCrear(await adminApi.crearAdministrador(datos))
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="tarjeta pila" onSubmit={enviar} aria-label="Nuevo administrador">
      <div className="encabezado-seccion">
        <h2>Nuevo administrador</h2>
        <button type="button" className="btn btn-fantasma btn-icono" aria-label="Cerrar" onClick={alCancelar}><X size={18} aria-hidden="true" /></button>
      </div>
      <div className="fila">
        <label className="campo"><span>Nombre</span><input className="control" value={datos.nombre} onChange={cambiar('nombre')} required maxLength={120} /></label>
        <label className="campo"><span>Correo</span><input className="control" type="email" value={datos.correo} onChange={cambiar('correo')} required /></label>
      </div>
      <div className="fila">
        <CampoClave etiqueta="Contraseña inicial" ayuda="mínimo 8" autoComplete="new-password" minLength={8} value={datos.password} onChange={cambiar('password')} required />
        <label className="campo"><span>Área responsable <span className="ayuda">· RF099</span></span>
          <input className="control" value={datos.areaResponsable} onChange={cambiar('areaResponsable')} required maxLength={120} placeholder="Soporte, Validaciones…" />
        </label>
      </div>
      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}
      <div><button className="btn btn-primario" disabled={enviando}>Crear administrador</button></div>
    </form>
  )
}

// RF091: buscar por nombre o correo, rol y estado, de 10 en 10.
export default function UsuariosAdmin({ yo, alCambiar }) {
  const [texto, setTexto] = useState('')
  const [filtros, setFiltros] = useState({ q: '', rol: '', estado: '' })
  const [pagina, setPagina] = useState(0)
  const [resultado, setResultado] = useState(null)
  const [ficha, setFicha] = useState(null)
  const [creando, setCreando] = useState(false)
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    if (texto.trim() === filtros.q) return undefined
    const t = setTimeout(() => { setFiltros((f) => ({ ...f, q: texto.trim() })); setPagina(0) }, 350)
    return () => clearTimeout(t)
  }, [texto, filtros.q])

  useEffect(() => {
    let vigente = true
    adminApi.usuarios({ ...filtros, pagina })
      .then((r) => { if (vigente) { setResultado(r); setError(null) } })
      .catch((e) => { if (vigente) setError(e.message) })
    return () => { vigente = false }
  }, [filtros, pagina, recarga])

  async function abrir(id) {
    try {
      setFicha(await adminApi.usuario(id))
    } catch (e) {
      setError(e.message)
    }
  }

  function actualizar(nueva, mensaje) {
    setFicha(nueva)
    setRecarga((n) => n + 1)
    alCambiar(mensaje)
  }

  return (
    <div className="pila">
      <div className="barra-busqueda-admin">
        <label className="con-icono" style={{ flex: '2 1 260px' }}>
          <span className="solo-lectores">Buscar usuarios</span>
          <Search size={18} aria-hidden="true" />
          <input className="control" type="search" placeholder="Buscar por nombre o correo…" value={texto} onChange={(e) => setTexto(e.target.value)} />
        </label>
        <select className="control" aria-label="Rol" value={filtros.rol} onChange={(e) => { setFiltros({ ...filtros, rol: e.target.value }); setPagina(0) }}>
          <option value="">Todos los roles</option>
          {Object.entries(ROLES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
        <select className="control" aria-label="Estado" value={filtros.estado} onChange={(e) => { setFiltros({ ...filtros, estado: e.target.value }); setPagina(0) }}>
          <option value="">Todos los estados</option>
          {Object.entries(ESTADOS_CUENTA).map(([v, t]) => <option key={v} value={v}>{t.texto}</option>)}
        </select>
        <button className="btn btn-primario" onClick={() => setCreando(true)}><UserPlus size={18} aria-hidden="true" /> Nuevo administrador</button>
      </div>

      {creando && (
        <NuevoAdministrador alCancelar={() => setCreando(false)}
          alCrear={(f) => { setCreando(false); setFicha(f); setRecarga((n) => n + 1); alCambiar(`Administrador ${f.nombre} creado`) }} />
      )}
      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}

      <div className={`admin-layout ${ficha ? 'con-seleccion' : ''}`}>
        <section className="tarjeta lista-admin" aria-label="Usuarios">
          {resultado && <p className="suave pequeno" style={{ padding: '14px 18px 0' }}>{resultado.totalElementos} usuario(s)</p>}
          <ul>
            {resultado?.contenido.map((u) => (
              <li key={u.id}>
                <button className="fila-admin" aria-pressed={ficha?.id === u.id} onClick={() => abrir(u.id)}>
                  <span className="avatar" aria-hidden="true">{iniciales(u.nombre)}</span>
                  <span className="info">
                    <strong>{u.nombre}</strong>
                    <span className="suave pequeno" style={{ overflowWrap: 'anywhere' }}>{u.correo}</span>
                    <span className="suave pequeno">{ROLES[u.rol]} · último acceso: {u.ultimoAcceso ? fecha(u.ultimoAcceso) : 'nunca'}</span>
                  </span>
                  <span className={`insignia ${ESTADOS_CUENTA[u.estado].clase}`}>{ESTADOS_CUENTA[u.estado].texto}</span>
                </button>
              </li>
            ))}
          </ul>
          {resultado && resultado.contenido.length === 0 && <p className="vacio">Ningún usuario coincide con la búsqueda.</p>}
          {resultado && (
            <div style={{ padding: 12 }}>
              <Paginacion pagina={resultado.pagina} totalPaginas={resultado.totalPaginas} alCambiar={setPagina} />
            </div>
          )}
        </section>

        {ficha && <FichaUsuario key={`${ficha.id}-${ficha.estado}-${ficha.historial.length}`} ficha={ficha} yo={yo}
          alActualizar={actualizar} alCerrar={() => setFicha(null)} />}
      </div>
    </div>
  )
}
