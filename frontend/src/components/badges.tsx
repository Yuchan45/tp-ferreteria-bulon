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

const MOVIMIENTO: Record<TipoMovimiento, { icon: LucideIcon; className: string }> = {
  INGRESO_COMPRA: { icon: ArrowDown, className: 'bg-mov-ingreso-bg text-mov-ingreso' },
  SALIDA_VENTA: { icon: ArrowUp, className: 'bg-mov-salida-bg text-mov-salida' },
  DEVOLUCION_CLIENTE: { icon: Undo2, className: 'bg-mov-devolucion-bg text-mov-devolucion' },
  DEVOLUCION_PROVEEDOR: { icon: Undo2, className: 'bg-mov-devolucion-bg text-mov-devolucion' },
  ROTURA: { icon: CircleX, className: 'bg-mov-rotura-bg text-mov-rotura' },
  AJUSTE: { icon: Pencil, className: 'bg-mov-ajuste-bg text-mov-ajuste' },
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
