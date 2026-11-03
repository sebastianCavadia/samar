import { Ban, ShieldAlert } from 'lucide-react'

// RF095: el prestador ve si su solicitud sigue en revisión o fue rechazada (con el motivo).
export default function AvisoValidacion({ usuario }) {
  if (usuario.estadoValidacion === 'PENDIENTE') {
    return (
      <div className="mensaje aviso">
        <ShieldAlert size={20} aria-hidden="true" />
        <span><strong>Tu cuenta está en revisión</strong>Un administrador está validando tu NIT y RNT. Podrás publicar servicios cuando termine.</span>
      </div>
    )
  }
  if (usuario.estadoValidacion === 'RECHAZADO') {
    return (
      <div className="mensaje error">
        <Ban size={20} aria-hidden="true" />
        <span><strong>Tu solicitud fue rechazada</strong>{usuario.motivoRechazo} Escribe a la administración de SAMAR para corregir tu documentación.</span>
      </div>
    )
  }
  return null
}
