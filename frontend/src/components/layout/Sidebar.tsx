import { ArrowLeftRight, LayoutDashboard, Package, TriangleAlert, Truck, Users, type LucideIcon } from 'lucide-react'
import { NavLink } from 'react-router'
import { listarBajoStock } from '@/api/productos'
import { useApi } from '@/hooks/useApi'
import { cn } from '@/lib/utils'

const ITEMS: { to: string; label: string; icon: LucideIcon; end?: boolean; contador?: boolean }[] = [
  { to: '/', label: 'Panel', icon: LayoutDashboard, end: true },
  { to: '/productos', label: 'Productos', icon: Package },
  { to: '/reponer', label: 'A reponer', icon: TriangleAlert, contador: true },
  { to: '/movimientos', label: 'Movimientos', icon: ArrowLeftRight },
  { to: '/proveedores', label: 'Proveedores', icon: Truck },
  { to: '/usuarios', label: 'Usuarios', icon: Users },
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
  const bajoStock = useApi((signal) => listarBajoStock(signal))
  const pendientes = bajoStock.data?.length ?? 0

  return (
    <aside className="sticky top-0 flex h-screen w-64 shrink-0 flex-col gap-1 bg-sidebar px-4 pt-5 pb-4 text-sidebar-foreground">
      <div className="flex items-center gap-3 pb-6 pl-2">
        <Logo />
        <div className="leading-tight">
          <p className="font-medium">Ferretería Bulón</p>
          <p className="text-[13px] text-sidebar-muted">Gestión de stock</p>
        </div>
      </div>

      <nav className="flex flex-col gap-1" aria-label="Principal">
        {ITEMS.map(({ to, label, icon: Icon, end, contador }) => (
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
    </aside>
  )
}
