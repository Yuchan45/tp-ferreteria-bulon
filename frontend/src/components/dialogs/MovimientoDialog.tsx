import { useState } from 'react'
import { ArrowDown, ArrowUp, ChevronRight, CircleX, Pencil, TriangleAlert } from 'lucide-react'
import { toast } from 'sonner'
import { registrarMovimiento } from '@/api/movimientos'
import { listarProductos } from '@/api/productos'
import type { TipoMovimiento } from '@/api/types'
import { Aviso } from '@/components/common'
import { Campo, Modal } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { useDataVersion } from '@/hooks/data-context'
import { useApi } from '@/hooks/useApi'
import { TIPO_LABEL, aNumero, fmtNumero } from '@/lib/format'
import { delta } from '@/lib/stock'
import { cn } from '@/lib/utils'
import type { PresetMovimiento } from './registrar-context'

const TIPOS: { tipo: TipoMovimiento; icon: typeof ArrowDown; color: string }[] = [
  { tipo: 'ENTRADA', icon: ArrowDown, color: 'text-mov-ingreso' },
  { tipo: 'SALIDA', icon: ArrowUp, color: 'text-mov-salida' },
  { tipo: 'AJUSTE', icon: Pencil, color: 'text-mov-ajuste' },
]

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  preset: PresetMovimiento
}

