import { useState } from 'react'
import { ChevronRight, CircleX, TriangleAlert } from 'lucide-react'
import { toast } from 'sonner'
import { listarStock } from '@/api/catalogo'
import { registrarMovimiento } from '@/api/movimientos'
import { listarProductos, proveedoresDeProducto } from '@/api/productos'
import type { TipoMovimiento } from '@/api/types'
import { ICONO_MOVIMIENTO } from '@/components/badges'
import { Aviso } from '@/components/common'
import { Campo, Modal } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { useSesion } from '@/hooks/auth-context'
import { useDataVersion } from '@/hooks/data-context'
import { useApi } from '@/hooks/useApi'
import { TIPO_LABEL, aNumero, esFraccionable, fmtNumero } from '@/lib/format'
import { delta, redondear, requiereMotivo, requiereProveedor } from '@/lib/stock'
import { cn } from '@/lib/utils'
import type { PresetMovimiento } from './registrar-context'

const TIPOS: TipoMovimiento[] = ['INGRESO_COMPRA', 'SALIDA_VENTA', 'AJUSTE', 'DEVOLUCION_CLIENTE', 'DEVOLUCION_PROVEEDOR', 'ROTURA']

const COLOR: Record<TipoMovimiento, string> = {
  INGRESO_COMPRA: 'text-mov-ingreso',
  SALIDA_VENTA: 'text-mov-salida',
  AJUSTE: 'text-mov-ajuste',
  DEVOLUCION_CLIENTE: 'text-mov-devolucion',
  DEVOLUCION_PROVEEDOR: 'text-mov-devolucion',
  ROTURA: 'text-mov-rotura',
}

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  preset: PresetMovimiento
}

