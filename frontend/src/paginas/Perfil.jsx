import { BadgeCheck, Briefcase, Clock, Mail, UserRound } from 'lucide-react'
import AvisoValidacion from '../componentes/AvisoValidacion'
import { ROLES, fecha, iniciales } from '../formato'

export default function Perfil({ usuario: u }) {
  return (
    <div className="pila-lg" style={{ maxWidth: 720, margin: '0 auto' }}>
      <section className="tarjeta pila-lg">
        <div className="perfil-cabecera">
          <span className="avatar grande" aria-hidden="true">{iniciales(u.nombre)}</span>
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
    </div>
  )
}