export function MovimientoDialog({ open, onOpenChange, preset }: Props) {
  const { invalidate } = useDataVersion()
  const productos = useApi((signal) => listarProductos(signal))

  const [productoId, setProductoId] = useState(preset.productoId ? String(preset.productoId) : '')
  const [tipo, setTipo] = useState<TipoMovimiento>(preset.tipo ?? 'SALIDA')
  const [cantidad, setCantidad] = useState(preset.cantidad ? String(preset.cantidad) : '')
  const [observacion, setObservacion] = useState('')
  const [intentado, setIntentado] = useState(false)
  const [errorServidor, setErrorServidor] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  const producto = productos.data?.find((p) => String(p.id) === productoId)
  const n = aNumero(cantidad)

  // Validaciones (las mismas reglas que aplica el backend, más la observación obligatoria en ajustes)
  const errores = {
    producto: !producto ? 'Elegí un producto' : undefined,
    cantidad:
      n == null || !Number.isInteger(n)
        ? 'Ingresá una cantidad entera'
        : tipo !== 'AJUSTE' && n <= 0
          ? 'La cantidad debe ser mayor a cero'
          : tipo === 'AJUSTE' && n === 0
            ? 'El ajuste no puede ser 0'
            : undefined,
    observacion: tipo === 'AJUSTE' && !observacion.trim() ? 'Indicá el motivo del ajuste' : undefined,
  }
  const resultante = producto && n != null && !errores.cantidad ? producto.stockActual + delta(tipo, n) : undefined
  const stockNegativo = resultante != null && resultante < 0
  const cantidadError = stockNegativo ? `Stock insuficiente: hay ${fmtNumero(producto!.stockActual)} unidades disponibles` : errores.cantidad

  async function enviar() {
    setIntentado(true)
    setErrorServidor(undefined)
    if (errores.producto || errores.cantidad || errores.observacion || stockNegativo || !producto || n == null) return
    setEnviando(true)
    try {
      await registrarMovimiento({ productoId: producto.id, tipo, cantidad: n, observacion: observacion.trim() || undefined })
      toast.success('Movimiento registrado', {
        description: `${TIPO_LABEL[tipo]} de ${fmtNumero(Math.abs(n))} u. · ${producto.codigo} ${producto.descripcion}`,
      })
      invalidate()
      onOpenChange(false)
    } catch (e) {
      setErrorServidor((e as Error).message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      titulo="Registrar movimiento"
      descripcion="Todo cambio de stock queda registrado en el historial."
      onSubmit={enviar}
      acciones={
        <>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" disabled={enviando || stockNegativo}>
            {enviando ? 'Registrando…' : 'Registrar movimiento'}
          </Button>
        </>
      }
    >
      <Campo label="Producto" htmlFor="mov-producto" error={intentado ? errores.producto : undefined}>
        <Select value={productoId} onValueChange={setProductoId} disabled={productos.loading}>
          <SelectTrigger id="mov-producto" className="w-full" aria-invalid={intentado && !!errores.producto}>
            <SelectValue placeholder={productos.loading ? 'Cargando productos…' : 'Seleccionar producto'} />
          </SelectTrigger>
          <SelectContent position="popper" className="max-h-72">
            {productos.data?.map((p) => (
              <SelectItem key={p.id} value={String(p.id)}>
                <span className="font-mono text-[13px]">{p.codigo}</span> · {p.descripcion}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Campo>

      <div className="flex flex-col gap-1.5">
        <span className="text-[13px] font-medium">Tipo de movimiento</span>
        <ToggleGroup
          type="single"
          value={tipo}
          onValueChange={(v) => v && setTipo(v as TipoMovimiento)}
          spacing={1}
          className="w-full rounded-lg bg-muted p-1"
        >
          {TIPOS.map(({ tipo: t, icon: Icon, color }) => (
            <ToggleGroupItem
              key={t}
              value={t}
              className="h-9 flex-1 rounded-md text-muted-foreground data-[state=on]:border data-[state=on]:bg-card data-[state=on]:text-foreground"
            >
              <Icon className={cn(tipo === t ? color : 'text-muted-foreground')} />
              {TIPO_LABEL[t]}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>

      <div className="grid grid-cols-2 items-start gap-4">
        <Campo
          label="Cantidad"
          htmlFor="mov-cantidad"
          error={intentado || stockNegativo ? cantidadError : undefined}
          ayuda={tipo === 'AJUSTE' ? 'Usá un número negativo para restar.' : undefined}
        >
          <Input
            id="mov-cantidad"
            type="number"
            inputMode="numeric"
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            aria-invalid={(intentado && !!errores.cantidad) || stockNegativo}
          />
        </Campo>
        <Campo label={tipo === 'AJUSTE' ? 'Motivo' : 'Observación'} htmlFor="mov-obs" error={intentado ? errores.observacion : undefined}>
          <Input
            id="mov-obs"
            value={observacion}
            onChange={(e) => setObservacion(e.target.value)}
            placeholder={tipo === 'AJUSTE' ? 'Ej.: recuento, rotura' : 'Ej.: venta mostrador'}
            aria-invalid={intentado && !!errores.observacion}
          />
        </Campo>
      </div>

      {producto && (
        <div className={cn('flex flex-col gap-3 rounded-lg p-4', stockNegativo ? 'bg-stock-out-bg' : 'bg-muted')}>
          <div className="flex items-center gap-6">
            <div>
              <p className="text-[13px] font-medium text-muted-foreground">Stock actual</p>
              <p className="tabular text-3xl font-semibold">{fmtNumero(producto.stockActual)}</p>
            </div>
            <ChevronRight className="size-5 text-muted-foreground" aria-hidden />
            <div>
              <p className="text-[13px] font-medium text-muted-foreground">Stock resultante</p>
              <p
                className={cn(
                  'tabular text-3xl font-semibold',
                  resultante == null ? 'text-muted-foreground' : stockNegativo ? 'text-stock-out' : resultante <= producto.stockMinimo ? 'text-stock-low' : 'text-foreground',
                )}
              >
                {resultante == null ? '—' : resultante < 0 ? `−${fmtNumero(-resultante)}` : fmtNumero(resultante)}
              </p>
            </div>
          </div>
          {stockNegativo ? (
            <p className="flex items-center gap-2 text-[13px] text-stock-out">
              <CircleX className="size-4" /> No se puede registrar: el stock no puede quedar negativo.
            </p>
          ) : (
            resultante != null &&
            resultante <= producto.stockMinimo && (
              <p className="flex items-center gap-2 text-[13px] text-stock-low">
                <TriangleAlert className="size-4" /> Quedará en o por debajo del mínimo ({fmtNumero(producto.stockMinimo)}) y aparecerá en "A reponer".
              </p>
            )
          )}
        </div>
      )}

      {errorServidor && (
        <Aviso icon={CircleX} tono="error" titulo="No se pudo registrar">
          {errorServidor}
        </Aviso>
      )}
    </Modal>
  )
}
