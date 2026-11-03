import { useState } from 'react'
import { Mail, TriangleAlert, UserPlus, UserRound } from 'lucide-react'
import { cuentasApi } from '../api/cliente'
import CampoClave from '../componentes/CampoClave'
import PanelAcceso from '../componentes/PanelAcceso'

const VACIO = {
  nombre: '', correo: '', password: '', tipoDocumento: 'CC', numeroDocumento: '',
  nacionalidad: '', paisResidencia: '', fechaNacimiento: '', discapacidad: false,
}

// RF001 + RF002: registro del turista.
export default function RegistroTurista({ alRegistrar, irALogin }) {
  const [datos, setDatos] = useState(VACIO)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (campo) => (e) =>
    setDatos({ ...datos, [campo]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  async function enviar(e) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await cuentasApi.registrarTurista({ ...datos, fechaNacimiento: datos.fechaNacimiento || null })
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
        <h1>Crea tu cuenta</h1>
        <p className="subtitulo">Regístrate como turista para reservar y recibir recomendaciones.</p>
      </div>
      <form className="pila" onSubmit={enviar}>
        <label className="campo">
          <span>Nombre completo</span>
          <span className="con-icono">
            <UserRound size={18} aria-hidden="true" />
            <input className="control" autoComplete="name" value={datos.nombre} onChange={cambiar('nombre')} required maxLength={120} />
          </span>
        </label>
        <label className="campo">
          <span>Correo electrónico</span>
          <span className="con-icono">
            <Mail size={18} aria-hidden="true" />
            <input className="control" type="email" autoComplete="email" placeholder="tu@correo.com"
              value={datos.correo} onChange={cambiar('correo')} required />
          </span>
        </label>
        <CampoClave ayuda="mínimo 8 caracteres" autoComplete="new-password" minLength={8}
          value={datos.password} onChange={cambiar('password')} required />

        <div className="fila">
          <label className="campo" style={{ flex: '0 1 140px' }}>
            <span>Documento</span>
            <select className="control" value={datos.tipoDocumento} onChange={cambiar('tipoDocumento')}>
              <option value="CC">Cédula (CC)</option>
              <option value="CE">Cédula de extranjería</option>
              <option value="PASAPORTE">Pasaporte</option>
            </select>
          </label>
          <label className="campo">
            <span>Número</span>
            <input className="control" inputMode="text" value={datos.numeroDocumento} onChange={cambiar('numeroDocumento')} required maxLength={40} />
          </label>
        </div>
        <div className="fila">
          <label className="campo">
            <span>Nacionalidad <span className="ayuda">· opcional</span></span>
            <input className="control" value={datos.nacionalidad} onChange={cambiar('nacionalidad')} />
          </label>
          <label className="campo">
            <span>País de residencia <span className="ayuda">· opcional</span></span>
            <input className="control" autoComplete="country-name" value={datos.paisResidencia} onChange={cambiar('paisResidencia')} />
          </label>
        </div>
        <label className="campo">
          <span>Fecha de nacimiento <span className="ayuda">· la usamos para actividades con edad mínima</span></span>
          <input className="control" type="date" autoComplete="bday" value={datos.fechaNacimiento} onChange={cambiar('fechaNacimiento')} />
        </label>
        <label className="casilla">
          <input type="checkbox" checked={datos.discapacidad} onChange={cambiar('discapacidad')} />
          Tengo alguna discapacidad y quiero ver opciones accesibles
        </label>

        {error && <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>}
        <button type="submit" className="btn btn-primario btn-grande btn-bloque" disabled={enviando}>
          <UserPlus size={18} aria-hidden="true" /> {enviando ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
        <p className="suave pequeno">Tus datos se tratan según la Ley 1581 de 2012 de protección de datos personales.</p>
      </form>
      <div className="separador">¿Ya tienes cuenta?</div>
      <button type="button" className="btn btn-secundario btn-bloque" onClick={irALogin}>Iniciar sesión</button>
    </PanelAcceso>
  )
}
