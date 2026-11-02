import { useState, type ReactNode } from 'react'
import { ArrowLeftRight, FileSpreadsheet, FileText, Loader2, Package, TriangleAlert, type LucideIcon } from 'lucide-react'
import { listarCategorias, listarStock } from '@/api/catalogo'
import type { FiltrosReporte, FormatoReporte, TipoReporte } from '@/api/reportes'
import type { EstadoStock, TipoMovimiento } from '@/api/types'
import { FiltroEstado } from '@/components/FiltroEstado'
import { Campo } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useSesion } from '@/hooks/auth-context'
import { useApi } from '@/hooks/useApi'
import { useDescargaReporte } from '@/hooks/useDescargaReporte'
import { TIPO_LABEL } from '@/lib/format'
import { cn } from '@/lib/utils'

const TODOS = 'todos'

/** yyyy-mm-dd en hora local, para los <input type="date">. */
const dia = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

const hace = (dias: number) => {
  const d = new Date()
  d.setDate(d.getDate() - dias)
  return d
}

const PERIODOS: { label: string; desde: () => string }[] = [
  { label: 'Hoy', desde: () => dia(new Date()) },
  { label: 'Últimos 7 días', desde: () => dia(hace(6)) },
  { label: 'Este mes', desde: () => dia(new Date(new Date().getFullYear(), new Date().getMonth(), 1)) },
]

/**
 * Los tres reportes con sus filtros, en un solo lugar. Cada vista (Productos, Movimientos,
 * A reponer) también tiene su botón "Exportar" con los filtros que se están viendo.
 */
