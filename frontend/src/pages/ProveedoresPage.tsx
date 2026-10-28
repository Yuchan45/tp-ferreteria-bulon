import { useState } from 'react'
import { Plus, Truck } from 'lucide-react'
import { listarProductos } from '@/api/productos'
import { listarProveedores } from '@/api/proveedores'
import { ErrorCarga, EstadoVacio, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { ProveedorDialog } from '@/components/dialogs/ProveedorDialog'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useApi } from '@/hooks/useApi'
import { fmtNumero, incluye } from '@/lib/format'

export function ProveedoresPage() {
  const proveedores = useApi((signal) => listarProveedores(signal))
  const productos = useApi((signal) => listarProductos(signal))
  const [busqueda, setBusqueda] = useState('')
  const [nuevo, setNuevo] = useState(0)

  const cantidad = (id: number) => productos.data?.filter((p) => p.proveedorId === id).length
  const lista = (proveedores.data ?? []).filter((p) => !busqueda || incluye(p.razonSocial, busqueda) || incluye(p.cuit, busqueda))

  return (
    <>
      <Toolbar>
        <SearchField value={busqueda} onChange={setBusqueda} placeholder="Buscar por razón social o CUIT" />
        <div className="flex-1" />
        <Button onClick={() => setNuevo((n) => n + 1)}>
          <Plus /> Nuevo proveedor
        </Button>
      </Toolbar>

      {proveedores.error ? (
        <ErrorCarga error={proveedores.error} onRetry={proveedores.reload} />
      ) : !proveedores.data ? (
        <TablaCargando filas={5} />
      ) : lista.length === 0 ? (
        <EstadoVacio icon={Truck} titulo="No hay proveedores" descripcion={proveedores.data.length ? 'Ningún proveedor coincide con la búsqueda.' : 'Todavía no se cargó ningún proveedor.'} />
      ) : (
        <div className="overflow-hidden rounded-lg border bg-card">
          <Table>
            <TableHeader className="bg-muted">
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4 text-muted-foreground">Razón social</TableHead>
                <TableHead className="text-muted-foreground">CUIT</TableHead>
                <TableHead className="text-muted-foreground">Teléfono</TableHead>
                <TableHead className="text-muted-foreground">Email</TableHead>
                <TableHead className="pr-4 text-right text-muted-foreground">Productos</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {lista.map((p) => (
                <TableRow key={p.id} className="h-13">
                  <TableCell className="pl-4 font-medium">{p.razonSocial}</TableCell>
                  <TableCell className="font-mono text-[13px]">{p.cuit ?? '—'}</TableCell>
                  <TableCell className="text-muted-foreground">{p.telefono ?? '—'}</TableCell>
                  <TableCell className="text-muted-foreground">{p.email ?? '—'}</TableCell>
                  <TableCell className="tabular pr-4 text-right font-medium">{cantidad(p.id) == null ? '…' : fmtNumero(cantidad(p.id)!)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {nuevo > 0 && <ProveedorDialog key={nuevo} open onOpenChange={(o) => !o && setNuevo(0)} />}
    </>
  )
}
