import { useState, type FormEvent } from 'react'
import { api } from '../api/client'
import type { Categoria, Producto, ProductoRequest, Proveedor, Ubicacion, UnidadMedida } from '../api/types'
import { useSession } from '../session'
import { Badge, ErrorMsg, EstadoBadge, Field, Heading, Pager, Table } from '../components/ui'
import { useAction, useAsync, usePaged } from '../hooks/useAsync'

const UNIDADES: UnidadMedida[] = ['UNIDAD', 'METRO', 'KILOGRAMO', 'LITRO', 'CAJA', 'ROLLO', 'PAQUETE']

export default function Productos() {
  const { puedeEscribir } = useSession()
  const [q, setQ] = useState('')
  const [categoriaId, setCategoriaId] = useState('')
  const [activo, setActivo] = useState('')
  const [reposicion, setReposicion] = useState(false)
  // undefined = formulario cerrado; null = producto nuevo
  const [editando, setEditando] = useState<Producto | null | undefined>(undefined)

  const categorias = useAsync(() => api.get<Categoria[]>('/categorias'), 'categorias')
  const ubicaciones = useAsync(() => api.get<Ubicacion[]>('/ubicaciones'), 'ubicaciones')
  const lista = usePaged<Producto>('/productos', {
    q,
    categoriaId,
    activo,
    reposicion: reposicion || undefined,
  })

  return (
    <>
      <Heading title="Productos">Catálogo con el stock por ubicación y su estado de reposición.</Heading>

      {editando !== undefined && (
        <ProductoForm
          key={editando?.id ?? 'nuevo'}
          producto={editando}
          categorias={categorias.data ?? []}
          onCategoriaCreada={categorias.reload}
          onClose={(guardado) => {
            setEditando(undefined)
            if (guardado) lista.reload()
          }}
        />
      )}

      <div className="toolbar">
        <input type="search" placeholder="Buscar por código o nombre" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)} aria-label="Categoría">
          <option value="">Todas las categorías</option>
          {categorias.data?.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>
        <select value={activo} onChange={(e) => setActivo(e.target.value)} aria-label="Estado">
          <option value="">Activos e inactivos</option>
          <option value="true">Solo activos</option>
          <option value="false">Solo inactivos</option>
        </select>
        <label className="check">
          <input type="checkbox" checked={reposicion} onChange={(e) => setReposicion(e.target.checked)} />
          Pendientes de reposición
        </label>
        {puedeEscribir && editando === undefined && (
          <button type="button" className="button toolbar__end" onClick={() => setEditando(null)}>
            Nuevo producto
          </button>
        )}
      </div>

      <ErrorMsg error={lista.error} />
      <Table empty={!lista.loading && lista.rows.length === 0 ? 'No hay productos que coincidan.' : undefined}>
        <thead>
          <tr>
            <th>Código</th>
            <th>Producto</th>
            <th>Categoría</th>
            {ubicaciones.data?.map((u) => <th key={u.id} className="num">{u.nombre}</th>)}
            <th className="num">Total</th>
            <th className="num">Mínimo</th>
            <th className="num">Pto. pedido</th>
            <th>Estado</th>
            {puedeEscribir && <th />}
          </tr>
        </thead>
        <tbody>
          {lista.rows.map((p) => (
            <tr key={p.id} className={p.activo ? undefined : 'row--inactive'}>
              <td>{p.codigo}</td>
              <td>
                {p.nombre} {!p.activo && <Badge clase="pending">Inactivo</Badge>}
              </td>
              <td>{p.categoria}</td>
              {ubicaciones.data?.map((u) => (
                <td key={u.id} className="num">{p.existencias.find((e) => e.ubicacionId === u.id)?.cantidad ?? 0}</td>
              ))}
              <td className="num"><strong>{p.stockTotal}</strong></td>
              <td className="num">{p.stockMinimo}</td>
              <td className="num">{p.puntoPedido}</td>
              <td><EstadoBadge estado={p.estadoStock} /></td>
              {puedeEscribir && (
                <td>
                  <button type="button" className="button button--ghost" onClick={() => setEditando(p)}>Editar</button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </Table>
      <Pager info={lista.info} onPage={lista.setPage} />
    </>
  )
}

function ProductoForm({
  producto,
  categorias,
  onCategoriaCreada,
  onClose,
}: {
  producto: Producto | null
  categorias: Categoria[]
  onCategoriaCreada: () => void
  onClose: (guardado: boolean) => void
}) {
  const proveedores = useAsync(() => api.get<Proveedor[]>('/proveedores'), 'proveedores')
  const { run, error, pending } = useAction()
  const nuevaCategoria = useAction()
  const [nombreCategoria, setNombreCategoria] = useState<string | null>(null)

  const [f, setF] = useState({
    codigo: producto?.codigo ?? '',
    nombre: producto?.nombre ?? '',
    descripcion: producto?.descripcion ?? '',
    categoriaId: producto ? String(producto.categoriaId) : '',
    unidadMedida: producto?.unidadMedida ?? ('UNIDAD' as UnidadMedida),
    precio: producto?.precio != null ? String(producto.precio) : '',
    stockMinimo: String(producto?.stockMinimo ?? 0),
    puntoPedido: String(producto?.puntoPedido ?? 0),
    activo: producto?.activo ?? true,
    proveedorIds: producto?.proveedorIds ?? [],
    proveedorPreferidoId: producto?.proveedorPreferidoId ? String(producto.proveedorPreferidoId) : '',
  })
  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF((s) => ({ ...s, [k]: v }))

  const toggleProveedor = (id: number) => {
    const ids = f.proveedorIds.includes(id) ? f.proveedorIds.filter((x) => x !== id) : [...f.proveedorIds, id]
    setF((s) => ({
      ...s,
      proveedorIds: ids,
      proveedorPreferidoId: ids.includes(Number(s.proveedorPreferidoId)) ? s.proveedorPreferidoId : '',
    }))
  }

  const crearCategoria = async () => {
    if (!nombreCategoria?.trim()) return
    const ok = await nuevaCategoria.run(async () => {
      const c = await api.post<Categoria>('/categorias', { nombre: nombreCategoria.trim() })
      onCategoriaCreada()
      set('categoriaId', String(c.id))
    })
    if (ok) setNombreCategoria(null)
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const body: ProductoRequest = {
      codigo: f.codigo.trim(),
      nombre: f.nombre.trim(),
      descripcion: f.descripcion.trim() || undefined,
      categoriaId: Number(f.categoriaId),
      unidadMedida: f.unidadMedida,
      precio: f.precio === '' ? undefined : Number(f.precio),
      stockMinimo: Number(f.stockMinimo),
      puntoPedido: Number(f.puntoPedido),
      activo: f.activo,
      proveedorIds: f.proveedorIds,
      proveedorPreferidoId: f.proveedorPreferidoId ? Number(f.proveedorPreferidoId) : undefined,
    }
    const ok = await run(() => (producto ? api.put(`/productos/${producto.id}`, body) : api.post('/productos', body)))
    if (ok) onClose(true)
  }

  const seleccionados = proveedores.data?.filter((p) => f.proveedorIds.includes(p.id)) ?? []

  return (
    <form className="card form" onSubmit={submit}>
      <h2>{producto ? `Editar ${producto.codigo}` : 'Nuevo producto'}</h2>
      <div className="form__grid">
        <Field label="Código"><input value={f.codigo} onChange={(e) => set('codigo', e.target.value)} maxLength={50} required /></Field>
        <Field label="Nombre"><input value={f.nombre} onChange={(e) => set('nombre', e.target.value)} maxLength={120} required /></Field>
        <Field label="Descripción" wide><input value={f.descripcion} onChange={(e) => set('descripcion', e.target.value)} maxLength={200} /></Field>

        <Field label="Categoría">
          <select value={f.categoriaId} onChange={(e) => set('categoriaId', e.target.value)} required>
            <option value="" disabled>Elegí una categoría</option>
            {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
          {nombreCategoria === null ? (
            <button type="button" className="link" onClick={() => setNombreCategoria('')}>+ Crear categoría</button>
          ) : (
            <span className="inline">
              <input placeholder="Nombre de la categoría" value={nombreCategoria} onChange={(e) => setNombreCategoria(e.target.value)} />
              <button type="button" className="button" disabled={nuevaCategoria.pending} onClick={crearCategoria}>Crear</button>
              <button type="button" className="button button--ghost" onClick={() => setNombreCategoria(null)}>Cancelar</button>
            </span>
          )}
          <ErrorMsg error={nuevaCategoria.error} />
        </Field>
        <Field label="Unidad de medida">
          <select value={f.unidadMedida} onChange={(e) => set('unidadMedida', e.target.value as UnidadMedida)}>
            {UNIDADES.map((u) => <option key={u} value={u}>{u.charAt(0) + u.slice(1).toLowerCase()}</option>)}
          </select>
        </Field>
        <Field label="Precio"><input type="number" min="0" step="0.01" value={f.precio} onChange={(e) => set('precio', e.target.value)} /></Field>
        <Field label="Stock mínimo"><input type="number" min="0" step="1" value={f.stockMinimo} onChange={(e) => set('stockMinimo', e.target.value)} required /></Field>
        <Field label="Punto de pedido (no menor al mínimo)"><input type="number" min="0" step="1" value={f.puntoPedido} onChange={(e) => set('puntoPedido', e.target.value)} required /></Field>
        <label className="check"><input type="checkbox" checked={f.activo} onChange={(e) => set('activo', e.target.checked)} /> Producto activo</label>
      </div>

      <fieldset className="fieldset">
        <legend>Proveedores</legend>
        <div className="checks">
          {proveedores.data?.map((p) => (
            <label key={p.id} className="check">
              <input type="checkbox" checked={f.proveedorIds.includes(p.id)} onChange={() => toggleProveedor(p.id)} />
              {p.razonSocial}
            </label>
          ))}
        </div>
        <Field label="Proveedor preferido (se usa para sugerir órdenes de compra)">
          <select value={f.proveedorPreferidoId} onChange={(e) => set('proveedorPreferidoId', e.target.value)}>
            <option value="">Sin preferido</option>
            {seleccionados.map((p) => <option key={p.id} value={p.id}>{p.razonSocial}</option>)}
          </select>
        </Field>
      </fieldset>

      <ErrorMsg error={error} />
      <div className="actions">
        <button type="submit" className="button" disabled={pending}>{pending ? 'Guardando…' : 'Guardar'}</button>
        <button type="button" className="button button--ghost" onClick={() => onClose(false)}>Cancelar</button>
      </div>
    </form>
  )
}
