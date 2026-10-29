import { ChevronRight } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { listarStock } from '@/api/catalogo'
import { MovimientoBadge, StockBadge } from '@/components/badges'
import { CeldaProducto, ErrorCarga, EstadoVacio, KpiCard, Seccion } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useSesion } from '@/hooks/auth-context'
import { useApi } from '@/hooks/useApi'
import { useMovimientosGlobales } from '@/hooks/useMovimientosGlobales'
import { esHoy, fmtDelta, fmtHora, fmtNumero } from '@/lib/format'
import { delta, faltante, porUrgencia } from '@/lib/stock'

export function PanelPage() {
  const navigate = useNavigate()
  const { sucursal } = useSesion()
  const stock = useApi((signal) => listarStock(sucursal.id, signal), [sucursal.id])
  const movimientos = useMovimientosGlobales(sucursal.id)

  if (stock.error) return <ErrorCarga error={stock.error} onRetry={stock.reload} />

  const lista = stock.data ?? []
  const aReponer = lista.filter((s) => s.estado === 'REPONER').length
  const criticos = lista.filter((s) => s.estado === 'CRITICO').length
  const alertas = lista.filter((s) => s.estado !== 'OK').sort(porUrgencia)
  const hoy = (movimientos.data ?? []).filter((m) => esHoy(m.fecha))
  const ingresosHoy = hoy.filter((m) => delta(m.tipo, m.cantidad) > 0).length
  const salidasHoy = hoy.filter((m) => delta(m.tipo, m.cantidad) < 0).length
  const cargando = stock.loading && !stock.data
  const valor = (n: number) => (cargando ? <Skeleton className="h-9 w-14 bg-muted" /> : fmtNumero(n))

  return (
    <>
      <div className="grid grid-cols-4 gap-4">
        <KpiCard titulo="Productos" valor={valor(lista.length)} detalle={sucursal.nombre} />
        <KpiCard tono="bajo" titulo="A reponer" valor={valor(aReponer)} detalle="En o por debajo del punto de pedido" />
        <KpiCard tono="sin-stock" titulo="Críticos" valor={valor(criticos)} detalle="En o por debajo del mínimo" />
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
          {!cargando && alertas.length === 0 ? (
            <p className="py-6 text-center text-muted-foreground">No hay productos en o por debajo del punto de pedido.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="pl-0 text-muted-foreground">Producto</TableHead>
                  <TableHead className="text-right text-muted-foreground">Stock</TableHead>
                  <TableHead className="text-right text-muted-foreground">Pto. pedido</TableHead>
                  <TableHead className="text-right text-muted-foreground">Faltante</TableHead>
                  <TableHead className="text-muted-foreground">Estado</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {alertas.slice(0, 5).map((s) => (
                  <TableRow key={s.productoId} className="h-13 cursor-pointer" onClick={() => navigate(`/productos/${s.productoId}`)}>
                    <TableCell className="pl-0">
                      <CeldaProducto descripcion={s.nombre} codigo={s.codigo} />
                    </TableCell>
                    <TableCell className="tabular text-right font-medium">{fmtNumero(s.cantidad)}</TableCell>
                    <TableCell className="tabular text-right">{fmtNumero(s.puntoPedido)}</TableCell>
                    <TableCell className="tabular text-right font-medium">{fmtNumero(faltante(s))}</TableCell>
                    <TableCell>
                      <StockBadge estado={s.estado} />
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
                  <Link to={`/productos/${m.productoId}`} className="min-w-0 flex-1 hover:underline">
                    <p className="truncate">{m.producto.nombre}</p>
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
