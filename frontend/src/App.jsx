import { useEffect, useState } from 'react'
import { Compass, Contrast, LayoutDashboard, LogIn, LogOut, Menu, Moon, UserPlus, UserRound, X } from 'lucide-react'
import { cuentasApi, guardarToken } from './api/cliente'
import { guardarApariencia, leerApariencia } from './apariencia'
import Logo from './componentes/Logo'
import { iniciales } from './formato'
import Catalogo from './paginas/Catalogo'
import Login from './paginas/Login'
import MisServicios from './paginas/MisServicios'
import Perfil from './paginas/Perfil'
import RegistroTurista from './paginas/RegistroTurista'

export default function App() {
  const [usuario, setUsuario] = useState(null)
  const [vista, setVista] = useState('catalogo')
  const [aviso, setAviso] = useState(null)
  const [apariencia, setApariencia] = useState(leerApariencia)
  const [menuAbierto, setMenuAbierto] = useState(false)

  // Si ya hay un token guardado, recupera el perfil (el gateway lo valida).
  useEffect(() => {
    cuentasApi.perfil().then(setUsuario).catch(() => guardarToken(null))
  }, [])

  function cambiarApariencia(cambio) {
    const nueva = { ...apariencia, ...cambio }
    setApariencia(nueva)
    guardarApariencia(nueva)
  }

  function ir(destino) {
    setMenuAbierto(false)
    setVista(destino)
    window.scrollTo({ top: 0 })
  }

  function iniciar(u) {
    setUsuario(u)
    setAviso(null)
    ir(u.rol === 'PRESTADOR' ? 'mis-servicios' : 'catalogo')
  }

  function salir() {
    guardarToken(null)
    setUsuario(null)
    ir('catalogo')
  }

  const enlaces = [
    { id: 'catalogo', texto: 'Explorar', Icono: Compass },
    usuario?.rol === 'PRESTADOR' && { id: 'mis-servicios', texto: 'Mis servicios', Icono: LayoutDashboard },
  ].filter(Boolean)

  const actual = (id) => (vista === id ? 'page' : undefined)

  return (
    <div className="app">
      <a className="saltar" href="#contenido">Saltar al contenido</a>

      <header className="encabezado">
        <div className="contenedor">
          <button className="marca" onClick={() => ir('catalogo')} aria-label="SAMAR, ir al inicio">
            <Logo tamano={38} />
            <span>
              <strong>SAMAR</strong>
              <small>Turismo Santa Marta</small>
            </span>
          </button>

          <nav className="nav-principal" aria-label="Principal">
            {enlaces.map(({ id, texto, Icono }) => (
              <button key={id} aria-current={actual(id)} onClick={() => ir(id)}>
                <Icono size={18} aria-hidden="true" /> {texto}
              </button>
            ))}
          </nav>

          <div className="encabezado-acciones">
            <button className="interruptor-tema" aria-pressed={apariencia.altoContraste} title="Alto contraste"
              aria-label="Alto contraste" onClick={() => cambiarApariencia({ altoContraste: !apariencia.altoContraste })}>
              <Contrast size={19} aria-hidden="true" />
            </button>
            <button className="interruptor-tema" aria-pressed={apariencia.oscuro} title="Modo oscuro"
              aria-label="Modo oscuro" onClick={() => cambiarApariencia({ oscuro: !apariencia.oscuro })}>
              <Moon size={19} aria-hidden="true" />
            </button>

            {usuario ? (
              <>
                <button className="usuario-chip solo-escritorio" aria-current={actual('perfil')} onClick={() => ir('perfil')}>
                  <span className="avatar" aria-hidden="true">{iniciales(usuario.nombre)}</span>
                  <span className="nombre">{usuario.nombre}</span>
                </button>
                <button className="interruptor-tema solo-escritorio" title="Cerrar sesión" aria-label="Cerrar sesión" onClick={salir}>
                  <LogOut size={19} aria-hidden="true" />
                </button>
              </>
            ) : (
              <>
                <button className="btn btn-fantasma solo-escritorio" onClick={() => { setAviso(null); ir('login') }}>Ingresar</button>
                <button className="btn btn-primario solo-escritorio" onClick={() => ir('registro')}>Crear cuenta</button>
              </>
            )}

            <button className="interruptor-tema boton-menu" aria-label="Menú" aria-expanded={menuAbierto}
              aria-controls="menu-movil" onClick={() => setMenuAbierto(!menuAbierto)}>
              {menuAbierto ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
            </button>
          </div>
        </div>

        <div id="menu-movil" className="menu-movil" data-abierto={menuAbierto}>
          {enlaces.map(({ id, texto, Icono }) => (
            <button key={id} className="btn btn-fantasma" aria-current={actual(id)} onClick={() => ir(id)}>
              <Icono size={18} aria-hidden="true" /> {texto}
            </button>
          ))}
          {usuario ? (
            <>
              <button className="btn btn-fantasma" onClick={() => ir('perfil')}><UserRound size={18} aria-hidden="true" /> Mi perfil</button>
              <button className="btn btn-fantasma" onClick={salir}><LogOut size={18} aria-hidden="true" /> Cerrar sesión</button>
            </>
          ) : (
            <>
              <button className="btn btn-fantasma" onClick={() => { setAviso(null); ir('login') }}><LogIn size={18} aria-hidden="true" /> Ingresar</button>
              <button className="btn btn-fantasma" onClick={() => ir('registro')}><UserPlus size={18} aria-hidden="true" /> Crear cuenta</button>
            </>
          )}
        </div>
      </header>

      <main id="contenido" className="contenedor" tabIndex={-1}>
        {vista === 'catalogo' && <Catalogo />}
        {vista === 'mis-servicios' && usuario && <MisServicios usuario={usuario} />}
        {vista === 'perfil' && usuario && <Perfil usuario={usuario} />}
        {vista === 'login' && !usuario && (
          <Login aviso={aviso} alIniciar={iniciar} irARegistro={() => { setAviso(null); ir('registro') }} />
        )}
        {vista === 'registro' && !usuario && (
          <RegistroTurista
            alRegistrar={() => { setAviso('Tu cuenta fue creada. Ya puedes iniciar sesión.'); ir('login') }}
            irALogin={() => ir('login')}
          />
        )}
      </main>

      <footer className="pie">
        <div className="contenedor">
          <span><strong>SAMAR</strong> · Turismo integrado y sostenible en Santa Marta</span>
          <span>Proyecto de Arquitectura de Software · Universidad del Magdalena</span>
        </div>
      </footer>
    </div>
  )
}
