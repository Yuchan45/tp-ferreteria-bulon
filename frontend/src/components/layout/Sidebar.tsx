import {
  ArrowLeftRight,
  FileChartColumn,
  LayoutDashboard,
  LogOut,
  Package,
  ShieldCheck,
  TriangleAlert,
  Truck,
  UserRound,
  Users,
  type LucideIcon,
} from 'lucide-react'
import { NavLink } from 'react-router'
import { listarAReponer } from '@/api/productos'
import { useSesion, useAuth } from '@/hooks/auth-context'
import { useApi } from '@/hooks/useApi'
import { ROL_LABEL, iniciales } from '@/lib/format'
import { cn } from '@/lib/utils'

const ITEMS: { to: string; label: string; icon: LucideIcon; end?: boolean; contador?: boolean; soloAdmin?: boolean }[] = [
  { to: '/', label: 'Panel', icon: LayoutDashboard, end: true },
  { to: '/productos', label: 'Productos', icon: Package },
  { to: '/reponer', label: 'A reponer', icon: TriangleAlert, contador: true },
  { to: '/movimientos', label: 'Movimientos', icon: ArrowLeftRight },
  { to: '/proveedores', label: 'Proveedores', icon: Truck },
  { to: '/reportes', label: 'Reportes', icon: FileChartColumn },
  { to: '/usuarios', label: 'Usuarios', icon: Users, soloAdmin: true },
]

function Logo() {
  return (
    <svg className="size-8 shrink-0" viewBox="0 0 32 32" aria-hidden="true">
      <path className="fill-brand" d="M16 1.5 28.6 8.75v14.5L16 30.5 3.4 23.25V8.75z" />
      <circle cx="16" cy="16" r="6" className="fill-sidebar" />
    </svg>
  )
}

export function Sidebar() {
  const { usuario, sucursal, esAdmin } = useSesion()
  const { cerrarSesion } = useAuth()
  const aReponer = useApi((signal) => listarAReponer(sucursal.id, signal), [sucursal.id])
  const pendientes = aReponer.data?.length ?? 0
  const RolIcono = esAdmin ? ShieldCheck : UserRound

  return (
    <aside className="sticky top-0 flex h-screen w-64 shrink-0 flex-col gap-1 bg-sidebar px-4 pt-5 pb-4 text-sidebar-foreground">
      <div className="flex items-center gap-3 pb-6 pl-2">
        <Logo />
        <div className="leading-tight">
          <p className="font-medium">Ferretería Bulón</p>
          <p className="text-[13px] text-sidebar-muted">{sucursal.nombre}</p>
        </div>
      </div>

      <nav className="flex flex-col gap-1" aria-label="Principal">
        {/* El menú solo muestra lo que el rol puede usar; el backend lo controla igual */}
        {ITEMS.filter((item) => !item.soloAdmin || esAdmin).map(({ to, label, icon: Icon, end, contador }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              cn(
                'flex h-10 items-center gap-3 rounded-lg px-3 transition-colors',
                isActive ? 'bg-sidebar-accent font-medium text-sidebar-foreground' : 'text-sidebar-muted hover:bg-sidebar-accent/50 hover:text-sidebar-foreground',
              )
            }
          >
            {({ isActive }) => (
              <>
                {/* El acento naranja de marca se usa solo en el ítem activo */}
                <Icon className={cn('size-4.5', isActive && 'text-brand')} aria-hidden />
                <span className="flex-1">{label}</span>
                {contador && pendientes > 0 && (
                  <span className="rounded-full bg-stock-low-bg px-2 text-[13px] font-medium text-stock-low" aria-label={`${pendientes} productos a reponer`}>
                    {pendientes}
                  </span>
                )}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {/* Con qué rol se está usando el sistema: cambia el menú, los botones y lo que permite el backend */}
      <section aria-label="Rol del usuario" className="mt-auto mb-3 rounded-lg bg-sidebar-accent/60 px-3 py-2.5">
        <p className="text-[11px] font-medium tracking-wide text-sidebar-muted uppercase">Rol</p>
        <p className="mt-1 flex items-center gap-2 font-medium">
          <RolIcono className={cn('size-4 shrink-0', esAdmin && 'text-stock-ok')} aria-hidden />
          {ROL_LABEL[usuario.rol]}
        </p>
        <p className="mt-0.5 text-[13px] text-sidebar-muted">{esAdmin ? 'Todas las sucursales' : `Solo ${sucursal.nombre}`}</p>
      </section>

      <div className="flex items-center gap-3 border-t border-sidebar-border px-2 pt-3">
        <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-sidebar-accent text-[13px] font-medium">
          {iniciales(usuario.nombre)}
        </div>
        <div className="min-w-0 flex-1 leading-tight">
          <p className="truncate text-[13px] font-medium">{usuario.nombre}</p>
          <p className="truncate font-mono text-[13px] text-sidebar-muted">{usuario.username}</p>
        </div>
        <button
          type="button"
          onClick={cerrarSesion}
          title="Cerrar sesión"
          className="rounded-md p-1.5 text-sidebar-muted transition-colors hover:bg-sidebar-accent hover:text-sidebar-foreground"
        >
          <LogOut className="size-4" />
          <span className="sr-only">Cerrar sesión</span>
        </button>
      </div>
    </aside>
  )
}
