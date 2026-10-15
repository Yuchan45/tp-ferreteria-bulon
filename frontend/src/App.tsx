import { ConnectionStatus } from './components/ConnectionStatus'

function Logo() {
  return (
    <svg className="brand__logo" viewBox="0 0 32 32" aria-hidden="true">
      <path fill="currentColor" d="M16 1.5 28.6 8.75v14.5L16 30.5 3.4 23.25V8.75z" />
      <circle cx="16" cy="16" r="6" fill="var(--header-bg)" />
    </svg>
  )
}

function App() {
  return (
    <>
      <header className="topbar">
        <div className="container topbar__inner">
          <div className="brand">
            <Logo />
            <div>
              <span className="brand__name">Ferretería Bulón</span>
              <span className="brand__tagline">Gestión de stock</span>
            </div>
          </div>
        </div>
      </header>

      <main className="container main">
        <div className="page-heading">
          <h1>Panel general</h1>
          <p>Estado de la conexión entre el sistema y el servidor.</p>
        </div>

        <ConnectionStatus />
      </main>

      <footer className="container page-footer">Bulón S.R.L. · Sistema de gestión de stock</footer>
    </>
  )
}

export default App
