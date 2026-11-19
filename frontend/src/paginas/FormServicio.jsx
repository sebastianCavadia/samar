import { useRef, useState } from 'react'
import { ArrowLeft, ImageIcon, ImagePlus, Link2, Save, Star, Trash2, TriangleAlert } from 'lucide-react'
import { ofertaApi } from '../api/cliente'
import { CATEGORIAS, DESCRIPCION_TIPO, DIETAS, DIFICULTADES, TIPOS, errorImagen } from '../formato'
import { ICONO_CATEGORIA, ICONO_TIPO } from '../iconos'

const VACIO = {
  tipo: 'ACTIVIDAD', nombre: '', descripcion: '', modalidad: 'RESERVA', precioBase: '',
  direccion: '', zona: '', accesible: false, categorias: [], imagenes: [],
  tipoHabitacion: '', capacidadPersonas: '', horaCheckIn: '', horaCheckOut: '',
  duracionHoras: '', dificultad: 'MEDIA', edadMinima: '', horaInicio: '',
  tipoComida: '', aforo: '', horaApertura: '', horaCierre: '', dietas: [],
}

const texto = (v) => (v == null ? '' : String(v))
const nulo = (v) => (v === '' ? null : v)

const MAX_IMAGENES = 10

function desdeDetalle(d) {
  return {
    ...VACIO,
    tipo: d.tipo, nombre: d.nombre, descripcion: texto(d.descripcion), modalidad: d.modalidad,
    precioBase: texto(d.precioBase), direccion: texto(d.direccion), zona: texto(d.zona),
    accesible: d.accesible, categorias: d.categorias,
    imagenes: d.imagenes.map((i) => ({ url: i.url, descripcion: i.descripcion })),
    ...(d.hospedaje && {
      tipoHabitacion: texto(d.hospedaje.tipoHabitacion), capacidadPersonas: texto(d.hospedaje.capacidadPersonas),
      horaCheckIn: texto(d.hospedaje.horaCheckIn), horaCheckOut: texto(d.hospedaje.horaCheckOut),
    }),
    ...(d.actividad && {
      duracionHoras: texto(d.actividad.duracionHoras), dificultad: d.actividad.dificultad || 'MEDIA',
      edadMinima: texto(d.actividad.edadMinima), horaInicio: texto(d.actividad.horaInicio),
    }),
    ...(d.gastronomico && {
      tipoComida: texto(d.gastronomico.tipoComida), aforo: texto(d.gastronomico.aforo),
      horaApertura: texto(d.gastronomico.horaApertura), horaCierre: texto(d.gastronomico.horaCierre),
      dietas: d.gastronomico.dietas,
    }),
  }
}

function aRequest(f) {
  return {
    tipo: f.tipo, nombre: f.nombre, descripcion: nulo(f.descripcion), modalidad: f.modalidad,
    precioBase: Number(f.precioBase), direccion: f.direccion, zona: f.zona, accesible: f.accesible,
    categorias: f.categorias,
    imagenes: f.imagenes.map((i) => ({ url: i.url.trim(), descripcion: i.descripcion.trim() })),
    hospedaje: f.tipo !== 'HOSPEDAJE' ? null : {
      tipoHabitacion: nulo(f.tipoHabitacion), capacidadPersonas: Number(f.capacidadPersonas),
      horaCheckIn: nulo(f.horaCheckIn), horaCheckOut: nulo(f.horaCheckOut),
    },
    actividad: f.tipo !== 'ACTIVIDAD' ? null : {
      duracionHoras: nulo(f.duracionHoras), dificultad: f.dificultad,
      edadMinima: nulo(f.edadMinima), horaInicio: nulo(f.horaInicio),
    },
    gastronomico: f.tipo !== 'GASTRONOMICO' ? null : {
      tipoComida: nulo(f.tipoComida), aforo: nulo(f.aforo),
      horaApertura: nulo(f.horaApertura), horaCierre: nulo(f.horaCierre), dietas: f.dietas,
    },
  }
}

function Campo({ etiqueta, ayuda, children, ancho }) {
  return (
    <label className="campo" style={ancho ? { flex: ancho } : undefined}>
      <span>{etiqueta}{ayuda && <span className="ayuda"> · {ayuda}</span>}</span>
      {children}
    </label>
  )
}

function Miniatura({ url }) {
  const [rota, setRota] = useState(false)
  if (rota) {
    return (
      <div className="miniatura sin-foto" role="img" aria-label="No pudimos cargar esta imagen">
        <ImageIcon size={26} aria-hidden="true" />
      </div>
    )
  }
  return <img className="miniatura" src={url} alt="" onError={() => setRota(true)} />
}