export function MovimientoDialog({ open, onOpenChange, preset }: Props) {
  const { sucursal } = useSesion()
  const { invalidate } = useDataVersion()
  const stock = useApi((signal) => listarStock(sucursal.id, signal), [sucursal.id])
  const productos = useApi((signal) => listarProductos(signal))

  const [productoId, setProductoId] = useState(preset.productoId ? String(preset.productoId) : '')
  const [tipo, setTipo] = useState<TipoMovimiento>(preset.tipo ?? 'SALIDA_VENTA')
  const [cantidad, setCantidad] = useState(preset.cantidad ? String(preset.cantidad) : '')
  const [motivo, setMotivo] = useState('')
  const [referencia, setReferencia] = useState('')
  const [proveedorElegido, setProveedorElegido] = useState(preset.proveedorId ? String(preset.proveedorId) : '')
  const [intentado, setIntentado] = useState(false)
  const [errorServidor, setErrorServidor] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  const fila = stock.data?.find((s) => String(s.productoId) === productoId)
  const producto = productos.data?.find((p) => String(p.id) === productoId)
  const proveedores = useApi(
    (signal) => (productoId ? proveedoresDeProducto(Number(productoId), signal) : Promise.resolve([])),
    [productoId],
  )
  // Por defecto, el proveedor preferido del producto
  const proveedorId = proveedorElegido || String(proveedores.data?.find((p) => p.preferido)?.proveedorId ?? '')

  const n = aNumero(cantidad)
  const conProveedor = requiereProveedor(tipo)
  const conMotivo = requiereMotivo(tipo)

  // Mismas reglas que valida el backend
  const errores = {
    producto: !fila ? 'Elegí un producto' : undefined,
    cantidad:
      n == null
        ? 'Ingresá una cantidad'
        : tipo !== 'AJUSTE' && n <= 0
          ? 'La cantidad debe ser mayor a cero'
          : tipo === 'AJUSTE' && n === 0
            ? 'El ajuste no puede ser 0'
            : producto && !esFraccionable(producto.unidadMedida) && !Number.isInteger(n)
              ? 'Este producto se maneja por unidad: la cantidad debe ser entera'
              : undefined,
    motivo: conMotivo && !motivo.trim() ? 'Indicá el motivo' : undefined,
    proveedor: conProveedor && !proveedorId ? 'Elegí el proveedor' : undefined,
  }
  const resultante = fila && n != null && !errores.cantidad ? redondear(fila.cantidad + delta(tipo, n)) : undefined
  const stockNegativo = resultante != null && resultante < 0
  const cantidadError = stockNegativo ? `Stock insuficiente: hay ${fmtNumero(fila!.cantidad)} disponibles` : errores.cantidad

  async function enviar() {
    setIntentado(true)
    setErrorServidor(undefined)
    if (Object.values(errores).some(Boolean) || stockNegativo || !fila || n == null) return
    setEnviando(true)
    try {
      await registrarMovimiento({
        productoId: fila.productoId,
        sucursalId: sucursal.id,
        tipo,
        cantidad: n,
        motivo: conMotivo ? motivo.trim() : undefined,
        observaciones: !conMotivo && motivo.trim() ? motivo.trim() : undefined,
        referencia: referencia.trim() || undefined,
        proveedorId: conProveedor ? Number(proveedorId) : undefined,
      })
      toast.success('Movimiento registrado', {
        description: `${TIPO_LABEL[tipo]} de ${fmtNumero(Math.abs(n))} · ${fila.codigo} ${fila.nombre}`,
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
      descripcion={`${sucursal.nombre} · Todo cambio de stock queda registrado en el historial.`}
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
        <Select
          value={productoId}
          onValueChange={(v) => {
            setProductoId(v)
            setProveedorElegido('')
          }}
          disabled={!stock.data}
        >
          <SelectTrigger id="mov-producto" className="w-full" aria-invalid={intentado && !!errores.producto}>
            <SelectValue placeholder={stock.data ? 'Seleccionar producto' : 'Cargando productos…'} />
          </SelectTrigger>
          <SelectContent position="popper" className="max-h-72">
            {stock.data?.map((s) => (
              <SelectItem key={s.productoId} value={String(s.productoId)}>
                <span className="font-mono text-[13px]">{s.codigo}</span> · {s.nombre}
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
          className="grid w-full grid-cols-3 rounded-lg bg-muted p-1"
        >
          {TIPOS.map((t) => {
            const Icon = ICONO_MOVIMIENTO[t]
            return (
              <ToggleGroupItem
                key={t}
                value={t}
                className="h-9 w-full rounded-md text-[13px] text-muted-foreground data-[state=on]:border data-[state=on]:bg-card data-[state=on]:text-foreground"
              >
                <Icon className={cn(tipo === t ? COLOR[t] : 'text-muted-foreground')} />
                {TIPO_LABEL[t]}
              </ToggleGroupItem>
            )
          })}
        </ToggleGroup>
      </div>

      <div className="grid grid-cols-2 items-start gap-4">
        <Campo
          label={producto ? `Cantidad (${producto.unidadMedida.toLowerCase()})` : 'Cantidad'}
          htmlFor="mov-cantidad"
          error={intentado || stockNegativo ? cantidadError : undefined}
          ayuda={tipo === 'AJUSTE' ? 'Usá un número negativo para restar.' : undefined}
        >
          <Input
            id="mov-cantidad"
            type="number"
            step={producto && esFraccionable(producto.unidadMedida) ? '0.001' : '1'}
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            aria-invalid={(intentado && !!errores.cantidad) || stockNegativo}
          />
        </Campo>
        <Campo label="Referencia" htmlFor="mov-ref" ayuda="Remito, factura o ticket">
          <Input id="mov-ref" value={referencia} onChange={(e) => setReferencia(e.target.value)} placeholder="REM-0001-00000123" />
        </Campo>
      </div>

      {conProveedor && (
        <Campo label="Proveedor" htmlFor="mov-proveedor" error={intentado ? errores.proveedor : undefined}>
          <Select value={proveedorId} onValueChange={setProveedorElegido} disabled={!productoId}>
            <SelectTrigger id="mov-proveedor" className="w-full" aria-invalid={intentado && !!errores.proveedor}>
              <SelectValue placeholder={productoId ? 'Elegí el proveedor' : 'Primero elegí el producto'} />
            </SelectTrigger>
            <SelectContent position="popper">
              {proveedores.data?.map((p) => (
                <SelectItem key={p.proveedorId} value={String(p.proveedorId)}>
                  {p.razonSocial}
                  {p.preferido ? ' (preferido)' : ''}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Campo>
      )}

      <Campo label={conMotivo ? 'Motivo' : 'Observaciones'} htmlFor="mov-motivo" error={intentado ? errores.motivo : undefined}>
        <Input
          id="mov-motivo"
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
          placeholder={conMotivo ? 'Ej.: recuento, mercadería fallada' : 'Opcional'}
          aria-invalid={intentado && !!errores.motivo}
        />
      </Campo>

      {fila && (
        <div className={cn('flex flex-col gap-3 rounded-lg p-4', stockNegativo ? 'bg-stock-out-bg' : 'bg-muted')}>
          <div className="flex items-center gap-6">
            <div>
              <p className="text-[13px] font-medium text-muted-foreground">Stock actual</p>
              <p className="tabular text-3xl font-semibold">{fmtNumero(fila.cantidad)}</p>
            </div>
            <ChevronRight className="size-5 text-muted-foreground" aria-hidden />
            <div>
              <p className="text-[13px] font-medium text-muted-foreground">Stock resultante</p>
              <p
                className={cn(
                  'tabular text-3xl font-semibold',
                  resultante == null
                    ? 'text-muted-foreground'
                    : stockNegativo || resultante <= fila.stockMinimo
                      ? 'text-stock-out'
                      : resultante <= fila.puntoPedido
                        ? 'text-stock-low'
                        : 'text-foreground',
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
            resultante <= fila.puntoPedido && (
              <p className="flex items-center gap-2 text-[13px] text-stock-low">
                <TriangleAlert className="size-4" /> Quedará en o por debajo del punto de pedido ({fmtNumero(fila.puntoPedido)}) y
                aparecerá en "A reponer".
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
