const TIMEOUT_MS = 10_000

/** Error de la API con el mensaje legible que devuelve el backend (ProblemDetail.detail). */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

const MENSAJES_POR_ESTADO: Record<number, string> = {
  404: 'No se encontró el recurso solicitado.',
  500: 'Error interno del servidor.',
  502: 'El servidor no responde. Verificá que el backend esté corriendo.',
  503: 'El servicio no está disponible en este momento.',
}

// Token de la sesión actual. Lo setea AuthProvider al iniciar o cerrar sesión.
let token: string | null = null
let alExpirar: (() => void) | null = null

export function configurarSesion(nuevoToken: string | null, onExpirada?: () => void) {
  token = nuevoToken
  if (onExpirada) alExpirar = onExpirada
}

/**
 * Llama a la API REST (`/api/...`, redirigido por Vite al backend).
 * Si hay sesión, agrega `Authorization: Bearer <token>`. Lanza ApiError con el `detail`
 * del ProblemDetail cuando la respuesta no es 2xx, o ApiError(0) si no hay conexión.
 * Un 401 con sesión iniciada significa que el token venció: se cierra la sesión.
 */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const timeout = AbortSignal.timeout(TIMEOUT_MS)
  const signal = init.signal ? AbortSignal.any([init.signal, timeout]) : timeout

  let res: Response
  try {
    res = await fetch(`/api${path}`, {
      ...init,
      signal,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    })
  } catch (error) {
    // Cancelación externa (p. ej. al desmontar): se propaga tal cual
    if (init.signal?.aborted) throw error
    throw new ApiError(0, 'No se pudo conectar con el servidor.')
  }

  if (!res.ok) {
    let detail: string | undefined
    try {
      const body = (await res.json()) as { detail?: string }
      detail = body.detail
    } catch {
      // respuesta sin cuerpo JSON
    }
    if (res.status === 401 && token && !path.startsWith('/auth/')) alExpirar?.()
    throw new ApiError(res.status, detail ?? MENSAJES_POR_ESTADO[res.status] ?? `Error ${res.status}`)
  }

  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export const post = <T>(path: string, body: unknown) =>
  api<T>(path, { method: 'POST', body: JSON.stringify(body) })

export const put = <T>(path: string, body: unknown) =>
  api<T>(path, { method: 'PUT', body: JSON.stringify(body) })
