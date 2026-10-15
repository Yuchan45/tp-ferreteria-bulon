import { useHealth, type Connection } from '../hooks/useHealth'
import type { HealthStatus } from '../api/health'

const MENSAJES: Record<Connection, { titulo: string; detalle: string }> = {
  checking: {
    titulo: 'Verificando conexión…',
    detalle: 'Consultando el estado del servidor.',
  },
  online: {
    titulo: 'Conectado',
    detalle: 'El servidor y la base de datos responden correctamente.',
  },
  degraded: {
    titulo: 'Conexión parcial',
    detalle: 'El servidor responde, pero la base de datos no está disponible.',
  },
  offline: {
    titulo: 'Sin conexión',
    detalle: 'No se pudo contactar al servidor. Verificá que el backend esté corriendo en el puerto 8080.',
  },
}

type Indicador = 'ok' | 'error' | 'pending'

function indicadorDe(status: HealthStatus | undefined): Indicador {
  if (status === 'UP') return 'ok'
  if (status) return 'error'
  return 'pending'
}

function Fila({ nombre, estado, texto }: { nombre: string; estado: Indicador; texto: string }) {
  return (
    <li className="status-row">
      <span className="status-row__name">{nombre}</span>
      <span className={`badge badge--${estado}`}>{texto}</span>
    </li>
  )
}

export function ConnectionStatus() {
  const { connection, health, checkedAt, checking, refresh } = useHealth()
  const mensaje = MENSAJES[connection]

  const servidor: Indicador =
    connection === 'checking' ? 'pending' : connection === 'offline' ? 'error' : 'ok'
  const db = indicadorDe(health?.components?.db?.status)

  return (
    <section className={`card status status--${connection}`} aria-labelledby="estado-titulo">
      <header className="status__header">
        <span className="status__dot" aria-hidden="true" />
        <div aria-live="polite">
          <h2 id="estado-titulo" className="status__title">{mensaje.titulo}</h2>
          <p className="status__detail">{mensaje.detalle}</p>
        </div>
      </header>

      <ul className="status__list">
        <Fila
          nombre="Servidor (API)"
          estado={servidor}
          texto={servidor === 'ok' ? 'En línea' : servidor === 'error' ? 'No responde' : '—'}
        />
        <Fila
          nombre="Base de datos"
          estado={db}
          texto={db === 'ok' ? 'En línea' : db === 'error' ? 'Caída' : 'Sin datos'}
        />
      </ul>

      <footer className="status__footer">
        <span className="status__time">
          {checkedAt
            ? `Última verificación: ${checkedAt.toLocaleTimeString('es-AR', { hourCycle: 'h23' })}`
            : 'Esperando la primera verificación'}
        </span>
        <button type="button" className="button" onClick={refresh} disabled={checking}>
          {checking ? 'Verificando…' : 'Verificar ahora'}
        </button>
      </footer>
    </section>
  )
}
