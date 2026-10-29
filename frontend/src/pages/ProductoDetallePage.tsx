import { useState } from 'react'
import { ChevronRight, Pencil, Plus } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { listarMovimientos } from '@/api/movimientos'
import { obtenerProducto, proveedoresDeProducto, stockDeProducto } from '@/api/productos'
import { MovimientoBadge, StockBadge } from '@/components/badges'
import { ErrorCarga, KpiCard, Seccion, TablaCargando } from '@/components/common'
import { ProductoDialog } from '@/components/dialogs/ProductoDialog'
import { useRegistrarMovimiento } from '@/components/dialogs/registrar-context'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useSesion } from '@/hooks/auth-context'
import { useApi } from '@/hooks/useApi'
import { TIPO_LABEL, UNIDAD_LABEL, fmtDelta, fmtFechaHora, fmtNumero, fmtPrecio } from '@/lib/format'
import { conStockResultante, delta } from '@/lib/stock'

export function ProductoDetallePage() {
  const id = Number(useParams().id)
  const { sucursal, esAdmin } = useSesion()
  const abrirMovimiento = useRegistrarMovimiento()
  const producto = useApi((signal) => obtenerProducto(id, signal), [id])
  const stock = useApi((signal) => stockDeProducto(id, signal), [id])
  const proveedores = useApi((signal) => proveedoresDeProducto(id, signal), [id])
  const movimientos = useApi((signal) => listarMovimientos(id, sucursal.id, signal), [id, sucursal.id])
  const [editar, setEditar] = useState(0)

  if (producto.error) return <ErrorCarga error={producto.error} onRetry={producto.reload} />
  if (!producto.data) return <TablaCargando filas={5} />

  const p = producto.data
  const fila = stock.data?.find((s) => s.sucursalId === sucursal.id)
  const preferido = proveedores.data?.find((pr) => pr.preferido)
  const historial = movimientos.data && fila ? conStockResultante(movimientos.data, fila.cantidad) : undefined
  const sobrePedido = fila ? fila.cantidad - fila.puntoPedido : 0

  return (
    <>
      <nav className="-mb-1 flex items-center gap-1.5 text-[13px]" aria-label="Migas de pan">
        <Link to="/productos" className="text-primary hover:underline">
          Productos
        </Link>
        <ChevronRight className="size-3.5 text-muted-foreground" />
        <span className="text-muted-foreground">{p.codigo}</span>
      </nav>

      <div className="flex items-end gap-3">
        <div className="flex-1">
          <p className="font-mono text-[13px] text-muted-foreground">{p.codigo}</p>
          <div className="flex items-center gap-3">
            <h2 className="text-2xl font-semibold">{p.nombre}</h2>
            {fila && <StockBadge estado={fila.estado} />}
          </div>
          {p.descripcion && <p className="text-muted-foreground">{p.descripcion}</p>}
        </div>
        {esAdmin && (
          <Button variant="outline" onClick={() => setEditar((n) => n + 1)} disabled={!stock.data || !proveedores.data}>
            <Pencil /> Editar
          </Button>
        )}
        <Button onClick={() => abrirMovimiento({ productoId: p.id })}>
          <Plus /> Registrar movimiento
        </Button>
      </div>

      <div className="grid grid-cols-4 items-stretch gap-4">
        <KpiCard
          tono={!fila ? 'neutral' : fila.estado === 'OK' ? 'ok' : fila.estado === 'REPONER' ? 'bajo' : 'sin-stock'}
          titulo={`Stock · ${sucursal.nombre}`}
          valor={fila ? fmtNumero(fila.cantidad) : '—'}
          detalle={
            !fila
              ? UNIDAD_LABEL[p.unidadMedida]
              : sobrePedido > 0
                ? `${fmtNumero(sobrePedido)} por encima del punto de pedido`
                : sobrePedido === 0
                  ? 'Justo en el punto de pedido'
                  : `${fmtNumero(-sobrePedido)} por debajo del punto de pedido`
          }
        />
        <KpiCard
          titulo="Mínimo / Pto. pedido"
          valor={fila ? `${fmtNumero(fila.stockMinimo)} / ${fmtNumero(fila.puntoPedido)}` : '—'}
          detalle={`Total en todas las sucursales: ${fmtNumero(p.stockTotal)}`}
        />
        <KpiCard titulo="Precio" valor={fmtPrecio(p.precio)} detalle={`Por ${UNIDAD_LABEL[p.unidadMedida].toLowerCase()}`} />
        <div className="flex flex-col gap-2 rounded-lg border bg-card p-5">
          <p className="text-[13px] font-medium text-muted-foreground">Proveedor preferido</p>
          {!proveedores.data ? (
            <p className="text-muted-foreground">Cargando…</p>
          ) : preferido ? (
            <>
              <p className="font-medium">{preferido.razonSocial}</p>
              <p className="text-[13px] text-muted-foreground">
                Costo {fmtPrecio(preferido.precioCosto)}
                {preferido.plazoEntregaDias != null && ` · entrega en ${preferido.plazoEntregaDias} días`}
              </p>
              {proveedores.data.length > 1 && (
                <p className="text-[13px] text-muted-foreground">
                  {proveedores.data.length - 1} {proveedores.data.length === 2 ? 'alternativo' : 'alternativos'}
                </p>
              )}
            </>
          ) : (
            <p className="text-muted-foreground">Sin proveedor asignado</p>
          )}
        </div>
      </div>

      <Seccion
        titulo={`Historial de movimientos · ${sucursal.nombre}`}
        acciones={historial && <span className="text-[13px] text-muted-foreground">{historial.length} {historial.length === 1 ? 'movimiento' : 'movimientos'}</span>}
      >
        {movimientos.error ? (
          <ErrorCarga error={movimientos.error} onRetry={movimientos.reload} />
        ) : !historial ? (
          <TablaCargando filas={4} columnas={5} />
        ) : historial.length === 0 ? (
          <p className="py-6 text-center text-muted-foreground">Este producto todavía no tiene movimientos en la sucursal.</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-0 text-muted-foreground">Fecha</TableHead>
                <TableHead className="text-muted-foreground">Tipo</TableHead>
                <TableHead className="text-right text-muted-foreground">Cantidad</TableHead>
                <TableHead className="text-right text-muted-foreground">Stock resultante</TableHead>
                <TableHead className="text-muted-foreground">Detalle</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {historial.map((m) => (
                <TableRow key={m.id} className="h-13">
                  <TableCell className="pl-0 text-muted-foreground">{fmtFechaHora(m.fecha)}</TableCell>
                  <TableCell>
                    <MovimientoBadge tipo={m.tipo} />
                  </TableCell>
                  <TableCell className="tabular text-right font-medium">{fmtDelta(delta(m.tipo, m.cantidad))}</TableCell>
                  <TableCell className="tabular text-right">{fmtNumero(m.stockResultante)}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {[m.motivo ?? m.observaciones, m.referencia].filter(Boolean).join(' · ') || TIPO_LABEL[m.tipo]}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Seccion>

      {editar > 0 && <ProductoDialog key={editar} open producto={p} stock={fila} preferido={preferido} onOpenChange={(o) => !o && setEditar(0)} />}
    </>
  )
}
