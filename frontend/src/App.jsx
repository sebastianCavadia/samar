import { useCallback, useEffect, useState } from 'react'
import {
  CalendarCheck, Compass, Contrast, Inbox, LayoutDashboard, LogIn, LogOut, Menu, Moon, ShieldCheck, UserPlus, UserRound, X,
} from 'lucide-react'
import { adminApi, cuentasApi, guardarToken } from './api/cliente'
import { guardarApariencia, leerApariencia } from './apariencia'
import Logo from './componentes/Logo'
import Toast from './componentes/Toast'
import { iniciales } from './formato'
import Catalogo from './paginas/Catalogo'
import Login from './paginas/Login'
import MisReservas from './paginas/MisReservas'
import MisServicios from './paginas/MisServicios'
import Perfil from './paginas/Perfil'
import RegistroPrestador from './paginas/RegistroPrestador'
import RegistroTurista from './paginas/RegistroTurista'
import ReservasRecibidas from './paginas/ReservasRecibidas'
import PanelAdmin from './paginas/admin/PanelAdmin'

export default function App() {
  const [usuario, setUsuario] = useState(null)
  const [vista, setVista] = useState('catalogo')
  const [aviso, setAviso] = useState(null)
  const [apariencia, setApariencia] = useState(leerApariencia)
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [servicioAbierto, setServicioAbierto] = useState(null)
  const [reservaNueva, setReservaNueva] = useState(null)
  const [pendientesAdmin, setPendientesAdmin] = useState(0)
  const [toast, setToast] = useState(null)
  const cerrarToast = useCallback(() => setToast(null), [])

  // Si ya hay un token guardado, recupera el perfil (el gateway lo valida).
  useEffect(() => {
    cuentasApi.perfil().then(setUsuario).catch(() => guardarToken(null))
  }, [])

  // RF096: al entrar un administrador se carga cuántos prestadores esperan validación.
  useEffect(() => {
    if (usuario?.rol !== 'ADMINISTRADOR') return
    adminApi.resumen().then((r) => setPendientesAdmin(r.prestadoresPendientes)).catch(() => {})
  }, [usuario])

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

  /** Desde la navegación, "Explorar" siempre vuelve a la lista (cierra el servicio abierto). */
  function irA(destino) {
    if (destino === 'catalogo') setServicioAbierto(null)
    ir(destino)
  }

  function iniciar(u) {
    setUsuario(u)
    setAviso(null)
    // Si entró para reservar, vuelve al servicio que estaba viendo.
    if (servicioAbierto && u.rol === 'TURISTA') ir('catalogo')
    else irA({ PRESTADOR: 'mis-servicios', ADMINISTRADOR: 'admin' }[u.rol] ?? 'catalogo')
  }

  function salir() {
    guardarToken(null)
    setUsuario(null)
    irA('catalogo')
  }

  function alReservar(reserva) {
    setReservaNueva(reserva.id)
    setServicioAbierto(null)
    setToast('Reserva creada: págala para que el prestador pueda confirmarla')
    ir('mis-reservas')
  }

  const enlaces = [
    { id: 'catalogo', texto: 'Explorar', Icono: Compass },
    usuario?.rol === 'TURISTA' && { id: 'mis-reservas', texto: 'Mis reservas', Icono: CalendarCheck },
    usuario?.rol === 'PRESTADOR' && { id: 'mis-servicios', texto: 'Mis servicios', Icono: LayoutDashboard },
    usuario?.rol === 'PRESTADOR' && { id: 'reservas-recibidas', texto: 'Reservas', Icono: Inbox },
    // RF096: el contador avisa al administrador que hay prestadores esperando validación.
    usuario?.rol === 'ADMINISTRADOR' && { id: 'admin', texto: 'Administración', Icono: ShieldCheck, conteo: pendientesAdmin },
  ].filter(Boolean)

  const actual = (id) => (vista === id ? 'page' : undefined)

  return (
    <div className="app">
      <a className="saltar" href="#contenido">Saltar al contenido</a>

      <header className="encabezado">
        <div className="contenedor">
          <button className="marca" onClick={() => irA('catalogo')} aria-label="SAMAR, ir al inicio">
            <Logo tamano={38} />
            <span>
              <strong>SAMAR</strong>
              <small>Turismo Santa Marta</small>
            </span>
          </button>

          <nav className="nav-principal" aria-label="Principal">
            {enlaces.map(({ id, texto, Icono, conteo }) => (
              <button key={id} aria-current={actual(id)} onClick={() => irA(id)}>
                <Icono size={18} aria-hidden="true" /> {texto}
                {conteo > 0 && <span className="conteo aviso" aria-label={`${conteo} pendientes`}>{conteo}</span>}
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
          {enlaces.map(({ id, texto, Icono, conteo }) => (
            <button key={id} className="btn btn-fantasma" aria-current={actual(id)} onClick={() => irA(id)}>
              <Icono size={18} aria-hidden="true" /> {texto}
              {conteo > 0 && <span className="conteo aviso" aria-label={`${conteo} pendientes`}>{conteo}</span>}
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
        {vista === 'catalogo' && (
          <Catalogo usuario={usuario} servicioAbierto={servicioAbierto} abrirServicio={setServicioAbierto}
            alIrALogin={() => { setAviso(null); ir('login') }} alReservar={alReservar} />
        )}
        {vista === 'mis-reservas' && usuario && <MisReservas resaltar={reservaNueva} />}
        {vista === 'mis-servicios' && usuario && <MisServicios usuario={usuario} />}
        {vista === 'reservas-recibidas' && usuario && <ReservasRecibidas />}
        {vista === 'perfil' && usuario && <Perfil usuario={usuario} />}
        {vista === 'admin' && usuario && <PanelAdmin usuario={usuario} alCambiarPendientes={setPendientesAdmin} />}
        {vista === 'login' && !usuario && (
          <Login aviso={aviso} alIniciar={iniciar} irARegistro={() => { setAviso(null); ir('registro') }}
            irARegistroPrestador={() => { setAviso(null); ir('registro-prestador') }} />
        )}
        {vista === 'registro' && !usuario && (
          <RegistroTurista
            alRegistrar={() => { setAviso('Tu cuenta fue creada. Ya puedes iniciar sesión.'); ir('login') }}
            irALogin={() => ir('login')}
            irARegistroPrestador={() => ir('registro-prestador')}
          />
        )}
        {vista === 'registro-prestador' && !usuario && (
          <RegistroPrestador
            alRegistrar={() => {
              setAviso('Solicitud enviada. Ya puedes iniciar sesión; publicarás cuando un administrador valide tu NIT y RNT.')
              ir('login')
            }}
            irALogin={() => ir('login')}
            irARegistroTurista={() => ir('registro')}
          />
        )}
      </main>

      <footer className="pie">
        <div className="contenedor">
          <span><strong>SAMAR</strong> · Turismo integrado y sostenible en Santa Marta</span>
          <span>Proyecto de Arquitectura de Software · Universidad del Magdalena</span>
        </div>
      </footer>

      <Toast mensaje={toast} alCerrar={cerrarToast} />
    </div>
  )
}
