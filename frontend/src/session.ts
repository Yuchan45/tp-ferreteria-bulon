import { createContext, useContext } from 'react'
import type { Rol, Usuario } from './api/types'

export interface AuthState {
  user: Usuario | null
  /** false mientras se valida la sesión guardada al abrir la app. */
  ready: boolean
  signIn: (username: string, password: string) => Promise<void>
  signOut: () => void
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth fuera de AuthProvider')
  return ctx
}

/** Usuario autenticado y lo que su rol puede hacer. Solo se usa dentro de pantallas con sesión. */
export function useSession() {
  const { user } = useAuth()
  if (!user) throw new Error('Sin sesión')
  const rol: Rol = user.rol
  return { user, rol, puedeEscribir: rol !== 'VENDEDOR', esAdmin: rol === 'ADMINISTRADOR' }
}
