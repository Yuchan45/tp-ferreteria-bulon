import { post } from './client'
import type { Rol, Sesion } from './types'

export const login = (username: string, password: string) =>
  post<Sesion>('/auth/login', { username, password })

/** Crea la cuenta con el rol elegido (el empleado, con su sucursal) y devuelve su sesión. */
export const registrar = (datos: {
  username: string
  nombre: string
  email: string | null
  password: string
  rol: Rol
  sucursalId: number | null
}) => post<Sesion>('/auth/registro', datos)
