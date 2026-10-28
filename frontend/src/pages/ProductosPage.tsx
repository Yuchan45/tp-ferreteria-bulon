import { useState } from 'react'
import { ChevronRight, PackageSearch, Plus } from 'lucide-react'
import { useNavigate } from 'react-router'
import { listarProductos } from '@/api/productos'
import { listarProveedores } from '@/api/proveedores'
import { StockBadge } from '@/components/badges'
import { ErrorCarga, EstadoVacio, POR_PAGINA, Paginacion, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { ProductoDialog } from '@/components/dialogs/ProductoDialog'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useApi } from '@/hooks/useApi'
import { fmtNumero, incluye } from '@/lib/format'
import { estadoStock, type EstadoStock } from '@/lib/stock'

const TODOS = 'todos'

export function ProductosPage() {
  const navigate = useNavigate()
  const productos = useApi((signal) => listarProductos(signal))
  const proveedores = useApi((signal) => listarProveedores(signal))
  const [busqueda, setBusqueda] = useState('')
  const [proveedor, setProveedor] = useState(TODOS)
  const [estado, setEstado] = useState<EstadoStock | typeof TODOS>(TODOS)
  const [pagina, setPagina] = useState(1)
  const [nuevo, setNuevo] = useState(0)

  const nombreProveedor = (id: number | null) => proveedores.data?.find((p) => p.id === id)?.razonSocial ?? '—'
  const filtrados = (productos.data ?? [])
    .filter((p) => !busqueda || incluye(p.codigo, busqueda) || incluye(p.descripcion, busqueda))
    .filter((p) => proveedor === TODOS || String(p.proveedorId) === proveedor)
    .filter((p) => estado === TODOS || estadoStock(p) === estado)
    .sort((a, b) => a.codigo.localeCompare(b.codigo))
  const visibles = filtrados.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA)
  const filtrar = <T,>(set: (v: T) => void) => (v: T) => {
    set(v)
    setPagina(1)
  }

  return (
    <>
      <Toolbar>
        <SearchField value={busqueda} onChange={filtrar(setBusqueda)} placeholder="Buscar por código o descripción" />
        <Select value={proveedor} onValueChange={filtrar(setProveedor)}>
          <SelectTrigger className="w-60" aria-label="Filtrar por proveedor">
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
        <Select value={estado} onValueChange={filtrar((v: string) => setEstado(v as EstadoStock))}>
          <SelectTrigger className="w-44" aria-label="Filtrar por estado">
            <SelectValue />
          </SelectTrigger>
          <SelectContent position="popper">
            <SelectItem value={TODOS}>Estado: Todos</SelectItem>
            <SelectItem value="OK">OK</SelectItem>
            <SelectItem value="BAJO">Stock bajo</SelectItem>
            <SelectItem value="SIN_STOCK">Sin stock</SelectItem>
          </SelectContent>
        </Select>
        <div className="flex-1" />
        <Button onClick={() => setNuevo((n) => n + 1)}>
          <Plus /> Nuevo producto
        </Button>
      </Toolbar>

      {productos.error ? (
        <ErrorCarga error={productos.error} onRetry={productos.reload} />
      ) : !productos.data ? (
        <TablaCargando />
      ) : filtrados.length === 0 ? (
        <EstadoVacio icon={PackageSearch} titulo="No hay productos" descripcion={productos.data.length ? 'Ningún producto coincide con los filtros.' : 'Todavía no se cargó ningún producto.'} />
      ) : (
        <>
          <div className="overflow-hidden rounded-lg border bg-card">
            <Table>
              <TableHeader className="bg-muted">
                <TableRow className="hover:bg-transparent">
                  <TableHead className="pl-4 text-muted-foreground">Código</TableHead>
                  <TableHead className="text-muted-foreground">Descripción</TableHead>
                  <TableHead className="text-muted-foreground">Proveedor</TableHead>
                  <TableHead className="text-right text-muted-foreground">Stock</TableHead>
                  <TableHead className="text-right text-muted-foreground">Mínimo</TableHead>
                  <TableHead className="text-muted-foreground">Estado</TableHead>
                  <TableHead className="w-12" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibles.map((p) => (
                  <TableRow key={p.id} className="h-13 cursor-pointer" onClick={() => navigate(`/productos/${p.id}`)}>
                    <TableCell className="pl-4 font-mono text-[13px]">{p.codigo}</TableCell>
                    <TableCell>{p.descripcion}</TableCell>
                    <TableCell className="text-muted-foreground">{nombreProveedor(p.proveedorId)}</TableCell>
                    <TableCell className="tabular text-right font-medium">{fmtNumero(p.stockActual)}</TableCell>
                    <TableCell className="tabular text-right text-muted-foreground">{fmtNumero(p.stockMinimo)}</TableCell>
                    <TableCell>
                      <StockBadge estado={estadoStock(p)} />
                    </TableCell>
                    <TableCell>
                      <ChevronRight className="size-4 text-muted-foreground" aria-label="Ver detalle" />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <Paginacion pagina={pagina} total={filtrados.length} onChange={setPagina} unidad="productos" />
        </>
      )}

      {nuevo > 0 && <ProductoDialog key={nuevo} open onOpenChange={(o) => !o && setNuevo(0)} />}
    </>
  )
}
