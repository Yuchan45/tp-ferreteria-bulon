import { useState } from 'react'
import { CircleX, Info } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError } from '@/api/client'
import { actualizarProducto, crearProducto } from '@/api/productos'
import { listarProveedores } from '@/api/proveedores'
import type { Producto } from '@/api/types'
import { Aviso } from '@/components/common'
import { Campo, Modal } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useDataVersion } from '@/hooks/data-context'
import { useApi } from '@/hooks/useApi'
import { aNumero } from '@/lib/format'

const SIN_PROVEEDOR = 'ninguno'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Si se pasa, el diálogo edita ese producto; si no, crea uno nuevo. */
  producto?: Producto
}

export function ProductoDialog({ open, onOpenChange, producto }: Props) {
  const { invalidate } = useDataVersion()
  const proveedores = useApi((signal) => listarProveedores(signal))

  const [codigo, setCodigo] = useState(producto?.codigo ?? '')
  const [descripcion, setDescripcion] = useState(producto?.descripcion ?? '')
  const [precio, setPrecio] = useState(producto?.precio != null ? String(producto.precio) : '')
  const [stockMinimo, setStockMinimo] = useState(producto ? String(producto.stockMinimo) : '')
  const [proveedorId, setProveedorId] = useState(producto?.proveedorId ? String(producto.proveedorId) : SIN_PROVEEDOR)
  const [intentado, setIntentado] = useState(false)
  const [errorCodigo, setErrorCodigo] = useState<string>()
  const [errorServidor, setErrorServidor] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  const precioN = aNumero(precio)
  const minimoN = aNumero(stockMinimo)
  const errores = {
    codigo: !codigo.trim() ? 'El código es obligatorio' : errorCodigo,
    descripcion: !descripcion.trim() ? 'La descripción es obligatoria' : undefined,
    precio: precio.trim() && (precioN == null || precioN < 0) ? 'Ingresá un precio válido' : undefined,
    stockMinimo: minimoN == null || !Number.isInteger(minimoN) || minimoN < 0 ? 'Ingresá un número entero ≥ 0' : undefined,
  }

  async function enviar() {
    setIntentado(true)
    setErrorServidor(undefined)
    if (!codigo.trim() || errores.descripcion || errores.precio || errores.stockMinimo) return
    const input = {
      codigo: codigo.trim(),
      descripcion: descripcion.trim(),
      precio: precioN,
      stockMinimo: minimoN!,
      proveedorId: proveedorId === SIN_PROVEEDOR ? null : Number(proveedorId),
    }
    setEnviando(true)
    try {
      const guardado = producto ? await actualizarProducto(producto.id, input) : await crearProducto(input)
      toast.success(producto ? 'Producto actualizado' : 'Producto creado', { description: `${guardado.codigo} · ${guardado.descripcion}` })
      invalidate()
      onOpenChange(false)
    } catch (e) {
      const msg = (e as Error).message
      // El backend responde 422 "Ya existe un producto con código X"
      if (e instanceof ApiError && e.status === 422 && /código/i.test(msg)) setErrorCodigo(msg)
      else setErrorServidor(msg)
    } finally {
      setEnviando(false)
    }
  }

  const err = (campo: keyof typeof errores) => (intentado || campo === 'codigo' ? errores[campo] : undefined)

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      titulo={producto ? 'Editar producto' : 'Nuevo producto'}
      descripcion={producto ? 'El stock no se edita acá: cambia solo con movimientos.' : 'Completá los datos del artículo. El stock arranca en 0.'}
      onSubmit={enviar}
      acciones={
        <>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar producto'}
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label="Código (SKU)" htmlFor="prod-codigo" error={intentado || errorCodigo ? err('codigo') : undefined}>
          <Input
            id="prod-codigo"
            className="font-mono"
            value={codigo}
            onChange={(e) => {
              setCodigo(e.target.value)
              setErrorCodigo(undefined)
            }}
            placeholder="HER-006"
            aria-invalid={!!((intentado || errorCodigo) && errores.codigo)}
          />
        </Campo>
        <Campo label="Precio" htmlFor="prod-precio" error={err('precio')}>
          <Input id="prod-precio" type="number" min={0} step="0.01" value={precio} onChange={(e) => setPrecio(e.target.value)} placeholder="9500" aria-invalid={!!err('precio')} />
        </Campo>
      </div>
      <Campo label="Descripción" htmlFor="prod-desc" error={err('descripcion')}>
        <Input id="prod-desc" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} placeholder="Nivel de burbuja aluminio 40 cm" aria-invalid={!!err('descripcion')} />
      </Campo>
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label="Stock mínimo" htmlFor="prod-min" error={err('stockMinimo')}>
          <Input id="prod-min" type="number" min={0} value={stockMinimo} onChange={(e) => setStockMinimo(e.target.value)} placeholder="8" aria-invalid={!!err('stockMinimo')} />
        </Campo>
        <Campo label="Proveedor" htmlFor="prod-prov">
          <Select value={proveedorId} onValueChange={setProveedorId}>
            <SelectTrigger id="prod-prov" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper">
              <SelectItem value={SIN_PROVEEDOR}>Sin proveedor</SelectItem>
              {proveedores.data?.map((p) => (
                <SelectItem key={p.id} value={String(p.id)}>
                  {p.razonSocial}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Campo>
      </div>
      {!producto && (
        <p className="flex items-center gap-2 text-[13px] text-muted-foreground">
          <Info className="size-4 text-primary" /> El stock inicial se carga después como un movimiento de ingreso.
        </p>
      )}
      {errorServidor && (
        <Aviso icon={CircleX} tono="error" titulo="No se pudo guardar">
          {errorServidor}
        </Aviso>
      )}
    </Modal>
  )
}