// RF019 subir imágenes (JPEG o PNG, hasta 2 MB) o enlazarlas, y quitarlas. RF021: cada una lleva su
// descripción para lectores de pantalla. La primera es la foto principal de la tarjeta del catálogo.
function Galeria({ imagenes, alCambiar }) {
  const [enlace, setEnlace] = useState('')
  const [subiendo, setSubiendo] = useState(false)
  const [error, setError] = useState(null)
  const entrada = useRef(null)
  const llena = imagenes.length >= MAX_IMAGENES

  const agregar = (url) => alCambiar([...imagenes, { url, descripcion: '' }])
  const actualizar = (i, cambio) => alCambiar(imagenes.map((img, j) => (j === i ? { ...img, ...cambio } : img)))
  const quitar = (i) => alCambiar(imagenes.filter((_, j) => j !== i))
  const hacerPrincipal = (i) => alCambiar([imagenes[i], ...imagenes.filter((_, j) => j !== i)])

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
    setSubiendo(true)
    try {
      agregar((await ofertaApi.subirImagen(archivo)).url)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubiendo(false)
    }
  }

  function agregarEnlace() {
    const url = enlace.trim()
    if (!/^https?:\/\/.+/.test(url)) {
      setError('El enlace debe empezar por https://')
      return
    }
    setError(null)
    agregar(url)
    setEnlace('')
  }

  return (
    <div className="pila">
      {imagenes.length > 0 && (
        <ul className="galeria-form">
          {imagenes.map((img, i) => (
            <li key={`${img.url}-${i}`} className="item-galeria">
              <Miniatura url={img.url} />
              <div className="pila" style={{ gap: 8, flex: 1, minWidth: 0 }}>
                {i === 0 && <span className="insignia ok" style={{ alignSelf: 'flex-start' }}><Star size={13} aria-hidden="true" /> Principal</span>}
                <label className="campo">
                  <span>Descripción de la foto * <span className="ayuda">· la leen los lectores de pantalla</span></span>
                  <input className="control" value={img.descripcion} required maxLength={200}
                    onChange={(e) => actualizar(i, { descripcion: e.target.value })}
                    placeholder="Ej.: Playa de Cabo San Juan al amanecer" />
                </label>
                <div className="fila-botones">
                  {i > 0 && (
                    <button type="button" className="btn btn-fantasma" onClick={() => hacerPrincipal(i)}>
                      <Star size={15} aria-hidden="true" /> Hacer principal
                    </button>
                  )}
                  <button type="button" className="btn btn-fantasma" onClick={() => quitar(i)} aria-label={`Quitar la foto ${i + 1}`}>
                    <Trash2 size={15} aria-hidden="true" /> Quitar
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      {imagenes.length === 0 && (
        <div className="vacio-galeria suave"><ImageIcon size={26} aria-hidden="true" /> Aún no hay fotos. Los servicios con fotos reciben más reservas.</div>
      )}

      {llena
        ? <p className="suave pequeno">Llegaste al máximo de {MAX_IMAGENES} fotos.</p>
        : (
          <div className="fila" style={{ alignItems: 'flex-end' }}>
            <div className="pila" style={{ gap: 6, flex: '0 0 auto' }}>
              <input ref={entrada} type="file" accept="image/jpeg,image/png" hidden onChange={subir} />
              <button type="button" className="btn btn-secundario" disabled={subiendo} onClick={() => entrada.current?.click()}>
                <ImagePlus size={16} aria-hidden="true" /> {subiendo ? 'Subiendo…' : 'Subir foto'}
              </button>
              <span className="suave pequeno">JPEG o PNG, máximo 2 MB</span>
            </div>
            <label className="campo" style={{ flex: '1 1 260px' }}>
              <span>O pega un enlace</span>
              <input className="control" type="url" value={enlace} onChange={(e) => setEnlace(e.target.value)} placeholder="https://…"
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); agregarEnlace() } }} />
            </label>
            <button type="button" className="btn btn-fantasma" onClick={agregarEnlace} disabled={!enlace.trim()}>
              <Link2 size={16} aria-hidden="true" /> Agregar enlace
            </button>
          </div>
        )}

      {error && <div className="mensaje error" role="alert"><TriangleAlert size={18} aria-hidden="true" />{error}</div>}
    </div>
  )
}

