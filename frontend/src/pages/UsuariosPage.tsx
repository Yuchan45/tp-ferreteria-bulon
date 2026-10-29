import { useState } from 'react'
import { Plus, Users } from 'lucide-react'
import { listarSucursales } from '@/api/catalogo'
import type { Rol } from '@/api/types'
import { listarUsuarios } from '@/api/usuarios'
import { ErrorCarga, EstadoVacio, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { UsuarioDialog } from '@/components/dialogs/UsuarioDialog'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useApi } from '@/hooks/useApi'
import { ROL_LABEL, iniciales, incluye } from '@/lib/format'
import { cn } from '@/lib/utils'

const TODOS = 'todos'

export function UsuariosPage() {
  const usuarios = useApi((signal) => listarUsuarios(signal))
  const sucursales = useApi((signal) => listarSucursales(signal))
  const [busqueda, setBusqueda] = useState('')
  const [rol, setRol] = useState<Rol | typeof TODOS>(TODOS)
  const [nuevo, setNuevo] = useState(0)

  const lista = (usuarios.data ?? [])
    .filter((u) => !busqueda || incluye(u.nombre, busqueda) || incluye(u.username, busqueda))
    .filter((u) => rol === TODOS || u.rol === rol)
  // El administrador no tiene sucursal asignada: opera todas
  const nombreSucursal = (id: number | null) => (id == null ? 'Todas' : (sucursales.data?.find((s) => s.id === id)?.nombre ?? '…'))

  return (
    <>
      <Toolbar>
        <SearchField value={busqueda} onChange={setBusqueda} placeholder="Buscar por nombre o usuario" />
        <Select value={rol} onValueChange={(v) => setRol(v as Rol)}>
          <SelectTrigger className="w-56" aria-label="Filtrar por rol">
            <SelectValue />
          </SelectTrigger>
          <SelectContent position="popper">
            <SelectItem value={TODOS}>Rol: Todos</SelectItem>
            {(Object.keys(ROL_LABEL) as Rol[]).map((r) => (
              <SelectItem key={r} value={r}>
                {ROL_LABEL[r]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <div className="flex-1" />
        <Button onClick={() => setNuevo((n) => n + 1)}>
          <Plus /> Nuevo usuario
        </Button>
      </Toolbar>

      {usuarios.error ? (
        <ErrorCarga error={usuarios.error} onRetry={usuarios.reload} />
      ) : !usuarios.data ? (
        <TablaCargando filas={4} columnas={5} />
      ) : lista.length === 0 ? (
        <EstadoVacio icon={Users} titulo="No hay usuarios" descripcion="Ningún usuario coincide con los filtros." />
      ) : (
        <div className="overflow-hidden rounded-lg border bg-card">
          <Table>
            <TableHeader className="bg-muted">
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4 text-muted-foreground">Usuario</TableHead>
                <TableHead className="text-muted-foreground">Email</TableHead>
                <TableHead className="text-muted-foreground">Rol</TableHead>
                <TableHead className="text-muted-foreground">Sucursal</TableHead>
                <TableHead className="pr-4 text-muted-foreground">Estado</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {lista.map((u) => (
                <TableRow key={u.id} className="h-15">
                  <TableCell className="pl-4">
                    <div className="flex items-center gap-3">
                      <div className="flex size-8 items-center justify-center rounded-full bg-muted text-[13px] font-medium text-muted-foreground">{iniciales(u.nombre)}</div>
                      <div>
                        <p className="font-medium">{u.nombre}</p>
                        <p className="font-mono text-[13px] text-muted-foreground">{u.username}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{u.email ?? '—'}</TableCell>
                  <TableCell>
                    <span className="rounded-md border bg-muted px-2 py-0.5 text-[13px] font-medium">{ROL_LABEL[u.rol]}</span>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{nombreSucursal(u.sucursalId)}</TableCell>
                  <TableCell className="pr-4">
                    <span className={cn('flex items-center gap-1.5', !u.activo && 'text-muted-foreground')}>
                      <span className={cn('size-2 rounded-full', u.activo ? 'bg-stock-ok' : 'bg-muted-foreground')} aria-hidden />
                      {u.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {nuevo > 0 && <UsuarioDialog key={nuevo} open onOpenChange={(o) => !o && setNuevo(0)} />}
    </>
  )
}
