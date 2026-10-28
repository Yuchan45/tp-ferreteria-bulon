import { useState } from 'react'
import { ArrowDown, CircleCheck, Package, TriangleAlert } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { listarBajoStock } from '@/api/productos'
import { listarProveedores } from '@/api/proveedores'
import { StockBadge } from '@/components/badges'
import { Aviso, CeldaProducto, ErrorCarga, EstadoVacio, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { useRegistrarMovimiento } from '@/components/dialogs/registrar-context'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useApi } from '@/hooks/useApi'
import { fmtNumero, incluye } from '@/lib/format'
import { estadoStock, faltante, porUrgencia } from '@/lib/stock'

const TODOS = 'todos'

export function ReponerPage() {
  const navigate = useNavigate()
  const abrirMovimiento = useRegistrarMovimiento()
  const productos = useApi((signal) => listarBajoStock(signal))
  const proveedores = useApi((signal) => listarProveedores(signal))
  const [busqueda, setBusqueda] = useState('')
  const [proveedor, setProveedor] = useState(TODOS)

  if (productos.error) return <ErrorCarga error={productos.error} onRetry={productos.reload} />
  if (!productos.data) return <TablaCargando filas={6} columnas={7} />

  if (productos.data.length === 0)
    return (
      <EstadoVacio
        icon={CircleCheck}
        tono="ok"
        titulo="Todo en orden"
        descripcion="No hay productos en o por debajo del stock mínimo."
        accion={
          <Button variant="outline" asChild>
            <Link to="/productos">
              <Package /> Ver productos
            </Link>
          </Button>
        }
      />
    )

  const lista = [...productos.data]
    .filter((p) => !busqueda || incluye(p.codigo, busqueda) || incluye(p.descripcion, busqueda))
    .filter((p) => proveedor === TODOS || String(p.proveedorId) === proveedor)
    .sort(porUrgencia)
  const prov = (id: number | null) => proveedores.data?.find((p) => p.id === id)

  return (
    <>
      <Aviso icon={TriangleAlert} tono="bajo" titulo={`${productos.data.length} productos están en o por debajo del stock mínimo`}>
        Ordenados por urgencia: primero sin stock, luego por faltante.
      </Aviso>
      <Toolbar>
        <SearchField value={busqueda} onChange={setBusqueda} placeholder="Buscar por código o descripción" />
        <Select value={proveedor} onValueChange={setProveedor}>
          <SelectTrigger className="w-64" aria-label="Filtrar por proveedor">
            <SelectValue />
          </SelectTrigger>
          <SelectContent position="popper">
            <SelectItem value={TODOS}>Proveedor: Todos</SelectItem>
            {proveedores.data?.map((p) => (
              <SelectItem key={p.id} value={String(p.id)}>
                {p.razonSocial}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Toolbar>

      <div className="overflow-hidden rounded-lg border bg-card">
        <Table>
          <TableHeader className="bg-muted">
            <TableRow className="hover:bg-transparent">
              <TableHead className="pl-4 text-muted-foreground">Producto</TableHead>
              <TableHead className="text-muted-foreground">Proveedor</TableHead>
              <TableHead className="text-right text-muted-foreground">Stock</TableHead>
              <TableHead className="text-right text-muted-foreground">Mínimo</TableHead>
              <TableHead className="text-right text-muted-foreground">Faltante</TableHead>
              <TableHead className="text-muted-foreground">Estado</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {lista.map((p) => {
              const pr = prov(p.proveedorId)
              return (
                <TableRow key={p.id} className="h-15 cursor-pointer" onClick={() => navigate(`/productos/${p.id}`)}>
                  <TableCell className="pl-4">
                    <CeldaProducto descripcion={p.descripcion} codigo={p.codigo} />
                  </TableCell>
                  <TableCell>
                    <p>{pr?.razonSocial ?? '—'}</p>
                    {pr?.telefono && <p className="text-[13px] text-muted-foreground">{pr.telefono}</p>}
                  </TableCell>
                  <TableCell className="tabular text-right font-medium">{fmtNumero(p.stockActual)}</TableCell>
                  <TableCell className="tabular text-right text-muted-foreground">{fmtNumero(p.stockMinimo)}</TableCell>
                  <TableCell className="tabular text-right font-medium">{fmtNumero(faltante(p))}</TableCell>
                  <TableCell>
                    <StockBadge estado={estadoStock(p)} />
                  </TableCell>
                  <TableCell className="pr-4 text-right">
                    <Button
                      variant="outline"
                      onClick={(e) => {
                        e.stopPropagation()
                        abrirMovimiento({ productoId: p.id, tipo: 'ENTRADA', cantidad: faltante(p) || undefined })
                      }}
                    >
                      <ArrowDown /> Registrar ingreso
                    </Button>
                  </TableCell>
                </TableRow>
              )
            })}
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
