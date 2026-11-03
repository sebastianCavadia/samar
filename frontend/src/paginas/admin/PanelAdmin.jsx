import { useCallback, useEffect, useState } from 'react'
import { ShieldCheck, UserX, Users } from 'lucide-react'
import { adminApi } from '../../api/cliente'
import Toast from '../../componentes/Toast'
import BitacoraAdmin from './BitacoraAdmin'
import UsuariosAdmin from './UsuariosAdmin'
import ValidacionesAdmin from './ValidacionesAdmin'

const PESTANAS = [
  { id: 'validaciones', texto: 'Validaciones' },
  { id: 'usuarios', texto: 'Usuarios' },
  { id: 'bitacora', texto: 'Bitácora' },
]

// Administrar usuarios (RF091-RF100). Diseño basado en los mockups 2 y 3 del entregable.
export default function PanelAdmin({ usuario, alCambiarPendientes }) {
  const [pestana, setPestana] = useState('validaciones')
  const [resumen, setResumen] = useState(null)
  const [toast, setToast] = useState(null)
  const cerrarToast = useCallback(() => setToast(null), [])

  const cargarResumen = useCallback(() => {
    adminApi.resumen()
      .then((r) => { setResumen(r); alCambiarPendientes(r.prestadoresPendientes) })
      .catch(() => {})
  }, [alCambiarPendientes])

  useEffect(() => { cargarResumen() }, [cargarResumen])

  // Toda acción que cambia datos refresca los indicadores y muestra una confirmación.
  const alCambiar = useCallback((mensaje) => {
    setToast(mensaje)
    cargarResumen()
  }, [cargarResumen])

  return (
    <div className="pila-lg">
      <div className="encabezado-pagina">
        <div>
          <h1>Administración</h1>
          <p>{usuario.nombre}{usuario.areaResponsable && ` · Área: ${usuario.areaResponsable}`}</p>
        </div>
      </div>

      {resumen && (
        <div className="kpis">
          <div className="tarjeta kpi">
            <span className="icono-kpi"><ShieldCheck size={22} aria-hidden="true" /></span>
            <div><strong>{resumen.prestadoresPendientes}</strong><span>Prestadores por validar</span></div>
          </div>
          <div className="tarjeta kpi">
            <span className="icono-kpi"><Users size={22} aria-hidden="true" /></span>
            <div><strong>{resumen.usuarios}</strong><span>{resumen.turistas} turistas · {resumen.prestadores} prestadores · {resumen.administradores} admin.</span></div>
          </div>
          <div className="tarjeta kpi">
            <span className="icono-kpi"><UserX size={22} aria-hidden="true" /></span>
            <div><strong>{resumen.inactivos + resumen.bloqueados}</strong><span>{resumen.inactivos} inactivas · {resumen.bloqueados} bloqueadas</span></div>
          </div>
        </div>
      )}

      <div className="pestanas" role="tablist" aria-label="Secciones de administración">
        {PESTANAS.map((p) => (
          <button key={p.id} role="tab" aria-selected={pestana === p.id} onClick={() => setPestana(p.id)}>
            {p.texto}
            {p.id === 'validaciones' && resumen?.prestadoresPendientes > 0 && <span className="conteo aviso">{resumen.prestadoresPendientes}</span>}
          </button>
        ))}
      </div>

      <div role="tabpanel">
        {pestana === 'validaciones' && <ValidacionesAdmin alCambiar={alCambiar} />}
        {pestana === 'usuarios' && <UsuariosAdmin yo={usuario} alCambiar={alCambiar} />}
        {pestana === 'bitacora' && <BitacoraAdmin />}
      </div>

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
