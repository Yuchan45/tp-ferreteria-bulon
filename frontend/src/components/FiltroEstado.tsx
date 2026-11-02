import type { EstadoStock } from '@/api/types'
import { ESTADO_LABEL, PUNTO_ESTADO } from '@/lib/stock'
import { cn } from '@/lib/utils'

interface Props {
  /** Estados que se ofrecen, además de "Todos". */
  estados: EstadoStock[]
  value: EstadoStock | null
  onChange: (estado: EstadoStock | null) => void
  /** Cuántos elementos hay en cada estado (null = todos). */
  contar: (estado: EstadoStock | null) => number
  tamano?: 'sm' | 'md'
}

/** Filtro por estado de stock con pastillas de color y la cantidad de cada uno. */
export function FiltroEstado({ estados, value, onChange, contar, tamano = 'sm' }: Props) {
  const opciones: (EstadoStock | null)[] = [null, ...estados]
  return (
    <div role="group" aria-label="Filtrar por estado" className="flex flex-wrap gap-1.5">
      {opciones.map((e) => {
        const activo = value === e
        return (
          <button
            key={e ?? 'todos'}
            type="button"
            aria-pressed={activo}
            onClick={() => onChange(e)}
            className={cn(
              'flex items-center gap-1.5 rounded-full border transition-colors',
              tamano === 'sm' ? 'px-2.5 py-0.5 text-[13px]' : 'h-9 px-3.5 text-sm',
              activo ? 'border-foreground/20 bg-muted font-medium text-foreground' : 'bg-card text-muted-foreground hover:bg-muted/60',
            )}
          >
            {e && <span className={cn('size-2 rounded-full', PUNTO_ESTADO[e])} aria-hidden />}
            {e ? ESTADO_LABEL[e] : 'Todos'}
            <span className="tabular-nums text-muted-foreground">{contar(e)}</span>
          </button>
        )
      })}
    </div>
  )
}
