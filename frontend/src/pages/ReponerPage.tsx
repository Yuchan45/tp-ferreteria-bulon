import { useState } from 'react'
import { ArrowDown, CircleCheck, Package, TriangleAlert } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { listarAReponer, proveedoresDeProducto } from '@/api/productos'
import type { EstadoStock, ProductoProveedor } from '@/api/types'
import { StockBadge } from '@/components/badges'
import { Aviso, CeldaProducto, ErrorCarga, EstadoVacio, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { useRegistrarMovimiento } from '@/components/dialogs/registrar-context'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useSesion } from '@/hooks/auth-context'
import { useApi } from '@/hooks/useApi'
import { fmtNumero, fmtPrecio, incluye } from '@/lib/format'
import { faltante, porUrgencia } from '@/lib/stock'

const TODOS = 'todos'

export function ReponerPage() {
  const navigate = useNavigate()
  const { sucursal } = useSesion()
  const abrirMovimiento = useRegistrarMovimiento()
  // Cada fila trae su proveedor preferido (si tiene) para sugerir a quién comprar
  const filas = useApi(
    async (signal) => {
      const lista = await listarAReponer(sucursal.id, signal)
      return Promise.all(
        lista.map(async (s) => {
          const proveedores = await proveedoresDeProducto(s.productoId, signal)
          return { ...s, preferido: proveedores.find((p) => p.preferido) as ProductoProveedor | undefined }
        }),
      )
    },
    [sucursal.id],
  )
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState<EstadoStock | typeof TODOS>(TODOS)

  if (filas.error) return <ErrorCarga error={filas.error} onRetry={filas.reload} />
  if (!filas.data) return <TablaCargando filas={6} columnas={7} />

  if (filas.data.length === 0)
    return (
      <EstadoVacio
        icon={CircleCheck}
        tono="ok"
        titulo="Todo en orden"
        descripcion={`No hay productos en o por debajo del punto de pedido en ${sucursal.nombre}.`}
        accion={
          <Button variant="outline" asChild>
            <Link to="/productos">
              <Package /> Ver productos
            </Link>
          </Button>
        }
      />
    )

  const lista = [...filas.data]
    .filter((s) => !busqueda || incluye(s.codigo, busqueda) || incluye(s.nombre, busqueda))
    .filter((s) => estado === TODOS || s.estado === estado)
    .sort(porUrgencia)

  return (
    <>
      <Aviso icon={TriangleAlert} tono="bajo" titulo={`${filas.data.length} productos están en o por debajo del punto de pedido`}>
        Ordenados por urgencia: primero los críticos (en o por debajo del mínimo), luego por faltante.
      </Aviso>
      <Toolbar>
        <SearchField value={busqueda} onChange={setBusqueda} placeholder="Buscar por código o nombre" />
        <Select value={estado} onValueChange={(v) => setEstado(v as EstadoStock)}>
          <SelectTrigger className="w-44" aria-label="Filtrar por estado">
            <SelectValue />
          </SelectTrigger>
          <SelectContent position="popper">
            <SelectItem value={TODOS}>Estado: Todos</SelectItem>
            <SelectItem value="CRITICO">Crítico</SelectItem>
            <SelectItem value="REPONER">A reponer</SelectItem>
          </SelectContent>
        </Select>
      </Toolbar>

      <div className="overflow-hidden rounded-lg border bg-card">
        <Table>
          <TableHeader className="bg-muted">
            <TableRow className="hover:bg-transparent">
              <TableHead className="pl-4 text-muted-foreground">Producto</TableHead>
              <TableHead className="text-muted-foreground">Proveedor preferido</TableHead>
              <TableHead className="text-right text-muted-foreground">Stock</TableHead>
              <TableHead className="text-right text-muted-foreground">Mín. / Pto. pedido</TableHead>
              <TableHead className="text-right text-muted-foreground">Faltante</TableHead>
              <TableHead className="text-muted-foreground">Estado</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {lista.map((s) => (
              <TableRow key={s.productoId} className="h-15 cursor-pointer" onClick={() => navigate(`/productos/${s.productoId}`)}>
                <TableCell className="pl-4">
                  <CeldaProducto descripcion={s.nombre} codigo={s.codigo} />
                </TableCell>
                <TableCell>
                  <p>{s.preferido?.razonSocial ?? '—'}</p>
                  {s.preferido?.precioCosto != null && <p className="text-[13px] text-muted-foreground">Costo {fmtPrecio(s.preferido.precioCosto)}</p>}
                </TableCell>
                <TableCell className="tabular text-right font-medium">{fmtNumero(s.cantidad)}</TableCell>
                <TableCell className="tabular text-right text-muted-foreground">
                  {fmtNumero(s.stockMinimo)} / {fmtNumero(s.puntoPedido)}
                </TableCell>
                <TableCell className="tabular text-right font-medium">{fmtNumero(faltante(s))}</TableCell>
                <TableCell>
                  <StockBadge estado={s.estado} />
                </TableCell>
                <TableCell className="pr-4 text-right">
                  <Button
                    variant="outline"
                    onClick={(e) => {
                      e.stopPropagation()
                      abrirMovimiento({
                        productoId: s.productoId,
                        tipo: 'INGRESO_COMPRA',
                        cantidad: faltante(s) || undefined,
                        proveedorId: s.preferido?.proveedorId,
                      })
                    }}
                  >
                    <ArrowDown /> Registrar compra
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {lista.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="py-10 text-center text-muted-foreground">
                  Ningún producto coincide con los filtros.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </>
  )
}
