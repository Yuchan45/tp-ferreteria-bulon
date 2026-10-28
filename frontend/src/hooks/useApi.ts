import { useCallback, useEffect, useRef, useState } from 'react'
import { useDataVersion } from './data-context'

interface Resultado<T> {
  /** Consulta a la que corresponde este resultado (ver `clave`). */
  clave: string
  data?: T
  error?: Error
}

/**
 * Ejecuta `loader` al montar, cuando cambian `deps` y cada vez que se invalida
 * la información (después de crear o modificar algo). Cancela la consulta
 * anterior si el componente se desmonta o vuelve a consultar. Mientras recarga
 * conserva los datos anteriores, así la pantalla no parpadea.
 */
export function useApi<T>(loader: (signal: AbortSignal) => Promise<T>, deps: unknown[] = []) {
  const { version } = useDataVersion()
  const [retry, setRetry] = useState(0)
  const [resultado, setResultado] = useState<Resultado<T>>({ clave: '' })

  // loader se redefine en cada render; se usa siempre la última versión
  const loaderRef = useRef(loader)
  useEffect(() => {
    loaderRef.current = loader
  })

  const clave = `${version}|${retry}|${JSON.stringify(deps)}`

  useEffect(() => {
    const controller = new AbortController()
    loaderRef.current(controller.signal).then(
      (data) => setResultado({ clave, data }),
      (error: Error) => {
        if (!controller.signal.aborted) setResultado((r) => ({ clave, data: r.data, error }))
      },
    )
    return () => controller.abort()
  }, [clave])

  const reload = useCallback(() => setRetry((r) => r + 1), [])
  const actual = resultado.clave === clave
  return {
    data: resultado.data,
    error: actual ? resultado.error : undefined,
    loading: !actual,
    reload,
  }
}
