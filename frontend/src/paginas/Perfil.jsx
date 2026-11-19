import { useCallback, useEffect, useRef, useState } from 'react'
import { BadgeCheck, Briefcase, Camera, Clock, KeyRound, Mail, Save, Trash2, TriangleAlert, UserRound } from 'lucide-react'
import { cuentasApi } from '../api/cliente'
import AvisoValidacion from '../componentes/AvisoValidacion'
import CampoClave from '../componentes/CampoClave'
import Toast from '../componentes/Toast'
import { ROLES, errorImagen, fecha, iniciales } from '../formato'

// RF008: foto de perfil. Exige sesión, así que se descarga con el token y se muestra como blob.
function FotoPerfil({ usuario, alCambiar, alAvisar }) {
  const [url, setUrl] = useState(null)
  const [version, setVersion] = useState(0)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState(null)
  const entrada = useRef(null)
  const tieneFoto = Boolean(url)

  useEffect(() => {
    if (usuario.tieneFoto === false) return undefined
    let vigente = true
    let creada = null
    cuentasApi.foto()
      .then((blob) => {
        if (!vigente || !blob) return
        creada = URL.createObjectURL(blob)
        setUrl(creada)
      })
      .catch(() => {})
    return () => {
      vigente = false
      if (creada) URL.revokeObjectURL(creada)
    }
  }, [usuario.tieneFoto, version])

  async function subir(e) {
    const archivo = e.target.files?.[0]
    e.target.value = ''
    if (!archivo) return
    const problema = errorImagen(archivo)
    if (problema) {
      setError(problema)
      return
    }
    setError(null)
    setTrabajando(true)
    try {
      await cuentasApi.subirFoto(archivo)
      alCambiar(true)
      setVersion((v) => v + 1)
      alAvisar('Foto actualizada')
    } catch (err) {
      setError(err.message)
    } finally {
      setTrabajando(false)
    }
  }

  async function quitar() {
    setTrabajando(true)
    try {
      await cuentasApi.eliminarFoto()
      setUrl(null)
      alCambiar(false)
      alAvisar('Foto eliminada')
    } catch (err) {
      setError(err.message)
    } finally {
      setTrabajando(false)
    }
  }

  return (
    <div className="pila" style={{ gap: 10, alignItems: 'flex-start' }}>
      {url
        ? <img className="avatar grande foto" src={url} alt={`Foto de perfil de ${usuario.nombre}`} />
        : <span className="avatar grande" aria-hidden="true">{iniciales(usuario.nombre)}</span>}
      <div className="fila-botones">
        <input ref={entrada} type="file" accept="image/jpeg,image/png" hidden onChange={subir} />
        <button type="button" className="btn btn-secundario" disabled={trabajando} onClick={() => entrada.current?.click()}>
          <Camera size={16} aria-hidden="true" /> {tieneFoto ? 'Cambiar foto' : 'Subir foto'}
        </button>
        {tieneFoto && (
          <button type="button" className="btn btn-fantasma" disabled={trabajando} onClick={quitar}>
            <Trash2 size={16} aria-hidden="true" /> Quitar
          </button>
        )}
      </div>
      <span className="suave pequeno">JPEG o PNG, máximo 2 MB.</span>
      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}
    </div>
  )
}

// RF008: nombre y teléfono; los prestadores también su ubicación. El correo identifica la cuenta y no cambia.
function EditarDatos({ usuario: u, alGuardar }) {
  const esPrestador = u.rol === 'PRESTADOR'
  const [datos, setDatos] = useState({
    nombre: u.nombre, telefono: u.telefono ?? '', direccion: u.direccion ?? '', zona: u.zona ?? '',
  })
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const cambiar = (campo) => (e) => setDatos({ ...datos, [campo]: e.target.value })

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      alGuardar(await cuentasApi.editarPerfil(datos))
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="tarjeta pila" onSubmit={guardar} aria-labelledby="titulo-datos">
      <h2 id="titulo-datos">Tus datos</h2>
      <div className="fila">
        <label className="campo">
          <span>Nombre *</span>
          <input className="control" value={datos.nombre} onChange={cambiar('nombre')} required maxLength={120} autoComplete="name" />
        </label>
        <label className="campo">
          <span>Teléfono</span>
          <input className="control" type="tel" value={datos.telefono} onChange={cambiar('telefono')} maxLength={40} autoComplete="tel" />
        </label>
      </div>
      {esPrestador && (
        <div className="fila">
          <label className="campo">
            <span>Dirección del establecimiento *</span>
            <input className="control" value={datos.direccion} onChange={cambiar('direccion')} required maxLength={200} />
          </label>
          <label className="campo">
            <span>Zona</span>
            <input className="control" value={datos.zona} onChange={cambiar('zona')} maxLength={80} placeholder="Rodadero, Minca, Taganga…" />
          </label>
        </div>
      )}
      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}
      <div className="fila-botones">
        <button className="btn btn-primario" disabled={enviando}>
          <Save size={16} aria-hidden="true" /> {enviando ? 'Guardando…' : 'Guardar cambios'}
        </button>
      </div>
    </form>
  )
}

