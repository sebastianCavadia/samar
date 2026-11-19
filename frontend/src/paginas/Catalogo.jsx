import { useEffect, useState } from 'react'
import { Accessibility, Banknote, History, LayoutGrid, MapPin, RotateCcw, Salad, Search, SearchX, TriangleAlert, X } from 'lucide-react'
import { ofertaApi } from '../api/cliente'
import Paginacion from '../componentes/Paginacion'
import TarjetaServicio, { TarjetaEsqueleto } from '../componentes/TarjetaServicio'
import { CATEGORIAS, DIETAS, TIPOS, precio } from '../formato'
import { ICONO_CATEGORIA } from '../iconos'
import DetalleServicio from './DetalleServicio'

const FILTROS_VACIOS = { q: '', tipo: '', categoria: '', zona: '', precioMax: '', accesible: false, dieta: '' }
const TEXTO_VACIO = { q: '', zona: '', precioMax: '' }
const ZONAS = ['Rodadero', 'Taganga', 'Minca', 'Tayrona', 'Centro Histórico', 'Pozos Colorados', 'Bello Horizonte', 'Palomino']
const POR_PAGINA = 10

const textoLimpio = (b) => ({ q: b.q.trim(), zona: b.zona.trim(), precioMax: b.precioMax })

// RF083: las últimas 3 búsquedas quedan solo en este navegador (almacenamiento local), nunca en el servidor.
const CLAVE_RECIENTES = 'samar.busquedas'
const MAX_RECIENTES = 3

function leerRecientes() {
  try {
    const guardadas = JSON.parse(localStorage.getItem(CLAVE_RECIENTES))
    return Array.isArray(guardadas) ? guardadas.slice(0, MAX_RECIENTES) : []
  } catch {
    return []
  }
}

function conReciente(lista, texto) {
  const nueva = [texto, ...lista.filter((b) => b.toLowerCase() !== texto.toLowerCase())].slice(0, MAX_RECIENTES)
  try {
    localStorage.setItem(CLAVE_RECIENTES, JSON.stringify(nueva))
  } catch {
    // almacenamiento no disponible (modo privado): se recuerda solo mientras dure la página
  }
  return nueva
}

