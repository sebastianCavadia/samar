import { useState } from 'react'
import { Building2, FileBadge, Mail, MapPin, Phone, Store, TriangleAlert } from 'lucide-react'
import { cuentasApi } from '../api/cliente'
import CampoClave from '../componentes/CampoClave'
import PanelAcceso from '../componentes/PanelAcceso'
import { TIPOS_PRESTADOR } from '../formato'

const IDIOMAS = ['Español', 'Inglés', 'Francés', 'Alemán', 'Portugués', 'Italiano', 'Lengua de señas']

const VACIO = {
  tipoPrestador: 'HOTEL', nombre: '', correo: '', password: '', telefono: '',
  nit: '', rnt: '', direccion: '', zona: '', idiomasGuia: [],
}

// RF001 + RF003: registro de hoteles, restaurantes y agencias. Queda pendiente de validación (RF095).
export default function RegistroPrestador({ alRegistrar, irALogin, irARegistroTurista }) {
  const [datos, setDatos] = useState(VACIO)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (campo) => (e) => setDatos({ ...datos, [campo]: e.target.value })
  const alternarIdioma = (i) => setDatos({
    ...datos, idiomasGuia: datos.idiomasGuia.includes(i) ? datos.idiomasGuia.filter((x) => x !== i) : [...datos.idiomasGuia, i],
  })

  async function enviar(e) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await cuentasApi.registrarPrestador({
        ...datos,
        telefono: datos.telefono || null,
        zona: datos.zona || null,
        idiomasGuia: datos.tipoPrestador === 'AGENCIA_TURISTICA' ? datos.idiomasGuia : null,
      })
      alRegistrar()
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <PanelAcceso>
      <div>
        <h1>Registra tu negocio</h1>
        <p className="subtitulo">Publica tus servicios en SAMAR cuando validemos tu documentación.</p>
      </div>
      <div className="selector-cuenta" role="group" aria-label="Tipo de cuenta">
        <button type="button" onClick={irARegistroTurista}>Soy turista</button>
        <button type="button" aria-pressed="true">Tengo un negocio</button>
      </div>
      <form className="pila" onSubmit={enviar}>
        <label className="campo">
          <span>Tipo de negocio</span>
          <span className="con-icono">
            <Store size={18} aria-hidden="true" />
            <select className="control" value={datos.tipoPrestador} onChange={cambiar('tipoPrestador')}>
              {Object.entries(TIPOS_PRESTADOR).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
            </select>
          </span>
        </label>
        <label className="campo">
          <span>Nombre del negocio</span>
          <span className="con-icono">
            <Building2 size={18} aria-hidden="true" />
            <input className="control" value={datos.nombre} onChange={cambiar('nombre')} required maxLength={120} autoComplete="organization" />
          </span>
        </label>
        <div className="fila">
          <label className="campo">
            <span>Correo electrónico</span>
            <span className="con-icono">
              <Mail size={18} aria-hidden="true" />
              <input className="control" type="email" autoComplete="email" value={datos.correo} onChange={cambiar('correo')} required />
            </span>
          </label>
          <label className="campo">
            <span>Teléfono <span className="ayuda">· opcional</span></span>
            <span className="con-icono">
              <Phone size={18} aria-hidden="true" />
              <input className="control" type="tel" autoComplete="tel" value={datos.telefono} onChange={cambiar('telefono')} maxLength={40} />
            </span>
          </label>
        </div>
        <CampoClave ayuda="mínimo 8 caracteres" autoComplete="new-password" minLength={8}
          value={datos.password} onChange={cambiar('password')} required />

        <fieldset className="grupo-campos">
          <legend><FileBadge size={16} aria-hidden="true" /> Documentación que revisará el administrador</legend>
          <div className="fila">
            <label className="campo">
              <span>NIT</span>
              <input className="control" value={datos.nit} onChange={cambiar('nit')} required maxLength={40} placeholder="900123456-7" />
            </label>
            <label className="campo">
              <span>RNT <span className="ayuda">· Registro Nacional de Turismo</span></span>
              <input className="control" value={datos.rnt} onChange={cambiar('rnt')} required maxLength={40} />
            </label>
          </div>
        </fieldset>

        <div className="fila">
          <label className="campo" style={{ flex: '2 1 240px' }}>
            <span>Dirección del establecimiento</span>
            <span className="con-icono">
              <MapPin size={18} aria-hidden="true" />
              <input className="control" value={datos.direccion} onChange={cambiar('direccion')} required maxLength={200} autoComplete="street-address" />
            </span>
          </label>
          <label className="campo">
            <span>Zona <span className="ayuda">· opcional</span></span>
            <input className="control" value={datos.zona} onChange={cambiar('zona')} placeholder="Rodadero, Taganga…" maxLength={80} />
          </label>
        </div>

        {datos.tipoPrestador === 'AGENCIA_TURISTICA' && (
          <div className="pila" style={{ gap: 8 }}>
            <span style={{ fontSize: 14, fontWeight: 600 }}>Idiomas de tus guías</span>
            <div className="chips" role="group" aria-label="Idiomas de los guías">
              {IDIOMAS.map((i) => (
                <button key={i} type="button" className="chip" aria-pressed={datos.idiomasGuia.includes(i)} onClick={() => alternarIdioma(i)}>{i}</button>
              ))}
            </div>
          </div>
        )}

        {error && <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>}
        <button type="submit" className="btn btn-primario btn-grande btn-bloque" disabled={enviando}>
          {enviando ? 'Enviando…' : 'Enviar solicitud'}
        </button>
        <p className="suave pequeno">Un administrador revisará tu NIT y RNT. Mientras tanto podrás entrar, pero tus servicios no serán públicos.</p>
      </form>
      <div className="separador">¿Ya tienes cuenta?</div>
      <button type="button" className="btn btn-secundario btn-bloque" onClick={irALogin}>Iniciar sesión</button>
    </PanelAcceso>
  )
}
