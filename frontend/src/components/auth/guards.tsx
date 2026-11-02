import type { ReactNode } from 'react'
import { ShieldAlert } from 'lucide-react'
import { Navigate, useLocation } from 'react-router'
import { EstadoVacio } from '@/components/common'
import { useAuth } from '@/hooks/auth-context'

/**
 * Vistas que exigen sesión: sin usuario, redirige al login (y después vuelve a donde estaba).
 * Ojo: esto solo ordena la navegación. La seguridad real la pone el backend, que rechaza
 * cualquier request sin un token válido.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { usuario, cargando } = useAuth()
  const location = useLocation()
  if (!usuario) return <Navigate to="/login" replace state={{ desde: location.pathname }} />
  if (cargando) return <p className="p-8 text-muted-foreground">Cargando sesión…</p>
  return children
}

/** Vistas solo para administradores. El backend igual responde 403 si un empleado lo intenta. */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { esAdmin } = useAuth()
  if (!esAdmin)
    return <EstadoVacio icon={ShieldAlert} titulo="Sin permiso" descripcion="Esta sección es solo para administradores." />
  return children
}

/** Login y registro: si ya hay sesión, no tiene sentido mostrarlos. */
export function SoloInvitados({ children }: { children: ReactNode }) {
  const { usuario } = useAuth()
  if (usuario) return <Navigate to="/" replace />
  return children
}