// RF073 buscar y filtrar, RF077 listar sin criterios, RF082 "Nuevo". Diseño basado en el mockup 4.
// El servicio abierto lo guarda App: así, tras iniciar sesión para reservar, se vuelve al mismo servicio.
export default function Catalogo({ usuario, servicioAbierto, abrirServicio, alIrALogin, alReservar }) {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [borrador, setBorrador] = useState(TEXTO_VACIO) // campos de texto, se aplican con retardo
  const [orden, setOrden] = useState('recientes')
  const [pagina, setPagina] = useState(0)
  const [resultado, setResultado] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [reintento, setReintento] = useState(0)
  const [recientes, setRecientes] = useState(leerRecientes)
  const [sugerir, setSugerir] = useState(false)

  // Se guarda la búsqueda terminada (al enviar o al salir del campo), no cada palabra a medio escribir.
  function recordar(texto) {
    const t = texto.trim()
    if (t) setRecientes((lista) => conReciente(lista, t))
  }

  function usarReciente(texto) {
    setBorrador((b) => ({ ...b, q: texto }))
    setFiltros((f) => ({ ...f, q: texto }))
    setPagina(0)
    setSugerir(false)
    recordar(texto)
  }

  // Busca mientras se escribe, pero sin mandar una petición por cada tecla.
  useEffect(() => {
    const limpio = textoLimpio(borrador)
    if (limpio.q === filtros.q && limpio.zona === filtros.zona && limpio.precioMax === filtros.precioMax) return undefined
    const t = setTimeout(() => {
      setFiltros((f) => ({ ...f, ...limpio }))
      setPagina(0)
    }, 350)
    return () => clearTimeout(t)
  }, [borrador, filtros.q, filtros.zona, filtros.precioMax])

  useEffect(() => {
    let vigente = true
    setCargando(true)
    ofertaApi.buscar({ ...filtros, orden, pagina })
      .then((r) => { if (vigente) { setResultado(r); setError(null) } })
      .catch((e) => { if (vigente) setError(e.message) })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [filtros, orden, pagina, reintento])

  function buscarYa(e) {
    e.preventDefault()
    setFiltros((f) => ({ ...f, ...textoLimpio(borrador) }))
    setPagina(0)
    setSugerir(false)
    recordar(borrador.q)
  }

  function cambiar(campo, valor) {
    setFiltros((f) => ({ ...f, [campo]: valor }))
    setPagina(0)
  }

  function quitar(campo) {
    if (campo in TEXTO_VACIO) setBorrador((b) => ({ ...b, [campo]: '' }))
    cambiar(campo, FILTROS_VACIOS[campo])
  }

  function limpiarTodo() {
    setBorrador(TEXTO_VACIO)
    setFiltros(FILTROS_VACIOS)
    setPagina(0)
  }

  if (servicioAbierto) {
    return (
      <DetalleServicio id={servicioAbierto} volver={() => abrirServicio(null)} usuario={usuario}
        alIrALogin={alIrALogin} alReservar={alReservar} />
    )
  }

  const escribir = (campo) => (e) => setBorrador({ ...borrador, [campo]: e.target.value })

  const activos = [
    filtros.q && ['q', `"${filtros.q}"`],
    filtros.tipo && ['tipo', TIPOS[filtros.tipo]],
    filtros.categoria && ['categoria', CATEGORIAS[filtros.categoria]],
    filtros.zona && ['zona', filtros.zona],
    filtros.precioMax && ['precioMax', `Hasta ${precio(filtros.precioMax)}`],
    filtros.accesible && ['accesible', 'Accesible'],
    filtros.dieta && ['dieta', `Apto ${DIETAS[filtros.dieta].toLowerCase()}`],
  ].filter(Boolean)

  const total = resultado?.totalElementos ?? 0
  const desde = resultado ? resultado.pagina * POR_PAGINA + 1 : 0
  const hasta = resultado ? resultado.pagina * POR_PAGINA + resultado.contenido.length : 0

  return (
    <div className="pila-lg">
      <section className="heroe" aria-labelledby="titulo-heroe">
        <h1 id="titulo-heroe">Descubre Santa Marta y cuídala mientras la vives</h1>
        <p>Hospedajes, tours y gastronomía local de prestadores verificados, con alternativas lejos de las zonas más congestionadas.</p>
        <form className="buscador" role="search" onSubmit={buscarYa}>
          <label className="con-icono">
            <span className="solo-lectores">Buscar servicios</span>
            <Search size={20} aria-hidden="true" />
            <input className="control" type="search" placeholder="¿Qué servicio, lugar o experiencia buscas?"
              value={borrador.q} onChange={escribir('q')} autoComplete="off"
              onFocus={() => setSugerir(true)} onBlur={() => { setSugerir(false); recordar(borrador.q) }} />
            {borrador.q && (
              <button type="button" className="btn btn-fantasma btn-icono accion-campo" aria-label="Borrar búsqueda"
                onClick={() => quitar('q')}>
                <X size={18} aria-hidden="true" />
              </button>
            )}
          </label>
          <button type="submit" className="btn btn-primario">Buscar</button>
          {sugerir && !borrador.q && recientes.length > 0 && (
            <ul className="sugerencias" aria-label="Búsquedas recientes">
              {recientes.map((b) => (
                <li key={b}>
                  {/* onMouseDown: elige antes de que el campo pierda el foco y cierre la lista */}
                  <button type="button" onMouseDown={(e) => { e.preventDefault(); usarReciente(b) }}>
                    <History size={16} aria-hidden="true" /> {b}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </form>
        <div className="chips" role="group" aria-label="Explorar por categoría">
          {Object.entries(CATEGORIAS).map(([valor, texto]) => {
            const Icono = ICONO_CATEGORIA[valor]
            const activo = filtros.categoria === valor
            return (
              <button key={valor} type="button" className="chip" aria-pressed={activo}
                onClick={() => cambiar('categoria', activo ? '' : valor)}>
                <Icono size={16} aria-hidden="true" /> {texto}
              </button>
            )
          })}
        </div>
      </section>

      <section className="pila" aria-labelledby="titulo-resultados">
        <div className="barra-filtros">
          <div className="caja-filtro">
            <LayoutGrid size={20} aria-hidden="true" />
            <label>
              Tipo
              <select value={filtros.tipo} onChange={(e) => cambiar('tipo', e.target.value)}>
                <option value="">Todos</option>
                {Object.entries(TIPOS).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
              </select>
            </label>
          </div>
          <div className="caja-filtro">
            <MapPin size={20} aria-hidden="true" />
            <label>
              Zona
              <input list="zonas" placeholder="Todas" value={borrador.zona} onChange={escribir('zona')} />
            </label>
            <datalist id="zonas">{ZONAS.map((z) => <option key={z} value={z} />)}</datalist>
          </div>
          <div className="caja-filtro">
            <Banknote size={20} aria-hidden="true" />
            <label>
              Precio máximo (COP)
              <input type="number" inputMode="numeric" min="0" step="10000" placeholder="Sin límite"
                value={borrador.precioMax} onChange={escribir('precioMax')} />
            </label>
          </div>
          <div className="caja-filtro">
            <Accessibility size={20} aria-hidden="true" />
            <label>
              Accesibilidad
              <select value={filtros.accesible ? 'si' : ''} onChange={(e) => cambiar('accesible', e.target.value === 'si')}>
                <option value="">Todos</option>
                <option value="si">Solo accesibles</option>
              </select>
            </label>
          </div>
          <div className="caja-filtro">
            <Salad size={20} aria-hidden="true" />
            <label>
              Dieta (restaurantes)
              <select value={filtros.dieta} onChange={(e) => cambiar('dieta', e.target.value)}>
                <option value="">Cualquiera</option>
                {Object.entries(DIETAS).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
              </select>
            </label>
          </div>
        </div>

        {activos.length > 0 && (
          <div className="filtros-activos" aria-label="Filtros aplicados">
            {activos.map(([campo, texto]) => (
              <button key={campo} className="chip quitar" onClick={() => quitar(campo)} aria-label={`Quitar filtro ${texto}`}>
                {texto} <X size={14} aria-hidden="true" />
              </button>
            ))}
            <button className="btn-enlace" onClick={limpiarTodo}>Limpiar todo</button>
          </div>
        )}

        <div className="cabecera-resultados">
          <div>
            <h2 id="titulo-resultados" style={{ display: 'inline' }}>Resultados</h2>
            <span className="suave" aria-live="polite">
              {resultado && (total === 0 ? 'Sin coincidencias' : `Mostrando ${desde}-${hasta} de ${total}`)}
            </span>
          </div>
          <label className="ordenar">
            Ordenar por
            <select className="control" value={orden} onChange={(e) => { setOrden(e.target.value); setPagina(0) }}>
              <option value="recientes">Más recientes</option>
              <option value="precio_asc">Menor precio</option>
              <option value="precio_desc">Mayor precio</option>
              <option value="calificacion">Mejor calificados</option>
            </select>
          </label>
        </div>

        {error && (
          <div className="mensaje error" role="alert">
            <TriangleAlert size={20} aria-hidden="true" />
            <div className="pila" style={{ gap: 8 }}>
              <span><strong>No pudimos cargar el catálogo</strong>{error}</span>
              <button className="btn btn-secundario" style={{ alignSelf: 'flex-start' }} onClick={() => setReintento((n) => n + 1)}>
                <RotateCcw size={16} aria-hidden="true" /> Reintentar
              </button>
            </div>
          </div>
        )}

        {!resultado && cargando && (
          <div className="grilla">{Array.from({ length: 8 }, (_, i) => <TarjetaEsqueleto key={i} />)}</div>
        )}

        {resultado && total === 0 && !error && (
          <div className="vacio">
            <div className="icono-vacio"><SearchX size={30} aria-hidden="true" /></div>
            <h3>No encontramos servicios con esos criterios</h3>
            <p>Prueba con otra palabra, otra zona o quita algunos filtros.</p>
            {activos.length > 0 && <button className="btn btn-secundario" onClick={limpiarTodo}>Limpiar filtros</button>}
          </div>
        )}

        {resultado && total > 0 && (
          <ul className="grilla" aria-busy={cargando} style={{ opacity: cargando ? 0.6 : 1, transition: 'opacity .2s' }}>
            {resultado.contenido.map((s) => (
              <li key={s.id}><TarjetaServicio servicio={s} alAbrir={abrirServicio} /></li>
            ))}
          </ul>
        )}

        {resultado && (
          <Paginacion pagina={resultado.pagina} totalPaginas={resultado.totalPaginas}
            alCambiar={(p) => { setPagina(p); document.getElementById('titulo-resultados')?.scrollIntoView({ behavior: 'smooth' }) }} />
        )}
      </section>
    </div>
  )
}
