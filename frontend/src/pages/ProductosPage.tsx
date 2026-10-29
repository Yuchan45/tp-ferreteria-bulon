import { useState } from 'react'
import { ChevronRight, PackageSearch, Plus } from 'lucide-react'
import { useNavigate } from 'react-router'
import { listarCategorias, listarStock } from '@/api/catalogo'
import { listarProductos } from '@/api/productos'
import type { EstadoStock } from '@/api/types'
import { StockBadge } from '@/components/badges'
import { ErrorCarga, EstadoVacio, POR_PAGINA, Paginacion, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { ProductoDialog } from '@/components/dialogs/ProductoDialog'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useSesion } from '@/hooks/auth-context'
import { useApi } from '@/hooks/useApi'
import { fmtNumero, fmtPrecio, incluye } from '@/lib/format'

const TODOS = 'todos'

export function ProductosPage() {
  const navigate = useNavigate()
  const { sucursal, esAdmin } = useSesion()
  const productos = useApi((signal) => listarProductos(signal))
  const stock = useApi((signal) => listarStock(sucursal.id, signal), [sucursal.id])
  const categorias = useApi((signal) => listarCategorias(signal))
  const [busqueda, setBusqueda] = useState('')
  const [categoria, setCategoria] = useState(TODOS)
  const [estado, setEstado] = useState<EstadoStock | typeof TODOS>(TODOS)
  const [pagina, setPagina] = useState(1)
  const [nuevo, setNuevo] = useState(0)

  const nombreCategoria = (id: number | null) => categorias.data?.find((c) => c.id === id)?.nombre ?? '—'
  const stockDe = (id: number) => stock.data?.find((s) => s.productoId === id)
  const filtrados = (productos.data ?? [])
    .filter((p) => !busqueda || incluye(p.codigo, busqueda) || incluye(p.nombre, busqueda))
    .filter((p) => categoria === TODOS || String(p.categoriaId) === categoria)
    .filter((p) => estado === TODOS || stockDe(p.id)?.estado === estado)
    .sort((a, b) => a.codigo.localeCompare(b.codigo))
  const visibles = filtrados.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA)
  const filtrar = <T,>(set: (v: T) => void) => (v: T) => {
    set(v)
    setPagina(1)
  }
  const error = productos.error ?? stock.error

  return (
    <>
      <Toolbar>
        <SearchField value={busqueda} onChange={filtrar(setBusqueda)} placeholder="Buscar por código o nombre" />
        <Select value={categoria} onValueChange={filtrar(setCategoria)}>
          <SelectTrigger className="w-56" aria-label="Filtrar por categoría">
            <SelectValue />
          </SelectTrigger>
          <SelectContent position="popper">
            <SelectItem value={TODOS}>Categoría: Todas</SelectItem>
            {categorias.data?.map((c) => (
              <SelectItem key={c.id} value={String(c.id)}>
                {c.nombre}
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
            <SelectItem value="REPONER">A reponer</SelectItem>
            <SelectItem value="CRITICO">Crítico</SelectItem>
          </SelectContent>
        </Select>
        <div className="flex-1" />
        {esAdmin && (
          <Button onClick={() => setNuevo((n) => n + 1)}>
            <Plus /> Nuevo producto
          </Button>
        )}
      </Toolbar>

      {error ? (
        <ErrorCarga
          error={error}
          onRetry={() => {
            productos.reload()
            stock.reload()
          }}
        />
      ) : !productos.data || !stock.data ? (
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
                  <TableHead className="text-muted-foreground">Nombre</TableHead>
                  <TableHead className="text-muted-foreground">Categoría</TableHead>
                  <TableHead className="text-right text-muted-foreground">Precio</TableHead>
                  <TableHead className="text-right text-muted-foreground">Stock</TableHead>
                  <TableHead className="text-right text-muted-foreground">Pto. pedido</TableHead>
                  <TableHead className="text-muted-foreground">Estado</TableHead>
                  <TableHead className="w-12" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibles.map((p) => {
                  const s = stockDe(p.id)
                  return (
                    <TableRow key={p.id} className="h-13 cursor-pointer" onClick={() => navigate(`/productos/${p.id}`)}>
                      <TableCell className="pl-4 font-mono text-[13px]">{p.codigo}</TableCell>
                      <TableCell>{p.nombre}</TableCell>
                      <TableCell className="text-muted-foreground">{nombreCategoria(p.categoriaId)}</TableCell>
                      <TableCell className="tabular text-right">{fmtPrecio(p.precio)}</TableCell>
                      <TableCell className="tabular text-right font-medium">{s ? fmtNumero(s.cantidad) : '—'}</TableCell>
                      <TableCell className="tabular text-right text-muted-foreground">{s ? fmtNumero(s.puntoPedido) : '—'}</TableCell>
                      <TableCell>{s && <StockBadge estado={s.estado} />}</TableCell>
                      <TableCell>
                        <ChevronRight className="size-4 text-muted-foreground" aria-label="Ver detalle" />
                      </TableCell>
                    </TableRow>
                  )
                })}
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
