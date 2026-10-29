import { useState } from 'react'
import { CircleX, Info } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError } from '@/api/client'
import { listarCategorias } from '@/api/catalogo'
import { actualizarProducto, asignarProveedor, configurarUmbrales, crearProducto } from '@/api/productos'
import { listarProveedores } from '@/api/proveedores'
import type { Producto, ProductoProveedor, StockSucursal, UnidadMedida } from '@/api/types'
import { Aviso } from '@/components/common'
import { Campo, Modal } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useSesion } from '@/hooks/auth-context'
import { useDataVersion } from '@/hooks/data-context'
import { useApi } from '@/hooks/useApi'
import { UNIDAD_LABEL, aNumero } from '@/lib/format'

const NINGUNA = 'ninguna'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Si se pasa, el diálogo edita ese producto; si no, crea uno nuevo. */
  producto?: Producto
  /** Stock actual del producto en la sucursal (para precargar los umbrales). */
  stock?: StockSucursal
  /** Proveedor preferido actual (para precargarlo). */
  preferido?: ProductoProveedor
}

/** Alta / edición de producto: datos del catálogo, umbrales en la sucursal y proveedor preferido. */
export function ProductoDialog({ open, onOpenChange, producto, stock, preferido }: Props) {
  const { sucursal } = useSesion()
  const { invalidate } = useDataVersion()
  const proveedores = useApi((signal) => listarProveedores(signal))
  const categorias = useApi((signal) => listarCategorias(signal))

  const [codigo, setCodigo] = useState(producto?.codigo ?? '')
  const [nombre, setNombre] = useState(producto?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(producto?.descripcion ?? '')
  const [categoriaId, setCategoriaId] = useState(producto?.categoriaId ? String(producto.categoriaId) : NINGUNA)
  const [unidad, setUnidad] = useState<UnidadMedida>(producto?.unidadMedida ?? 'UNIDAD')
  const [precio, setPrecio] = useState(producto?.precio != null ? String(producto.precio) : '')
  const [stockMinimo, setStockMinimo] = useState(stock ? String(stock.stockMinimo) : '')
  const [puntoPedido, setPuntoPedido] = useState(stock ? String(stock.puntoPedido) : '')
  const [proveedorId, setProveedorId] = useState(preferido ? String(preferido.proveedorId) : NINGUNA)
  const [costo, setCosto] = useState(preferido?.precioCosto != null ? String(preferido.precioCosto) : '')
  const [intentado, setIntentado] = useState(false)
  const [errorCodigo, setErrorCodigo] = useState<string>()
  const [errorServidor, setErrorServidor] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  const precioN = aNumero(precio)
  const minimoN = aNumero(stockMinimo)
  const puntoN = aNumero(puntoPedido)
  const costoN = aNumero(costo)
  const errores = {
    codigo: !codigo.trim() ? 'El código es obligatorio' : errorCodigo,
    nombre: !nombre.trim() ? 'El nombre es obligatorio' : undefined,
    precio: precio.trim() && (precioN == null || precioN < 0) ? 'Ingresá un precio válido' : undefined,
    stockMinimo: minimoN == null || minimoN < 0 ? 'Ingresá un número ≥ 0' : undefined,
    puntoPedido:
      puntoN == null || puntoN < 0
        ? 'Ingresá un número ≥ 0'
        : minimoN != null && puntoN < minimoN
          ? 'No puede ser menor que el mínimo'
          : undefined,
    costo: costo.trim() && (costoN == null || costoN < 0) ? 'Ingresá un costo válido' : undefined,
  }
  const err = (c: keyof typeof errores) => (intentado || (c === 'codigo' && errorCodigo) ? errores[c] : undefined)

  async function enviar() {
    setIntentado(true)
    setErrorServidor(undefined)
    if (Object.values(errores).some(Boolean)) return
    const input = {
      codigo: codigo.trim(),
      nombre: nombre.trim(),
      descripcion: descripcion.trim() || null,
      categoriaId: categoriaId === NINGUNA ? null : Number(categoriaId),
      unidadMedida: unidad,
      precio: precioN,
    }
    setEnviando(true)
    try {
      const guardado = producto ? await actualizarProducto(producto.id, input) : await crearProducto(input)
      await configurarUmbrales(guardado.id, sucursal.id, minimoN!, puntoN!)
      if (proveedorId !== NINGUNA) {
        await asignarProveedor(guardado.id, Number(proveedorId), {
          preferido: true,
          codigoProveedor: preferido?.proveedorId === Number(proveedorId) ? preferido.codigoProveedor : null,
          precioCosto: costoN,
          plazoEntregaDias: preferido?.proveedorId === Number(proveedorId) ? preferido.plazoEntregaDias : null,
        })
      }
      toast.success(producto ? 'Producto actualizado' : 'Producto creado', { description: `${guardado.codigo} · ${guardado.nombre}` })
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
      <div className="grid grid-cols-[160px_1fr] items-start gap-4">
        <Campo label="Código (SKU)" htmlFor="prod-codigo" error={err('codigo')}>
          <Input
            id="prod-codigo"
            className="font-mono"
            value={codigo}
            onChange={(e) => {
              setCodigo(e.target.value)
              setErrorCodigo(undefined)
            }}
            placeholder="HER-006"
            aria-invalid={!!err('codigo')}
          />
        </Campo>
        <Campo label="Nombre" htmlFor="prod-nombre" error={err('nombre')}>
          <Input id="prod-nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nivel de burbuja 40 cm" aria-invalid={!!err('nombre')} />
        </Campo>
      </div>
      <Campo label="Descripción" htmlFor="prod-desc">
        <Input id="prod-desc" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} placeholder="Opcional" />
      </Campo>
      <div className="grid grid-cols-3 items-start gap-4">
        <Campo label="Categoría" htmlFor="prod-cat">
          <Select value={categoriaId} onValueChange={setCategoriaId}>
            <SelectTrigger id="prod-cat" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper">
              <SelectItem value={NINGUNA}>Sin categoría</SelectItem>
              {categorias.data?.map((c) => (
                <SelectItem key={c.id} value={String(c.id)}>
                  {c.nombre}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Campo>
        <Campo label="Unidad" htmlFor="prod-unidad">
          <Select value={unidad} onValueChange={(v) => setUnidad(v as UnidadMedida)}>
            <SelectTrigger id="prod-unidad" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper">
              {(Object.keys(UNIDAD_LABEL) as UnidadMedida[]).map((u) => (
                <SelectItem key={u} value={u}>
                  {UNIDAD_LABEL[u]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Campo>
        <Campo label="Precio de venta" htmlFor="prod-precio" error={err('precio')}>
          <Input id="prod-precio" type="number" min={0} step="0.01" value={precio} onChange={(e) => setPrecio(e.target.value)} aria-invalid={!!err('precio')} />
        </Campo>
      </div>
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label={`Stock mínimo · ${sucursal.nombre}`} htmlFor="prod-min" error={err('stockMinimo')} ayuda="Piso de seguridad">
          <Input id="prod-min" type="number" min={0} value={stockMinimo} onChange={(e) => setStockMinimo(e.target.value)} aria-invalid={!!err('stockMinimo')} />
        </Campo>
        <Campo label="Punto de pedido" htmlFor="prod-punto" error={err('puntoPedido')} ayuda="Al llegar acá, conviene pedir">
          <Input id="prod-punto" type="number" min={0} value={puntoPedido} onChange={(e) => setPuntoPedido(e.target.value)} aria-invalid={!!err('puntoPedido')} />
        </Campo>
      </div>
      <div className="grid grid-cols-[1fr_140px] items-start gap-4">
        <Campo label="Proveedor preferido" htmlFor="prod-prov">
          <Select value={proveedorId} onValueChange={setProveedorId}>
            <SelectTrigger id="prod-prov" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper">
              <SelectItem value={NINGUNA}>{preferido ? 'Sin cambios' : 'Sin proveedor'}</SelectItem>
              {proveedores.data?.map((p) => (
                <SelectItem key={p.id} value={String(p.id)}>
                  {p.razonSocial}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Campo>
        <Campo label="Costo" htmlFor="prod-costo" error={err('costo')}>
          <Input id="prod-costo" type="number" min={0} step="0.01" value={costo} onChange={(e) => setCosto(e.target.value)} disabled={proveedorId === NINGUNA} aria-invalid={!!err('costo')} />
        </Campo>
      </div>
      {!producto && (
        <p className="flex items-center gap-2 text-[13px] text-muted-foreground">
          <Info className="size-4 text-primary" /> El stock inicial se carga después como una compra al proveedor.
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
