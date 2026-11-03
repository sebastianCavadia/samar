import { useEffect, useState } from 'react'
import { History, TriangleAlert } from 'lucide-react'
import { adminApi } from '../../api/cliente'
import Paginacion from '../../componentes/Paginacion'
import { ACCIONES_ADMIN, fecha } from '../../formato'

// RF097: todas las acciones de los administradores, con fecha, área y motivo.
export default function BitacoraAdmin() {
  const [pagina, setPagina] = useState(0)
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let vigente = true
    adminApi.bitacora(pagina)
      .then((r) => { if (vigente) setResultado(r) })
      .catch((e) => { if (vigente) setError(e.message) })
    return () => { vigente = false }
  }, [pagina])

  return (
    <section className="tarjeta" style={{ padding: 0 }} aria-label="Bitácora administrativa">
      {error && <div className="mensaje error" role="alert" style={{ margin: 16 }}><TriangleAlert size={18} aria-hidden="true" />{error}</div>}
      {resultado?.contenido.length === 0 && (
        <div className="vacio"><div className="icono-vacio"><History size={26} aria-hidden="true" /></div><h3>Todavía no hay acciones registradas</h3></div>
      )}
      <ul className="lista-bitacora">
        {resultado?.contenido.map((e) => (
          <li key={e.id}>
            <span className="suave pequeno">{fecha(e.fecha)}</span>
            <span><strong>{e.administrador}</strong>{e.areaResponsable && <span className="suave"> ({e.areaResponsable})</span>}</span>
            <span>{ACCIONES_ADMIN[e.accion]}: <strong>{e.usuario}</strong></span>
            {e.detalle && <span className="suave pequeno detalle">{e.detalle}</span>}
          </li>
        ))}
      </ul>
      {resultado && (
        <div style={{ padding: 12 }}>
          <Paginacion pagina={resultado.pagina} totalPaginas={resultado.totalPaginas} alCambiar={setPagina} />
        </div>
      )}
    </section>
  )
}
