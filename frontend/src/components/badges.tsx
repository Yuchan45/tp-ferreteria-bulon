import { ArrowDown, ArrowUp, Pencil } from 'lucide-react'
import type { TipoMovimiento } from '@/api/types'
import { TIPO_LABEL } from '@/lib/format'
import type { EstadoStock } from '@/lib/stock'
import { cn } from '@/lib/utils'

const STOCK: Record<EstadoStock, { label: string; className: string }> = {
  OK: { label: 'OK', className: 'bg-stock-ok-bg text-stock-ok' },
  BAJO: { label: 'Stock bajo', className: 'bg-stock-low-bg text-stock-low' },
  SIN_STOCK: { label: 'Sin stock', className: 'bg-stock-out-bg text-stock-out' },
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

const MOVIMIENTO: Record<TipoMovimiento, { icon: typeof ArrowDown; className: string }> = {
  ENTRADA: { icon: ArrowDown, className: 'bg-mov-ingreso-bg text-mov-ingreso' },
  SALIDA: { icon: ArrowUp, className: 'bg-mov-salida-bg text-mov-salida' },
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
