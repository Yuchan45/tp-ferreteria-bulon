import { ChevronRight } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { listarProductos } from '@/api/productos'
import { MovimientoBadge, StockBadge } from '@/components/badges'
import { CeldaProducto, ErrorCarga, EstadoVacio, KpiCard, Seccion } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useApi } from '@/hooks/useApi'
import { useMovimientosGlobales } from '@/hooks/useMovimientosGlobales'
import { esHoy, fmtDelta, fmtHora, fmtNumero } from '@/lib/format'
import { delta, estadoStock, faltante, porUrgencia } from '@/lib/stock'

export function PanelPage() {
  const navigate = useNavigate()
  const productos = useApi((signal) => listarProductos(signal))
  const movimientos = useMovimientosGlobales()

  if (productos.error) return <ErrorCarga error={productos.error} onRetry={productos.reload} />

  const lista = productos.data ?? []
  const bajos = lista.filter((p) => estadoStock(p) === 'BAJO').length
  const sinStock = lista.filter((p) => estadoStock(p) === 'SIN_STOCK').length
  const aReponer = lista.filter((p) => estadoStock(p) !== 'OK').sort(porUrgencia)
  const hoy = (movimientos.data ?? []).filter((m) => esHoy(m.fecha))
  const ingresosHoy = hoy.filter((m) => m.tipo === 'ENTRADA').length
  const salidasHoy = hoy.filter((m) => m.tipo === 'SALIDA').length
  const cargando = productos.loading && !productos.data
  const valor = (n: number) => (cargando ? <Skeleton className="h-9 w-14 bg-muted" /> : fmtNumero(n))

  return (
    <>
      <div className="grid grid-cols-4 gap-4">
        <KpiCard titulo="Productos activos" valor={valor(lista.length)} detalle="En catálogo" />
        <KpiCard tono="bajo" titulo="Stock bajo" valor={valor(bajos)} detalle="En o por debajo del mínimo" />
        <KpiCard tono="sin-stock" titulo="Sin stock" valor={valor(sinStock)} detalle="Reposición urgente" />
        <KpiCard
          tono="primario"
          titulo="Movimientos del día"
          valor={movimientos.loading && !movimientos.data ? <Skeleton className="h-9 w-14 bg-muted" /> : fmtNumero(hoy.length)}
          detalle={`${ingresosHoy} ingresos · ${salidasHoy} salidas`}
        />
      </div>

      <div className="grid grid-cols-[1fr_380px] items-start gap-4">
        <Seccion
          titulo="Alertas de reposición"
          acciones={
            <Button variant="ghost" asChild>
              <Link to="/reponer">
                Ver todas <ChevronRight />
              </Link>
            </Button>
          }
        >
          {!cargando && aReponer.length === 0 ? (
            <p className="py-6 text-center text-muted-foreground">No hay productos en o por debajo del stock mínimo.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="pl-0 text-muted-foreground">Producto</TableHead>
                  <TableHead className="text-right text-muted-foreground">Stock</TableHead>
                  <TableHead className="text-right text-muted-foreground">Mínimo</TableHead>
                  <TableHead className="text-right text-muted-foreground">Faltante</TableHead>
                  <TableHead className="text-muted-foreground">Estado</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {aReponer.slice(0, 5).map((p) => (
                  <TableRow key={p.id} className="h-13 cursor-pointer" onClick={() => navigate(`/productos/${p.id}`)}>
                    <TableCell className="pl-0">
                      <CeldaProducto descripcion={p.descripcion} codigo={p.codigo} />
                    </TableCell>
                    <TableCell className="tabular text-right font-medium">{fmtNumero(p.stockActual)}</TableCell>
                    <TableCell className="tabular text-right">{fmtNumero(p.stockMinimo)}</TableCell>
                    <TableCell className="tabular text-right font-medium">{fmtNumero(faltante(p))}</TableCell>
                    <TableCell>
                      <StockBadge estado={estadoStock(p)} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Seccion>

        <Seccion titulo="Últimos movimientos" className="gap-2">
          {movimientos.error ? (
            <p className="text-[13px] text-stock-out">{movimientos.error.message}</p>
          ) : movimientos.data && movimientos.data.length === 0 ? (
            <EstadoVacio titulo="Sin movimientos" descripcion="Todavía no se registró ningún movimiento." />
          ) : (
            <ul className="divide-y">
              {(movimientos.data ?? []).slice(0, 6).map((m) => (
                <li key={m.id} className="flex items-center gap-3 py-3">
                  <MovimientoBadge tipo={m.tipo} />
                  <Link to={`/productos/${m.producto.id}`} className="min-w-0 flex-1 hover:underline">
                    <p className="truncate">{m.producto.descripcion}</p>
                    <p className="text-[13px] text-muted-foreground">
                      {m.producto.codigo} · {esHoy(m.fecha) ? fmtHora(m.fecha) : new Date(m.fecha).toLocaleDateString('es-AR')}
                    </p>
                  </Link>
                  <span className="tabular font-medium">{fmtDelta(delta(m.tipo, m.cantidad))}</span>
                </li>
              ))}
              {movimientos.loading && !movimientos.data && Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="my-3 h-10 bg-muted" />)}
            </ul>
          )}
        </Seccion>
      </div>
    </>
  )
}
