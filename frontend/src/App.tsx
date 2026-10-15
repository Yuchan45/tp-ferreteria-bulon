import { useEffect, useState, type ReactNode } from 'react'
import { AuthProvider } from './auth'
import { useAuth } from './session'
import type { Rol } from './api/types'
import Login from './pages/Login'
import Panel from './pages/Panel'
import Productos from './pages/Productos'
import Movimientos from './pages/Movimientos'
import Reposicion from './pages/Reposicion'
import Compras from './pages/Compras'
import Reportes from './pages/Reportes'
import Proveedores from './pages/Proveedores'
import Usuarios from './pages/Usuarios'

function Logo() {
  return (
    <svg className="brand__logo" viewBox="0 0 32 32" aria-hidden="true">
      <path fill="currentColor" d="M16 1.5 28.6 8.75v14.5L16 30.5 3.4 23.25V8.75z" />
      <circle cx="16" cy="16" r="6" fill="var(--header-bg)" />
    </svg>
  )
}

interface Ruta {
  path: string
  label: string
  page: () => ReactNode
  /** Roles con acceso; sin definir = todos. */
  roles?: Rol[]
}

const RUTAS: Ruta[] = [
  { path: '/', label: 'Panel', page: () => <Panel /> },
  { path: '/productos', label: 'Productos', page: () => <Productos /> },
  { path: '/movimientos', label: 'Movimientos', page: () => <Movimientos /> },
  { path: '/reposicion', label: 'Reposición', page: () => <Reposicion /> },
  { path: '/compras', label: 'Órdenes de compra', page: () => <Compras /> },
  { path: '/reportes', label: 'Reportes', page: () => <Reportes /> },
  { path: '/proveedores', label: 'Proveedores', page: () => <Proveedores /> },
  { path: '/usuarios', label: 'Usuarios', page: () => <Usuarios />, roles: ['ADMINISTRADOR'] },
]

function useHashPath() {
  const read = () => location.hash.slice(1) || '/'
  const [path, setPath] = useState(read)
  useEffect(() => {
    const onChange = () => setPath(read())
    addEventListener('hashchange', onChange)
    return () => removeEventListener('hashchange', onChange)
  }, [])
  return path
}

function Shell() {
  const { user, ready, signOut } = useAuth()
  const path = useHashPath()

  if (!ready) return null
  if (!user) {
    return (
      <>
        <Header />
        <Login />
      </>
    )
  }

  const visibles = RUTAS.filter((r) => !r.roles || r.roles.includes(user.rol))
  const actual = visibles.find((r) => r.path === path) ?? visibles[0]

  return (
    <>
      <Header>
        <div className="topbar__user">
          <span>
            {user.nombre} · <small>{user.rol.replace('_', ' ').toLowerCase()}</small>
          </span>
          <button type="button" className="button button--ghost button--onDark" onClick={signOut}>
            Cerrar sesión
          </button>
        </div>
      </Header>
      <nav className="nav" aria-label="Secciones">
        <div className="container nav__inner">
          {visibles.map((r) => (
            <a key={r.path} href={`#${r.path}`} aria-current={r === actual ? 'page' : undefined}>
              {r.label}
            </a>
          ))}
        </div>
      </nav>
      <main className="container main">{actual.page()}</main>
      <Footer />
    </>
  )
}

function Header({ children }: { children?: ReactNode }) {
  return (
    <header className="topbar">
      <div className="container topbar__inner">
        <div className="brand">
          <Logo />
          <div>
            <span className="brand__name">Ferretería Bulón</span>
            <span className="brand__tagline">Gestión de stock</span>
          </div>
        </div>
        {children}
      </div>
    </header>
  )
}

function Footer() {
  return <footer className="container page-footer">Bulón S.R.L. · Sistema de gestión de stock</footer>
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  )
}
