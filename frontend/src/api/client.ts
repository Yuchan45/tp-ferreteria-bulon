import type { FormatoReporte, Usuario } from './types'

const KEY = 'bulon.credenciales'

export class ApiError extends Error {
  readonly status: number
  readonly detail: string
  constructor(status: number, detail: string) {
    super(detail)
    this.status = status
    this.detail = detail
  }
}

let authorization: string | null = sessionStorage.getItem(KEY)
let onUnauthorized: () => void = () => {}

/** El proveedor de sesión se registra acá para que un 401 en cualquier pantalla cierre la sesión. */
export function setOnUnauthorized(callback: () => void) {
  onUnauthorized = callback
}

export function hasCredentials() {
  return authorization !== null
}

function setCredentials(value: string | null) {
  authorization = value
  if (value) sessionStorage.setItem(KEY, value)
  else sessionStorage.removeItem(KEY)
}

export const logout = () => setCredentials(null)

type Params = Record<string, string | number | boolean | null | undefined>

function url(path: string, params?: Params) {
  const query = new URLSearchParams()
  for (const [k, v] of Object.entries(params ?? {})) {
    if (v !== undefined && v !== null && v !== '') query.set(k, String(v))
  }
  const qs = query.toString()
  return `/api${path}${qs ? `?${qs}` : ''}`
}

async function send(path: string, init: RequestInit = {}, params?: Params): Promise<Response> {
  const headers = new Headers(init.headers)
  if (authorization) headers.set('Authorization', authorization)
  if (init.body) headers.set('Content-Type', 'application/json')

  let res: Response
  try {
    res = await fetch(url(path, params), { ...init, headers })
  } catch {
    throw new ApiError(0, 'No se pudo contactar al servidor.')
  }
  if (res.ok) return res

  // Los errores del backend son ProblemDetail ({status, detail}); si no, un mensaje genérico.
  const problem = await res.json().catch(() => null)
  const error = new ApiError(res.status, problem?.detail ?? `Error ${res.status}`)
  if (res.status === 401 && path !== '/auth/me') {
    logout()
    onUnauthorized()
  }
  throw error
}

async function json<T>(path: string, init?: RequestInit, params?: Params): Promise<T> {
  const res = await send(path, init, params)
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}

const body = (data: unknown): RequestInit => ({ body: JSON.stringify(data) })

export const api = {
  get: <T>(path: string, params?: Params) => json<T>(path, undefined, params),
  post: <T>(path: string, data?: unknown) =>
    json<T>(path, { method: 'POST', ...(data === undefined ? {} : body(data)) }),
  put: <T>(path: string, data: unknown) => json<T>(path, { method: 'PUT', ...body(data) }),
  delete: (path: string) => json<void>(path, { method: 'DELETE' }),
}

/** Login = GET /auth/me con las credenciales; si falla no queda nada guardado. */
export async function login(username: string, password: string): Promise<Usuario> {
  setCredentials(`Basic ${btoa(String.fromCharCode(...new TextEncoder().encode(`${username}:${password}`)))}`)
  try {
    return await api.get<Usuario>('/auth/me')
  } catch (error) {
    logout()
    throw error
  }
}

export const me = () => api.get<Usuario>('/auth/me')

/** Descarga un archivo con fetch para poder mandar el header de autenticación. */
export async function download(
  path: string,
  params: Params & { formato: FormatoReporte },
) {
  const res = await send(path, undefined, params)
  const disposition = res.headers.get('Content-Disposition') ?? ''
  const nombre = /filename="?([^";]+)"?/.exec(disposition)?.[1] ?? `reporte.${params.formato === 'PDF' ? 'pdf' : 'xlsx'}`
  const link = document.createElement('a')
  const objectUrl = URL.createObjectURL(await res.blob())
  link.href = objectUrl
  link.download = nombre
  link.click()
  URL.revokeObjectURL(objectUrl)
}
