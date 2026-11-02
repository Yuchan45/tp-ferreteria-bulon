import { useEffect, useRef, useState } from 'react'
import { Check, ChevronDown, Search } from 'lucide-react'
import type { StockSucursal } from '@/api/types'
import { FiltroEstado } from '@/components/FiltroEstado'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { fmtNumero, incluye } from '@/lib/format'
import { ESTADO_LABEL, PUNTO_ESTADO } from '@/lib/stock'
import { cn } from '@/lib/utils'

type Estado = StockSucursal['estado']

interface Props {
  id?: string
  value: string
  onChange: (productoId: string) => void
  productos: StockSucursal[] | undefined
  invalid?: boolean
}

/**
 * Selector de producto con buscador (por código o nombre, sin importar acentos) y
 * filtro por estado del stock. Se maneja con el teclado: ↑ ↓ para moverse, Enter para
 * elegir y Esc para cerrar.
 */
export function ProductoSelector({ id, value, onChange, productos, invalid }: Props) {
  const [abierto, setAbierto] = useState(false)
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState<Estado | null>(null)
  const [activo, setActivo] = useState(0)
  const lista = useRef<HTMLUListElement>(null)

  const elegido = productos?.find((p) => String(p.productoId) === value)
  const coinciden = (productos ?? []).filter((p) => !busqueda.trim() || incluye(p.codigo, busqueda) || incluye(p.nombre, busqueda))
  const filtrados = coinciden.filter((p) => !estado || p.estado === estado)
  // Cuántos hay de cada estado entre los que coinciden con la búsqueda
  const cuantos = (e: Estado | null) => (e ? coinciden.filter((p) => p.estado === e).length : coinciden.length)

  // Mantiene visible la opción activa al moverse con el teclado
  useEffect(() => {
    lista.current?.querySelector(`[data-indice="${activo}"]`)?.scrollIntoView({ block: 'nearest' })
  }, [activo])

  function abrir(estado: boolean) {
    setAbierto(estado)
    setBusqueda('')
    setEstado(null)
    // Al abrir, arranca marcando el producto ya elegido
    setActivo(Math.max(0, (productos ?? []).findIndex((p) => String(p.productoId) === value)))
  }

  function elegir(p: StockSucursal) {
    onChange(String(p.productoId))
    setAbierto(false)
  }

  function teclado(e: React.KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActivo((i) => Math.min(i + 1, filtrados.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActivo((i) => Math.max(i - 1, 0))
    } else if (e.key === 'Enter') {
      e.preventDefault()
      if (filtrados[activo]) elegir(filtrados[activo])
    }
  }

  return (
    <Popover open={abierto} onOpenChange={abrir}>
      <PopoverTrigger asChild>
        <button
          id={id}
          type="button"
          role="combobox"
          aria-expanded={abierto}
          aria-invalid={invalid}
          disabled={!productos}
          className="flex h-9 w-full items-center justify-between gap-1.5 rounded-lg border border-input bg-card py-2 pr-2.5 pl-3 text-left text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20"
        >
          {elegido ? (
            <span className="truncate">
              <span className="font-mono text-[13px]">{elegido.codigo}</span> · {elegido.nombre}
            </span>
          ) : (
            <span className="text-muted-foreground">{productos ? 'Buscar o seleccionar producto' : 'Cargando productos…'}</span>
          )}
          <ChevronDown className="size-4 shrink-0 text-muted-foreground" aria-hidden />
        </button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-(--radix-popover-trigger-width) p-0" onKeyDown={teclado}>
        <div className="flex items-center gap-2 border-b px-3">
          <Search className="size-4 shrink-0 text-muted-foreground" aria-hidden />
          <input
            autoFocus
            value={busqueda}
            onChange={(e) => {
              setBusqueda(e.target.value)
              setActivo(0)
            }}
            placeholder="Buscar por código o nombre"
            aria-label="Buscar producto"
            aria-controls="productos-opciones"
            className="h-10 w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
          />
        </div>
        <div className="border-b px-3 py-2">
          <FiltroEstado
            estados={['OK', 'REPONER', 'CRITICO']}
            value={estado}
            onChange={(e) => {
              setEstado(e)
              setActivo(0)
            }}
            contar={cuantos}
          />
        </div>
        {filtrados.length === 0 ? (
          <p className="px-3 py-6 text-center text-sm text-muted-foreground">
            {busqueda.trim() ? `Ningún producto coincide con “${busqueda.trim()}”` : 'No hay productos'}
            {estado && ` en estado ${ESTADO_LABEL[estado].toLowerCase()}`}.
          </p>
        ) : (
          <ul ref={lista} id="productos-opciones" role="listbox" className="max-h-72 overflow-y-auto p-1">
            {filtrados.map((p, i) => {
              const seleccionado = String(p.productoId) === value
              return (
                <li
                  key={p.productoId}
                  data-indice={i}
                  role="option"
                  aria-selected={seleccionado}
                  onMouseMove={() => setActivo(i)}
                  onClick={() => elegir(p)}
                  className={cn('flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm', i === activo && 'bg-muted')}
                >
                  <span className={cn('size-2 shrink-0 rounded-full', PUNTO_ESTADO[p.estado])} aria-hidden />
                  <span className="min-w-0 flex-1 truncate">
                    <span className="font-mono text-[13px]">{p.codigo}</span> · {p.nombre}
                  </span>
                  <span className="shrink-0 text-[13px] text-muted-foreground tabular-nums">{fmtNumero(p.cantidad)}</span>
                  <Check className={cn('size-4 shrink-0', seleccionado ? 'opacity-100' : 'opacity-0')} aria-hidden />
                </li>
              )
            })}
          </ul>
        )}
      </PopoverContent>
    </Popover>
  )
}
