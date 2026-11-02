import type { ReactNode } from 'react'
import { ChevronLeft, ChevronRight, Inbox, RefreshCw, Search, TriangleAlert, type LucideIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

type Tono = 'neutral' | 'primario' | 'ok' | 'bajo' | 'sin-stock'

const PUNTO: Record<Tono, string> = {
  neutral: 'bg-muted-foreground',
  primario: 'bg-primary',
  ok: 'bg-stock-ok',
  bajo: 'bg-stock-low',
  'sin-stock': 'bg-stock-out',
}

/** Indicador del panel: número grande en texto principal y un punto chico de color. */
export function KpiCard({ titulo, valor, detalle, tono = 'neutral' }: { titulo: string; valor: ReactNode; detalle?: ReactNode; tono?: Tono }) {
  return (
    <div className="flex flex-col gap-2 rounded-lg border bg-card p-5">
      <div className="flex items-center gap-2 text-[13px] font-medium text-muted-foreground">
        <span className={cn('size-2 rounded-full', PUNTO[tono])} aria-hidden />
        {titulo}
      </div>
      <div className="tabular text-3xl font-semibold">{valor}</div>
      {detalle && <div className="text-[13px] text-muted-foreground">{detalle}</div>}
    </div>
  )
}

/** Tarjeta con título de sección y acciones a la derecha. */
export function Seccion({ titulo, acciones, children, className }: { titulo: string; acciones?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={cn('flex flex-col gap-4 rounded-lg border bg-card p-5', className)}>
      <header className="flex items-center gap-3">
        <h2 className="flex-1 text-lg font-semibold">{titulo}</h2>
        {acciones}
      </header>
      {children}
    </section>
  )
}

export function Toolbar({ children }: { children: ReactNode }) {
  return <div className="flex flex-wrap items-center gap-3">{children}</div>
}

export function SearchField({ value, onChange, placeholder, className }: { value: string; onChange: (v: string) => void; placeholder: string; className?: string }) {
  return (
    <div className={cn('relative w-full max-w-90', className)}>
      <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
      <Input value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} className="pl-9" />
    </div>
  )
}

export function Aviso({ icon: Icon, tono, titulo, children, accion }: { icon: LucideIcon; tono: 'bajo' | 'error' | 'info'; titulo: string; children?: ReactNode; accion?: ReactNode }) {
  const estilos = {
    bajo: ['bg-stock-low-bg', 'text-stock-low'],
    error: ['bg-stock-out-bg', 'text-stock-out'],
    info: ['bg-mov-salida-bg', 'text-primary'],
  }[tono]
  return (
    <div role={tono === 'error' ? 'alert' : 'status'} className={cn('flex items-center gap-3 rounded-lg px-4 py-3', estilos[0])}>
      <Icon className={cn('size-5 shrink-0', estilos[1])} aria-hidden />
      <div className="flex-1">
        <p className="font-medium">{titulo}</p>
        {children && <p className="text-[13px] text-muted-foreground">{children}</p>}
      </div>
      {accion}
    </div>
  )
}

export function EstadoVacio({ icon: Icon = Inbox, titulo, descripcion, accion, tono = 'neutral' }: { icon?: LucideIcon; titulo: string; descripcion?: string; accion?: ReactNode; tono?: 'neutral' | 'ok' }) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-lg border bg-card px-6 py-16 text-center">
      <div className={cn('mb-1 flex size-14 items-center justify-center rounded-full', tono === 'ok' ? 'bg-stock-ok-bg text-stock-ok' : 'bg-muted text-muted-foreground')}>
        <Icon className="size-7" aria-hidden />
      </div>
      <p className="text-lg font-semibold">{titulo}</p>
      {descripcion && <p className="text-muted-foreground">{descripcion}</p>}
      {accion && <div className="mt-2">{accion}</div>}
    </div>
  )
}

/** Error al cargar datos, con opción de reintentar. */
export function ErrorCarga({ error, onRetry }: { error: Error; onRetry: () => void }) {
  return (
    <Aviso icon={TriangleAlert} tono="error" titulo="No se pudieron cargar los datos" accion={
      <Button variant="outline" onClick={onRetry}>
        <RefreshCw /> Reintentar
      </Button>
    }>
      {error.message}
    </Aviso>
  )
}

export function TablaCargando({ filas = 8, columnas = 6 }: { filas?: number; columnas?: number }) {
  return (
    <div className="overflow-hidden rounded-lg border bg-card" aria-busy="true" aria-label="Cargando">
      <div className="h-10 border-b bg-muted" />
      {Array.from({ length: filas }, (_, i) => (
        <div key={i} className="flex h-13 items-center gap-4 border-b px-4 last:border-0">
          {Array.from({ length: columnas }, (_, j) => (
            <Skeleton key={j} className={cn('h-3 bg-muted', j === 1 ? 'flex-1' : 'w-16')} />
          ))}
        </div>
      ))}
    </div>
  )
}

export const POR_PAGINA = 10

export function Paginacion({ pagina, total, porPagina = POR_PAGINA, onChange, unidad }: { pagina: number; total: number; porPagina?: number; onChange: (p: number) => void; unidad: string }) {
  const paginas = Math.max(1, Math.ceil(total / porPagina))
  const desde = total === 0 ? 0 : (pagina - 1) * porPagina + 1
  const hasta = Math.min(pagina * porPagina, total)
  return (
    <div className="flex items-center gap-2">
      <p className="flex-1 text-[13px] text-muted-foreground">
        Mostrando {desde}–{hasta} de {total} {unidad}
      </p>
      <Button variant="outline" disabled={pagina <= 1} onClick={() => onChange(pagina - 1)}>
        <ChevronLeft /> Anterior
      </Button>
      <Button variant="outline" disabled={pagina >= paginas} onClick={() => onChange(pagina + 1)}>
        Siguiente <ChevronRight />
      </Button>
    </div>
  )
}

/** Celda de producto: descripción y código (SKU) en monoespaciada. */
export function CeldaProducto({ descripcion, codigo, peso = 'medium' }: { descripcion: string; codigo: string; peso?: 'normal' | 'medium' }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className={peso === 'medium' ? 'font-medium' : undefined}>{descripcion}</span>
      <span className="font-mono text-[13px] text-muted-foreground">{codigo}</span>
    </div>
  )
}