// RF009: pide la contraseña actual; la nueva se guarda cifrada con BCrypt en el servidor.
function CambiarClave({ alCambiar }) {
  const VACIA = { actual: '', nueva: '', repetir: '' }
  const [claves, setClaves] = useState(VACIA)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const cambiar = (campo) => (e) => setClaves({ ...claves, [campo]: e.target.value })

  async function guardar(e) {
    e.preventDefault()
    if (claves.nueva !== claves.repetir) {
      setError('La nueva contraseña y su confirmación no coinciden.')
      return
    }
    setEnviando(true)
    setError(null)
    try {
      await cuentasApi.cambiarPassword(claves.actual, claves.nueva)
      setClaves(VACIA)
      alCambiar()
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="tarjeta pila" onSubmit={guardar} aria-labelledby="titulo-clave">
      <h2 id="titulo-clave">Cambiar contraseña</h2>
      <CampoClave etiqueta="Contraseña actual" value={claves.actual} onChange={cambiar('actual')} required autoComplete="current-password" />
      <div className="fila">
        <CampoClave etiqueta="Nueva contraseña" ayuda="mínimo 8" value={claves.nueva} onChange={cambiar('nueva')}
          required minLength={8} maxLength={72} autoComplete="new-password" />
        <CampoClave etiqueta="Repite la nueva" value={claves.repetir} onChange={cambiar('repetir')}
          required minLength={8} maxLength={72} autoComplete="new-password" />
      </div>
      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}
      <div className="fila-botones">
        <button className="btn btn-primario" disabled={enviando}>
          <KeyRound size={16} aria-hidden="true" /> {enviando ? 'Cambiando…' : 'Cambiar contraseña'}
        </button>
      </div>
    </form>
  )
}

export default function Perfil({ usuario: u, alActualizar }) {
  const [toast, setToast] = useState(null)
  const cerrarToast = useCallback(() => setToast(null), [])

  return (
    <div className="pila-lg" style={{ maxWidth: 720, margin: '0 auto' }}>
      <section className="tarjeta pila-lg">
        <div className="perfil-cabecera">
          <FotoPerfil usuario={u} alCambiar={(tieneFoto) => alActualizar({ ...u, tieneFoto })} alAvisar={setToast} />
          <div className="pila" style={{ gap: 6 }}>
            <h1>{u.nombre}</h1>
            <div className="chips">
              <span className="insignia tipo">{ROLES[u.rol] ?? u.rol}</span>
              <span className={u.estado === 'ACTIVO' ? 'insignia ok' : 'insignia apagada'}>{u.estado === 'ACTIVO' ? 'Cuenta activa' : u.estado}</span>
            </div>
          </div>
        </div>

        <AvisoValidacion usuario={u} />

        <div className="datos">
          <div className="dato"><Mail size={20} aria-hidden="true" /><div><small>Correo</small><span>{u.correo}</span></div></div>
          <div className="dato"><UserRound size={20} aria-hidden="true" /><div><small>Rol</small><span>{ROLES[u.rol] ?? u.rol}</span></div></div>
          <div className="dato"><Clock size={20} aria-hidden="true" /><div><small>Último acceso</small><span>{fecha(u.ultimoAcceso)}</span></div></div>
          {u.areaResponsable && (
            <div className="dato"><Briefcase size={20} aria-hidden="true" /><div><small>Área responsable</small><span>{u.areaResponsable}</span></div></div>
          )}
          {u.prestadorValidado && (
            <div className="dato"><BadgeCheck size={20} aria-hidden="true" /><div><small>Verificación</small><span>Prestador validado</span></div></div>
          )}
        </div>
      </section>

      <EditarDatos usuario={u} alGuardar={(nuevo) => { alActualizar({ ...nuevo, tieneFoto: u.tieneFoto }); setToast('Datos actualizados') }} />
      <CambiarClave alCambiar={() => setToast('Contraseña cambiada')} />

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