// RF016 registrar / RF017 editar. "detalle" viene solo al editar.
export default function FormServicio({ detalle, alGuardar, cancelar }) {
  const [f, setF] = useState(detalle ? desdeDetalle(detalle) : VACIO)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (campo) => (e) =>
    setF({ ...f, [campo]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })
  const alternar = (campo, valor) =>
    setF({ ...f, [campo]: f[campo].includes(valor) ? f[campo].filter((x) => x !== valor) : [...f[campo], valor] })

  async function enviar(e) {
    e.preventDefault()
    if (f.categorias.length === 0) {
      setError('Elige al menos una categoría: así los turistas encuentran tu servicio al filtrar.')
      return
    }
    setError(null)
    setEnviando(true)
    try {
      const guardado = detalle
        ? await ofertaApi.editar(detalle.id, aRequest(f))
        : await ofertaApi.crear(aRequest(f))
      alGuardar(guardado)
    } catch (err) {
      setError(err.message)
      setEnviando(false)
    }
  }

  return (
    <div className="pila-lg" style={{ maxWidth: 860, margin: '0 auto' }}>
      <div className="encabezado-pagina">
        <div>
          <button type="button" className="btn-enlace" onClick={cancelar} style={{ display: 'inline-flex', gap: 6, alignItems: 'center' }}>
            <ArrowLeft size={16} aria-hidden="true" /> Mis servicios
          </button>
          <h1 style={{ marginTop: 6 }}>{detalle ? 'Editar servicio' : 'Nuevo servicio'}</h1>
          <p>Los campos con * son obligatorios. Lo que escribas aquí es lo que verán los turistas.</p>
        </div>
      </div>

      <form className="tarjeta pila-lg" onSubmit={enviar}>
        <section className="seccion-form">
          <h3><span className="numero-paso">1</span>¿Qué ofreces? *</h3>
          <div className="selector-tipo" role="group" aria-label="Tipo de servicio">
            {Object.entries(TIPOS).map(([valor, etiqueta]) => {
              const Icono = ICONO_TIPO[valor]
              return (
                <button key={valor} type="button" className="opcion-tipo" aria-pressed={f.tipo === valor}
                  disabled={Boolean(detalle) && f.tipo !== valor} onClick={() => setF({ ...f, tipo: valor })}>
                  <Icono size={26} aria-hidden="true" />
                  <strong>{etiqueta}</strong>
                  <span>{DESCRIPCION_TIPO[valor]}</span>
                </button>
              )
            })}
          </div>
          {detalle && <p className="suave pequeno">El tipo no se puede cambiar después de registrar el servicio.</p>}
        </section>

        <section className="seccion-form">
          <h3><span className="numero-paso">2</span>Información básica</h3>
          <Campo etiqueta="Nombre del servicio *">
            <input className="control" value={f.nombre} onChange={cambiar('nombre')} maxLength={150} required
              placeholder="Ej.: Caminata al Parque Tayrona" />
          </Campo>
          <Campo etiqueta="Descripción" ayuda="qué incluye, qué llevar, qué lo hace especial">
            <textarea className="control" rows={4} value={f.descripcion} onChange={cambiar('descripcion')} maxLength={4000} />
          </Campo>
          <div className="fila">
            <Campo etiqueta="Precio base (COP) *">
              <input className="control" type="number" inputMode="numeric" min="0" step="any" value={f.precioBase}
                onChange={cambiar('precioBase')} required placeholder="180000" />
            </Campo>
            <Campo etiqueta="Modalidad *">
              <select className="control" value={f.modalidad} onChange={cambiar('modalidad')}>
                <option value="RESERVA">Con reserva previa</option>
                <option value="WALK_IN">Sin reserva (atención directa)</option>
              </select>
            </Campo>
          </div>
        </section>

        <section className="seccion-form">
          <h3><span className="numero-paso">3</span>Ubicación</h3>
          <div className="fila">
            <Campo etiqueta="Zona *" ancho="1 1 180px">
              <input className="control" value={f.zona} onChange={cambiar('zona')} required placeholder="Rodadero, Minca, Taganga…" />
            </Campo>
            <Campo etiqueta="Dirección *" ancho="2 1 260px">
              <input className="control" value={f.direccion} onChange={cambiar('direccion')} required />
            </Campo>
          </div>
        </section>

        <section className="seccion-form">
          <h3><span className="numero-paso">4</span>Categorías y accesibilidad *</h3>
          <div className="chips" role="group" aria-label="Categorías">
            {Object.entries(CATEGORIAS).map(([valor, etiqueta]) => {
              const Icono = ICONO_CATEGORIA[valor]
              return (
                <button key={valor} type="button" className="chip" aria-pressed={f.categorias.includes(valor)}
                  onClick={() => alternar('categorias', valor)}>
                  <Icono size={16} aria-hidden="true" /> {etiqueta}
                </button>
              )
            })}
          </div>
          <label className="casilla">
            <input type="checkbox" checked={f.accesible} onChange={cambiar('accesible')} />
            Es accesible para personas con movilidad reducida
          </label>
        </section>

        <section className="seccion-form">
          <h3><span className="numero-paso">5</span>Detalles de {TIPOS[f.tipo].toLowerCase()}</h3>
          {f.tipo === 'HOSPEDAJE' && (
            <>
              <div className="fila">
                <Campo etiqueta="Capacidad (personas) *">
                  <input className="control" type="number" min="1" value={f.capacidadPersonas} onChange={cambiar('capacidadPersonas')} required />
                </Campo>
                <Campo etiqueta="Tipo de habitación">
                  <input className="control" value={f.tipoHabitacion} onChange={cambiar('tipoHabitacion')} placeholder="Doble, suite…" />
                </Campo>
              </div>
              <div className="fila">
                <Campo etiqueta="Check-in"><input className="control" type="time" value={f.horaCheckIn} onChange={cambiar('horaCheckIn')} /></Campo>
                <Campo etiqueta="Check-out"><input className="control" type="time" value={f.horaCheckOut} onChange={cambiar('horaCheckOut')} /></Campo>
              </div>
            </>
          )}
          {f.tipo === 'ACTIVIDAD' && (
            <>
              <div className="fila">
                <Campo etiqueta="Duración (horas)">
                  <input className="control" type="number" min="0.5" step="0.5" value={f.duracionHoras} onChange={cambiar('duracionHoras')} />
                </Campo>
                <Campo etiqueta="Dificultad">
                  <select className="control" value={f.dificultad} onChange={cambiar('dificultad')}>
                    {Object.entries(DIFICULTADES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                  </select>
                </Campo>
              </div>
              <div className="fila">
                <Campo etiqueta="Edad mínima" ayuda="se valida al reservar">
                  <input className="control" type="number" min="0" max="99" value={f.edadMinima} onChange={cambiar('edadMinima')} />
                </Campo>
                <Campo etiqueta="Hora de inicio"><input className="control" type="time" value={f.horaInicio} onChange={cambiar('horaInicio')} /></Campo>
              </div>
            </>
          )}
          {f.tipo === 'GASTRONOMICO' && (
            <>
              <div className="fila">
                <Campo etiqueta="Tipo de cocina">
                  <input className="control" value={f.tipoComida} onChange={cambiar('tipoComida')} placeholder="Mariscos, caribeña…" />
                </Campo>
                <Campo etiqueta="Aforo"><input className="control" type="number" min="1" value={f.aforo} onChange={cambiar('aforo')} /></Campo>
              </div>
              <div className="fila">
                <Campo etiqueta="Apertura"><input className="control" type="time" value={f.horaApertura} onChange={cambiar('horaApertura')} /></Campo>
                <Campo etiqueta="Cierre"><input className="control" type="time" value={f.horaCierre} onChange={cambiar('horaCierre')} /></Campo>
              </div>
              <div className="pila" style={{ gap: 8 }}>
                <span style={{ fontSize: 14, fontWeight: 600 }}>Opciones dietéticas certificadas</span>
                <div className="chips" role="group" aria-label="Dietas">
                  {Object.entries(DIETAS).map(([v, t]) => (
                    <button key={v} type="button" className="chip" aria-pressed={f.dietas.includes(v)} onClick={() => alternar('dietas', v)}>{t}</button>
                  ))}
                </div>
              </div>
            </>
          )}
        </section>

        <section className="seccion-form">
          <h3><span className="numero-paso">6</span>Fotos <span className="suave pequeno" style={{ fontWeight: 400 }}>· hasta {MAX_IMAGENES}</span></h3>
          <Galeria imagenes={f.imagenes} alCambiar={(imagenes) => setF({ ...f, imagenes })} />
        </section>

        {error && <div className="mensaje error" role="alert"><TriangleAlert size={20} aria-hidden="true" />{error}</div>}

        <div className="barra-acciones">
          <button type="button" className="btn btn-secundario" onClick={cancelar}>Cancelar</button>
          <button type="submit" className="btn btn-primario" disabled={enviando}>
            <Save size={18} aria-hidden="true" /> {enviando ? 'Guardando…' : detalle ? 'Guardar cambios' : 'Publicar servicio'}
          </button>
        </div>
      </form>
    </div>
  )
}
