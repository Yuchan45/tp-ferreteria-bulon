import { useState } from 'react'
import { ArrowLeftRight } from 'lucide-react'
import { Link } from 'react-router'
import type { TipoMovimiento } from '@/api/types'
import { MovimientoBadge } from '@/components/badges'
import { CeldaProducto, ErrorCarga, EstadoVacio, POR_PAGINA, Paginacion, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useMovimientosGlobales } from '@/hooks/useMovimientosGlobales'
import { TIPO_LABEL, diaLocal, fmtDelta, fmtFechaHora, fmtNumero, incluye } from '@/lib/format'
import { delta } from '@/lib/stock'

const TODOS = 'todos'

export function MovimientosPage() {
  const movimientos = useMovimientosGlobales()
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [tipo, setTipo] = useState<TipoMovimiento | typeof TODOS>(TODOS)
  const [busqueda, setBusqueda] = useState('')
  const [pagina, setPagina] = useState(1)

  const filtrados = (movimientos.data ?? []).filter((m) => {
    const dia = diaLocal(m.fecha)
    return (
      (!desde || dia >= desde) &&
      (!hasta || dia <= hasta) &&
      (tipo === TODOS || m.tipo === tipo) &&
      (!busqueda || incluye(m.producto.codigo, busqueda) || incluye(m.producto.descripcion, busqueda))
    )
  })
  const visibles = filtrados.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA)
  const filtrar = <T,>(set: (v: T) => void) => (v: T) => {
    set(v)
    setPagina(1)
  }

  return (
    <>
      <Toolbar>
        <div className="flex items-center gap-2">
          <Input type="date" aria-label="Desde" value={desde} max={hasta || undefined} onChange={(e) => filtrar(setDesde)(e.target.value)} className="w-40" />
          <span className="text-muted-foreground">–</span>
          <Input type="date" aria-label="Hasta" value={hasta} min={desde || undefined} onChange={(e) => filtrar(setHasta)(e.target.value)} className="w-40" />
        </div>
        <Select value={tipo} onValueChange={filtrar((v: string) => setTipo(v as TipoMovimiento))}>
          <SelectTrigger className="w-40" aria-label="Filtrar por tipo">
            <SelectValue />
          </SelectTrigger>
          <SelectContent position="popper">
            <SelectItem value={TODOS}>Tipo: Todos</SelectItem>
            {(Object.keys(TIPO_LABEL) as TipoMovimiento[]).map((t) => (
              <SelectItem key={t} value={t}>
                {TIPO_LABEL[t]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <SearchField value={busqueda} onChange={filtrar(setBusqueda)} placeholder="Buscar producto por código o nombre" className="max-w-80" />
        <div className="flex-1" />
        {movimientos.data && <span className="text-[13px] text-muted-foreground">{fmtNumero(filtrados.length)} movimientos en el período</span>}
      </Toolbar>

      {movimientos.error ? (
        <ErrorCarga error={movimientos.error} onRetry={movimientos.reload} />
      ) : !movimientos.data ? (
        <TablaCargando />
      ) : filtrados.length === 0 ? (
        <EstadoVacio icon={ArrowLeftRight} titulo="No hay movimientos" descripcion={movimientos.data.length ? 'Ningún movimiento coincide con los filtros.' : 'Todavía no se registró ningún movimiento.'} />
      ) : (
        <>
          <div className="overflow-hidden rounded-lg border bg-card">
            <Table>
              <TableHeader className="bg-muted">
                <TableRow className="hover:bg-transparent">
                  <TableHead className="pl-4 text-muted-foreground">Fecha</TableHead>
                  <TableHead className="text-muted-foreground">Producto</TableHead>
                  <TableHead className="text-muted-foreground">Tipo</TableHead>
                  <TableHead className="text-right text-muted-foreground">Cantidad</TableHead>
                  <TableHead className="text-right text-muted-foreground">Stock resultante</TableHead>
                  <TableHead className="text-muted-foreground">Observación</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibles.map((m) => (
                  <TableRow key={m.id} className="h-13">
                    <TableCell className="pl-4 text-muted-foreground">{fmtFechaHora(m.fecha)}</TableCell>
                    <TableCell>
                      <Link to={`/productos/${m.producto.id}`} className="hover:underline">
                        <CeldaProducto descripcion={m.producto.descripcion} codigo={m.producto.codigo} peso="normal" />
                      </Link>
                    </TableCell>
                    <TableCell>
                      <MovimientoBadge tipo={m.tipo} />
                    </TableCell>
                    <TableCell className="tabular text-right font-medium">{fmtDelta(delta(m.tipo, m.cantidad))}</TableCell>
                    <TableCell className="tabular text-right">{fmtNumero(m.stockResultante)}</TableCell>
                    <TableCell className="max-w-60 truncate text-muted-foreground">{m.observacion ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <Paginacion pagina={pagina} total={filtrados.length} onChange={setPagina} unidad="movimientos" />
        </>
      )}
    </>
  )
}
