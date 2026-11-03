import { useState } from 'react'
import { CircleCheck, LogIn, Mail, TriangleAlert } from 'lucide-react'
import { cuentasApi, guardarToken } from '../api/cliente'
import CampoClave from '../componentes/CampoClave'
import PanelAcceso from '../componentes/PanelAcceso'

export default function Login({ aviso, alIniciar, irARegistro, irARegistroPrestador }) {
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      const sesion = await cuentasApi.login(correo, password)
      guardarToken(sesion.token)
      alIniciar(sesion.usuario)
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <PanelAcceso>
      <div>
        <h1>Inicia sesión</h1>
        <p className="subtitulo">Bienvenido de nuevo a SAMAR.</p>
      </div>
      {aviso && <div className="mensaje exito" role="status"><CircleCheck size={20} aria-hidden="true" />{aviso}</div>}
      <form className="pila" onSubmit={enviar} noValidate={false}>
        <label className="campo">
          <span>Correo electrónico</span>
          <span className="con-icono">
            <Mail size={18} aria-hidden="true" />
            <input className="control" type="email" autoComplete="email" placeholder="tu@correo.com"
              value={correo} onChange={(e) => setCorreo(e.target.value)} required />
          </span>
        </label>
        <CampoClave autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        {error && <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>}
        <button type="submit" className="btn btn-primario btn-grande btn-bloque" disabled={enviando}>
          <LogIn size={18} aria-hidden="true" /> {enviando ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
      <div className="separador">¿Aún no tienes cuenta?</div>
      <button type="button" className="btn btn-secundario btn-bloque" onClick={irARegistro}>Crear cuenta de turista</button>
      <button type="button" className="btn-enlace" style={{ alignSelf: 'center' }} onClick={irARegistroPrestador}>
        ¿Tienes un hotel, restaurante o agencia? Registra tu negocio
      </button>
    </PanelAcceso>
  )
}
