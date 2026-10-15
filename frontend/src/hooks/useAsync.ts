import { useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import type { Page } from '../api/types'

interface Result<T> {
  data?: T
  error: string | null
  loading: boolean
  reload: () => void
}

/**
 * Ejecuta `load` al montar y cada vez que cambia `key` (un string que resume los parámetros).
 * Descarta respuestas de pedidos viejos. Los datos anteriores se conservan mientras recarga.
 */
export function useAsync<T>(load: () => Promise<T>, key: string): Result<T> {
  const [state, setState] = useState<{ data?: T; error: string | null; loading: boolean }>({
    error: null,
    loading: true,
  })
  const [tick, setTick] = useState(0)

  useEffect(() => {
    let vigente = true
    setState((s) => ({ ...s, loading: true }))
    load().then(
      (data) => vigente && setState({ data, error: null, loading: false }),
      (e: unknown) =>
        vigente &&
        setState((s) => ({ ...s, error: e instanceof ApiError ? e.detail : String(e), loading: false })),
    )
    return () => {
      vigente = false
    }
    // `load` es una closure nueva en cada render; `key` y `tick` son lo que decide si hay que recargar.
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [key, tick])

  return { ...state, reload: () => setTick((t) => t + 1) }
}

type Filters = Record<string, string | number | boolean | null | undefined>

/** Lista paginada: vuelve a la página 0 cuando cambian los filtros. */
/** `refresh` es un contador que el llamador sube para forzar la recarga (p. ej. tras registrar algo). */
export function usePaged<T>(path: string, filters: Filters, refresh = 0, size = 20) {
  const filtersKey = JSON.stringify(filters)
  const [current, setCurrent] = useState({ filtersKey, page: 0 })
  const page = current.filtersKey === filtersKey ? current.page : 0

  const result = useAsync(
    () => api.get<Page<T>>(path, { ...filters, page, size }),
    `${path}|${filtersKey}|${page}|${refresh}`,
  )
  return {
    ...result,
    rows: result.data?.content ?? [],
    info: result.data?.page,
    setPage: (p: number) => setCurrent({ filtersKey, page: p }),
  }
}

/** Ejecuta una acción (guardar, enviar, cancelar…) y guarda el `detail` del error para mostrarlo. */
export function useAction() {
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  /** Devuelve true si la acción terminó bien. */
  const run = async (action: () => Promise<unknown>) => {
    setPending(true)
    setError(null)
    try {
      await action()
      return true
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : String(e))
      return false
    } finally {
      setPending(false)
    }
  }
  return { run, error, pending, clearError: () => setError(null) }
}
