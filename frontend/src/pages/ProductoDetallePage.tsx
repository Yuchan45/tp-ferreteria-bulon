import { useState } from 'react'
import { ChevronRight, Mail, Pencil, Phone, Plus } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { listarMovimientos } from '@/api/movimientos'
import { obtenerProducto } from '@/api/productos'
import { listarProveedores } from '@/api/proveedores'
import { MovimientoBadge, StockBadge } from '@/components/badges'
import { ErrorCarga, KpiCard, Seccion, TablaCargando } from '@/components/common'
import { ProductoDialog } from '@/components/dialogs/ProductoDialog'
import { useRegistrarMovimiento } from '@/components/dialogs/registrar-context'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useApi } from '@/hooks/useApi'
import { fmtDelta, fmtFechaHora, fmtNumero, fmtPrecio } from '@/lib/format'
import { conStockResultante, delta, estadoStock } from '@/lib/stock'

export function ProductoDetallePage() {
  const id = Number(useParams().id)
  const abrirMovimiento = useRegistrarMovimiento()
  const producto = useApi((signal) => obtenerProducto(id, signal), [id])
  const movimientos = useApi((signal) => listarMovimientos(id, signal), [id])
  const proveedores = useApi((signal) => listarProveedores(signal))
  const [editar, setEditar] = useState(0)

  if (producto.error) return <ErrorCarga error={producto.error} onRetry={producto.reload} />
  if (!producto.data) return <TablaCargando filas={5} />

  const p = producto.data
  const estado = estadoStock(p)
  const proveedor = proveedores.data?.find((pr) => pr.id === p.proveedorId)
  const historial = movimientos.data ? conStockResultante(movimientos.data, p.stockActual) : undefined
  const diferencia = p.stockActual - p.stockMinimo

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
            <h2 className="text-2xl font-semibold">{p.descripcion}</h2>
            <StockBadge estado={estado} />
          </div>
        </div>
        <Button variant="outline" onClick={() => setEditar((n) => n + 1)}>
          <Pencil /> Editar
        </Button>
        <Button onClick={() => abrirMovimiento({ productoId: p.id })}>
          <Plus /> Registrar movimiento
        </Button>
      </div>

      <div className="grid grid-cols-4 items-stretch gap-4">
        <KpiCard
          tono={estado === 'OK' ? 'ok' : estado === 'BAJO' ? 'bajo' : 'sin-stock'}
          titulo="Stock actual"
          valor={fmtNumero(p.stockActual)}
          detalle={diferencia > 0 ? `${fmtNumero(diferencia)} por encima del mínimo` : diferencia === 0 ? 'Justo en el mínimo' : `${fmtNumero(-diferencia)} unidades por debajo del mínimo`}
        />
        <KpiCard titulo="Stock mínimo" valor={fmtNumero(p.stockMinimo)} detalle="Umbral de reposición" />
        <KpiCard titulo="Precio" valor={fmtPrecio(p.precio)} detalle="Por unidad" />
        <div className="flex flex-col gap-2 rounded-lg border bg-card p-5">
          <p className="text-[13px] font-medium text-muted-foreground">Proveedor</p>
          {proveedor ? (
            <>
              <p className="font-medium">{proveedor.razonSocial}</p>
              {proveedor.telefono && (
                <p className="flex items-center gap-1.5 text-[13px] text-muted-foreground">
                  <Phone className="size-3.5" /> {proveedor.telefono}
                </p>
              )}
              {proveedor.email && (
                <p className="flex items-center gap-1.5 text-[13px] text-muted-foreground">
                  <Mail className="size-3.5" /> {proveedor.email}
                </p>
              )}
            </>
          ) : (
            <p className="text-muted-foreground">{p.proveedorId ? 'Cargando…' : 'Sin proveedor asignado'}</p>
          )}
        </div>
      </div>

      <Seccion
        titulo="Historial de movimientos"
        acciones={historial && <span className="text-[13px] text-muted-foreground">{historial.length} {historial.length === 1 ? 'movimiento' : 'movimientos'}</span>}
      >
        {movimientos.error ? (
          <ErrorCarga error={movimientos.error} onRetry={movimientos.reload} />
        ) : !historial ? (
          <TablaCargando filas={4} columnas={5} />
        ) : historial.length === 0 ? (
          <p className="py-6 text-center text-muted-foreground">Este producto todavía no tiene movimientos.</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-0 text-muted-foreground">Fecha</TableHead>
                <TableHead className="text-muted-foreground">Tipo</TableHead>
                <TableHead className="text-right text-muted-foreground">Cantidad</TableHead>
                <TableHead className="text-right text-muted-foreground">Stock resultante</TableHead>
                <TableHead className="text-muted-foreground">Observación</TableHead>
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
                  <TableCell className="text-muted-foreground">{m.observacion ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Seccion>

      {editar > 0 && <ProductoDialog key={editar} open producto={p} onOpenChange={(o) => !o && setEditar(0)} />}
    </>
  )
}