export function ReportesPage() {
  const { sucursal } = useSesion()
  const stock = useApi((signal) => listarStock(sucursal.id, signal), [sucursal.id])
  const categorias = useApi((signal) => listarCategorias(signal))

  // Las fechas se calculan una vez al abrir la vista (no en cada render)
  const [hoy] = useState(() => dia(new Date()))
  const [periodos] = useState(() => PERIODOS.map((p) => ({ label: p.label, desde: p.desde() })))
  const [categoria, setCategoria] = useState(TODOS)
  const [estadoStock, setEstadoStock] = useState<EstadoStock | null>(null)
  const [desde, setDesde] = useState(periodos[2].desde)
  const [hasta, setHasta] = useState(hoy)
  const [tipo, setTipo] = useState<TipoMovimiento | typeof TODOS>(TODOS)
  const [estadoReponer, setEstadoReponer] = useState<EstadoStock | null>(null)

  const lista = stock.data ?? []
  const contar = (e: EstadoStock | null, soloAReponer = false) =>
    lista.filter((s) => (soloAReponer ? s.estado !== 'OK' : true) && (!e || s.estado === e)).length

  return (
    <>
      <p className="text-muted-foreground">
        Generá los reportes de <span className="font-medium text-foreground">{sucursal.nombre}</span> en PDF, para imprimir o compartir, o en
        Excel, para analizar los datos. También podés exportar desde cada vista con los filtros que estés viendo.
      </p>

      <div className="grid gap-6 lg:grid-cols-3">
        <TarjetaReporte
          tipo="stock"
          icono={Package}
          titulo="Stock por sucursal"
          descripcion="Existencias de cada producto con su mínimo, punto de pedido y estado."
          filtros={{
            sucursalId: sucursal.id,
            categoriaId: categoria === TODOS ? null : Number(categoria),
            estado: estadoStock,
          }}
        >
          <Campo label="Categoría" htmlFor="rep-categoria">
            <Select value={categoria} onValueChange={setCategoria}>
              <SelectTrigger id="rep-categoria" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent position="popper">
                <SelectItem value={TODOS}>Todas</SelectItem>
                {categorias.data?.map((c) => (
                  <SelectItem key={c.id} value={String(c.id)}>
                    {c.nombre}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Campo>
          <Filtro label="Estado">
            <FiltroEstado estados={['OK', 'REPONER', 'CRITICO']} value={estadoStock} onChange={setEstadoStock} contar={(e) => contar(e)} />
          </Filtro>
        </TarjetaReporte>

        <TarjetaReporte
          tipo="movimientos"
          icono={ArrowLeftRight}
          titulo="Movimientos por período"
          descripcion="Compras, ventas, devoluciones, roturas y ajustes, con su responsable y detalle."
          filtros={{ sucursalId: sucursal.id, desde, hasta, tipo: tipo === TODOS ? null : tipo }}
        >
          <Filtro label="Período">
            <div className="flex flex-wrap gap-1.5">
              {periodos.map((p) => {
                const activo = desde === p.desde && hasta === hoy
                return (
                  <button
                    key={p.label}
                    type="button"
                    aria-pressed={activo}
                    onClick={() => {
                      setDesde(p.desde)
                      setHasta(hoy)
                    }}
                    className={cn(
                      'rounded-full border px-2.5 py-0.5 text-[13px] transition-colors',
                      activo ? 'border-foreground/20 bg-muted font-medium text-foreground' : 'bg-card text-muted-foreground hover:bg-muted/60',
                    )}
                  >
                    {p.label}
                  </button>
                )
              })}
            </div>
            <div className="grid grid-cols-2 gap-2">
              <Input type="date" aria-label="Desde" value={desde} max={hasta || undefined} onChange={(e) => setDesde(e.target.value)} className="px-2" />
              <Input type="date" aria-label="Hasta" value={hasta} min={desde || undefined} onChange={(e) => setHasta(e.target.value)} className="px-2" />
            </div>
          </Filtro>
          <Campo label="Tipo" htmlFor="rep-tipo">
            <Select value={tipo} onValueChange={(v) => setTipo(v as TipoMovimiento)}>
              <SelectTrigger id="rep-tipo" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent position="popper">
                <SelectItem value={TODOS}>Todos</SelectItem>
                {(Object.keys(TIPO_LABEL) as TipoMovimiento[]).map((t) => (
                  <SelectItem key={t} value={t}>
                    {TIPO_LABEL[t]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Campo>
        </TarjetaReporte>

        <TarjetaReporte
          tipo="a-reponer"
          icono={TriangleAlert}
          titulo="Productos a reponer"
          descripcion="Lo que está en o por debajo del punto de pedido, con el faltante, el proveedor preferido y el costo estimado."
          filtros={{ sucursalId: sucursal.id, estado: estadoReponer }}
        >
          <Filtro label="Estado">
            <FiltroEstado estados={['CRITICO', 'REPONER']} value={estadoReponer} onChange={setEstadoReponer} contar={(e) => contar(e, true)} />
          </Filtro>
        </TarjetaReporte>
      </div>
    </>
  )
}

function Filtro({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-[13px] font-medium">{label}</span>
      {children}
    </div>
  )
}

function TarjetaReporte({
  tipo,
  icono: Icono,
  titulo,
  descripcion,
  filtros,
  children,
}: {
  tipo: TipoReporte
  icono: LucideIcon
  titulo: string
  descripcion: string
  filtros: FiltrosReporte
  children: ReactNode
}) {
  const { generando, descargar } = useDescargaReporte()
  const boton = (formato: FormatoReporte, label: string, Icon: LucideIcon) => (
    <Button variant={formato === 'pdf' ? 'default' : 'outline'} className="flex-1" disabled={generando != null} onClick={() => descargar(tipo, formato, filtros)}>
      {generando === formato ? <Loader2 className="animate-spin" /> : <Icon />}
      {generando === formato ? 'Generando…' : label}
    </Button>
  )

  return (
    <section className="flex flex-col gap-5 rounded-lg border bg-card p-5">
      <header className="flex gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted">
          <Icono className="size-5 text-muted-foreground" aria-hidden />
        </div>
        <div>
          <h2 className="text-lg font-semibold">{titulo}</h2>
          <p className="text-[13px] text-muted-foreground">{descripcion}</p>
        </div>
      </header>
      <div className="flex flex-1 flex-col gap-4">{children}</div>
      <div className="flex gap-2 border-t pt-4">
        {boton('pdf', 'PDF', FileText)}
        {boton('excel', 'Excel', FileSpreadsheet)}
      </div>
    </section>
  )
}
