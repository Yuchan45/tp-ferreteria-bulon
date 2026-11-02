import { useState } from 'react'
import { ArrowLeftRight } from 'lucide-react'
import { Link } from 'react-router'
import type { TipoMovimiento } from '@/api/types'
import { FiltroTipoMovimiento, MovimientoBadge } from '@/components/badges'
import { ExportarReporte } from '@/components/ExportarReporte'
import { CeldaProducto, ErrorCarga, EstadoVacio, POR_PAGINA, Paginacion, SearchField, TablaCargando, Toolbar } from '@/components/common'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useSesion } from '@/hooks/auth-context'
import { useMovimientosGlobales } from '@/hooks/useMovimientosGlobales'
import { diaLocal, fmtDelta, fmtFechaHora, fmtNumero, incluye } from '@/lib/format'

export function MovimientosPage() {
  const { sucursal } = useSesion()
  const movimientos = useMovimientosGlobales(sucursal.id)
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [tipo, setTipo] = useState<TipoMovimiento | null>(null)
  const [busqueda, setBusqueda] = useState('')
  const [pagina, setPagina] = useState(1)

  // Primero las fechas y la búsqueda; el tipo se aplica después para poder contar cada uno
  const enPeriodo = (movimientos.data ?? []).filter((m) => {
    const dia = diaLocal(m.fecha)
    return (
      (!desde || dia >= desde) &&
      (!hasta || dia <= hasta) &&
      (!busqueda || incluye(m.codigo, busqueda) || incluye(m.nombre, busqueda))
    )
  })
  const filtrados = enPeriodo.filter((m) => !tipo || m.tipo === tipo)
  const contar = (t: TipoMovimiento | null) => (t ? enPeriodo.filter((m) => m.tipo === t).length : enPeriodo.length)
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
        <SearchField value={busqueda} onChange={filtrar(setBusqueda)} placeholder="Buscar producto por código o nombre" className="max-w-80" />
        <div className="flex-1" />
        {movimientos.data && <span className="text-[13px] text-muted-foreground">{fmtNumero(filtrados.length)} movimientos en el período</span>}
      </Toolbar>
      <div className="flex items-start gap-3">
        <div className="flex-1">
          <FiltroTipoMovimiento value={tipo} onChange={filtrar(setTipo)} contar={contar} />
        </div>
        <ExportarReporte tipo="movimientos" filtros={{ sucursalId: sucursal.id, desde, hasta, tipo, q: busqueda }} />
      </div>

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
                  <TableHead className="text-muted-foreground">Detalle</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibles.map((m) => (
                  <TableRow key={m.id} className="h-13">
                    <TableCell className="pl-4 text-muted-foreground">{fmtFechaHora(m.fecha)}</TableCell>
                    <TableCell>
                      <Link to={`/productos/${m.productoId}`} className="hover:underline">
                        <CeldaProducto descripcion={m.nombre} codigo={m.codigo} peso="normal" />
                      </Link>
                    </TableCell>
                    <TableCell>
                      <MovimientoBadge tipo={m.tipo} />
                    </TableCell>
                    <TableCell className="tabular text-right font-medium">{fmtDelta(m.efecto)}</TableCell>
                    <TableCell className="tabular text-right">{fmtNumero(m.stockResultante)}</TableCell>
                    <TableCell className="max-w-60 truncate text-muted-foreground">{[m.motivo ?? m.observaciones, m.referencia].filter(Boolean).join(' · ') || '—'}</TableCell>
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
