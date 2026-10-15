import { useEffect, useState } from 'react'
import { fetchHealth, type HealthResponse } from '../api/health'

/**
 * - checking: primera verificación en curso
 * - online:   backend y base de datos UP
 * - degraded: el backend responde pero algún componente está caído
 * - offline:  el backend no responde
 */
export type Connection = 'checking' | 'online' | 'degraded' | 'offline'

interface HealthState {
  connection: Connection
  health: HealthResponse | null
  checkedAt: Date | null
}

function toState(health: HealthResponse | null): HealthState {
  return {
    connection: !health ? 'offline' : health.status === 'UP' ? 'online' : 'degraded',
    health,
    checkedAt: new Date(),
  }
}

/**
 * Verifica /api/health al montar y luego `intervalMs` después de que termina
 * cada verificación (así nunca hay dos consultas superpuestas).
 */
export function useHealth(intervalMs = 15_000) {
  const [state, setState] = useState<HealthState>({
    connection: 'checking',
    health: null,
    checkedAt: null,
  })
  const [refreshing, setRefreshing] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    let timer: ReturnType<typeof setTimeout>

    const verificar = () =>
      fetchHealth(controller.signal).then(
        (health) => {
          setState(toState(health))
          timer = setTimeout(verificar, intervalMs)
        },
        () => {}, // verificación cancelada al desmontar el componente
      )

    verificar()
    return () => {
      controller.abort()
      clearTimeout(timer)
    }
  }, [intervalMs])

  /** Verificación manual (botón "Verificar ahora"). */
  const refresh = async () => {
    setRefreshing(true)
    setState(toState(await fetchHealth()))
    setRefreshing(false)
  }

  return { ...state, checking: refreshing || state.connection === 'checking', refresh }
}
