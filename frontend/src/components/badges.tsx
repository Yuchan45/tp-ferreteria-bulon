import { ArrowDown, ArrowUp, CircleX, Pencil, Undo2, type LucideIcon } from 'lucide-react'
import type { EstadoStock, TipoMovimiento } from '@/api/types'
import { TIPO_LABEL } from '@/lib/format'
import { cn } from '@/lib/utils'

const STOCK: Record<EstadoStock, { label: string; className: string }> = {
  OK: { label: 'OK', className: 'bg-stock-ok-bg text-stock-ok' },
  REPONER: { label: 'A reponer', className: 'bg-stock-low-bg text-stock-low' },
  CRITICO: { label: 'Crítico', className: 'bg-stock-out-bg text-stock-out' },
}

export function StockBadge({ estado }: { estado: EstadoStock }) {
  const { label, className } = STOCK[estado]
  return (
    <span className={cn('inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-[13px] font-medium whitespace-nowrap', className)}>
      <span className="size-1.5 rounded-full bg-current" aria-hidden />
      {label}
    </span>
  )
}

const MOVIMIENTO: Record<TipoMovimiento, { icon: LucideIcon; className: string; color: string }> = {
  INGRESO_COMPRA: { icon: ArrowDown, className: 'bg-mov-ingreso-bg text-mov-ingreso', color: 'text-mov-ingreso' },
  SALIDA_VENTA: { icon: ArrowUp, className: 'bg-mov-salida-bg text-mov-salida', color: 'text-mov-salida' },
  DEVOLUCION_CLIENTE: { icon: Undo2, className: 'bg-mov-devolucion-bg text-mov-devolucion', color: 'text-mov-devolucion' },
  DEVOLUCION_PROVEEDOR: { icon: Undo2, className: 'bg-mov-devolucion-bg text-mov-devolucion', color: 'text-mov-devolucion' },
  ROTURA: { icon: CircleX, className: 'bg-mov-rotura-bg text-mov-rotura', color: 'text-mov-rotura' },
  AJUSTE: { icon: Pencil, className: 'bg-mov-ajuste-bg text-mov-ajuste', color: 'text-mov-ajuste' },
}

/**
 * Filtro por tipo de movimiento: una pastilla por tipo, con su ícono y color (los mismos
 * de {@link MovimientoBadge}) y la cantidad de movimientos de cada uno.
 */
export function FiltroTipoMovimiento({
  value,
  onChange,
  contar,
}: {
  value: TipoMovimiento | null
  onChange: (tipo: TipoMovimiento | null) => void
  /** Cuántos movimientos hay de cada tipo (null = todos). */
  contar: (tipo: TipoMovimiento | null) => number
}) {
  const opciones: (TipoMovimiento | null)[] = [null, ...(Object.keys(MOVIMIENTO) as TipoMovimiento[])]
  return (
    <div role="group" aria-label="Filtrar por tipo" className="flex flex-wrap gap-1.5">
      {opciones.map((t) => {
        const activo = value === t
        const cantidad = contar(t)
        const Icon = t ? MOVIMIENTO[t].icon : null
        return (
          <button
            key={t ?? 'todos'}
            type="button"
            aria-pressed={activo}
            onClick={() => onChange(t)}
            className={cn(
              'flex h-8 items-center gap-1.5 rounded-full border px-3 text-[13px] transition-colors',
              activo
                ? cn('border-current/25 font-medium', t ? MOVIMIENTO[t].className : 'bg-muted text-foreground')
                : 'bg-card text-muted-foreground hover:bg-muted/60',
              !activo && cantidad === 0 && 'opacity-50',
            )}
          >
            {Icon && <Icon className={cn('size-3.5', !activo && MOVIMIENTO[t!].color)} aria-hidden />}
            {t ? TIPO_LABEL[t] : 'Todos'}
            <span className={cn('tabular-nums', activo ? 'opacity-70' : 'text-muted-foreground')}>{cantidad}</span>
          </button>
        )
      })}
    </div>
  )
}

/** Tipo de movimiento: siempre ícono + texto, nunca solo color. */
export function MovimientoBadge({ tipo }: { tipo: TipoMovimiento }) {
  const { icon: Icon, className } = MOVIMIENTO[tipo]
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[13px] font-medium whitespace-nowrap', className)}>
      <Icon className="size-3.5" aria-hidden />
      {TIPO_LABEL[tipo]}
    </span>
  )
}

export const ICONO_MOVIMIENTO: Record<TipoMovimiento, LucideIcon> = Object.fromEntries(
  Object.entries(MOVIMIENTO).map(([tipo, { icon }]) => [tipo, icon]),
) as Record<TipoMovimiento, LucideIcon>
