import { useState } from 'react'
import { Eye, EyeOff, LockKeyhole } from 'lucide-react'

// Contraseña con botón para mostrarla: evita errores al escribir en el celular.
export default function CampoClave({ etiqueta = 'Contraseña', ayuda, ...props }) {
  const [visible, setVisible] = useState(false)
  return (
    <label className="campo">
      <span>{etiqueta}{ayuda && <span className="ayuda"> · {ayuda}</span>}</span>
      <span className="con-icono">
        <LockKeyhole size={18} aria-hidden="true" />
        <input className="control" type={visible ? 'text' : 'password'} style={{ paddingRight: 48 }} {...props} />
        <button type="button" className="btn btn-fantasma btn-icono accion-campo" onClick={() => setVisible(!visible)}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-pressed={visible}>
          {visible ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
        </button>
      </span>
    </label>
  )
}
