import { createContext, useContext } from 'react'
import type { Sesion, Sucursal, Usuario } from '@/api/types'

export interface AuthValue {
  usuario: Usuario | null
  /** Sucursal con la que se trabaja: la del empleado o, para el administrador, la primera. */
  sucursal: Sucursal | null
  esAdmin: boolean
  /** true mientras se resuelve la sucursal de la sesión. */
  cargando: boolean
  iniciarSesion: (sesion: Sesion) => void
  cerrarSesion: () => void
}

export const AuthContext = createContext<AuthValue | null>(null)

export function useAuth() {
  const valor = useContext(AuthContext)
  if (!valor) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return valor
}

/** Dentro de las vistas protegidas la sesión y la sucursal siempre existen. */
export function useSesion() {
  const { usuario, sucursal, esAdmin } = useAuth()
  if (!usuario || !sucursal) throw new Error('useSesion requiere una sesión iniciada')
  return { usuario, sucursal, esAdmin }
}
