import { api, post } from './client'
import type { Sesion, Usuario } from './types'

export const login = (username: string, password: string) =>
  post<Sesion>('/auth/login', { username, password })

/** El backend siempre crea un EMPLEADO de la sucursal elegida y devuelve su sesión. */
export const registrar = (datos: { username: string; nombre: string; email: string | null; password: string; sucursalId: number }) =>
  post<Sesion>('/auth/registro', datos)

export const usuarioActual = (signal?: AbortSignal) => api<Usuario>('/auth/yo', { signal })
