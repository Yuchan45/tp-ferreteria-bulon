import { useState } from 'react'
import { Pencil, Plus, Truck } from 'lucide-react'
import { listarProveedores } from '@/api/proveedores'
import type { Proveedor } from '@/api/types'
import { ErrorCarga, EstadoVacio, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { ProveedorDialog } from '@/components/dialogs/ProveedorDialog'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useSesion } from '@/hooks/auth-context'
import { useApi } from '@/hooks/useApi'
import { incluye } from '@/lib/format'
import { cn } from '@/lib/utils'

export function ProveedoresPage() {
  const { esAdmin } = useSesion()
  const proveedores = useApi((signal) => listarProveedores(signal))
  const [busqueda, setBusqueda] = useState('')
  // Cada apertura remonta el diálogo (key) para que arranque con los datos del proveedor elegido
  const [dialogo, setDialogo] = useState<{ key: number; proveedor?: Proveedor } | null>(null)
  const abrir = (proveedor?: Proveedor) => setDialogo((d) => ({ key: (d?.key ?? 0) + 1, proveedor }))

  const lista = (proveedores.data ?? []).filter((p) => !busqueda || incluye(p.razonSocial, busqueda) || incluye(p.cuit, busqueda))

  return (
    <>
      <Toolbar>
        <SearchField value={busqueda} onChange={setBusqueda} placeholder="Buscar por razón social o CUIT" />
        <div className="flex-1" />
        {esAdmin && (
          <Button onClick={() => abrir()}>
            <Plus /> Nuevo proveedor
          </Button>
        )}
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
                <TableHead className="text-muted-foreground">Contacto</TableHead>
                <TableHead className={cn('text-muted-foreground', !esAdmin && 'pr-4')}>Estado</TableHead>
                {esAdmin && <TableHead className="pr-4" />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {lista.map((p) => (
                <TableRow key={p.id} className={cn('h-13', !p.activo && 'text-muted-foreground')}>
                  <TableCell className="pl-4 font-medium">{p.razonSocial}</TableCell>
                  <TableCell className="font-mono text-[13px]">{p.cuit ?? '—'}</TableCell>
                  <TableCell className="text-muted-foreground">{p.telefono ?? '—'}</TableCell>
                  <TableCell className="max-w-48 truncate text-muted-foreground" title={p.email ?? undefined}>{p.email ?? '—'}</TableCell>
                  <TableCell className="text-muted-foreground">{p.contacto ?? '—'}</TableCell>
                  <TableCell className={cn(!esAdmin && 'pr-4')}>
                    <span className="flex items-center gap-1.5">
                      <span className={cn('size-2 rounded-full', p.activo ? 'bg-stock-ok' : 'bg-muted-foreground')} aria-hidden />
                      {p.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </TableCell>
                  {esAdmin && (
                    <TableCell className="pr-4 text-right">
                      <Button variant="ghost" size="icon-sm" onClick={() => abrir(p)} aria-label={`Editar ${p.razonSocial}`} title="Editar">
                        <Pencil />
                      </Button>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {dialogo && <ProveedorDialog key={dialogo.key} open proveedor={dialogo.proveedor} onOpenChange={(o) => !o && setDialogo(null)} />}
    </>
  )
}
