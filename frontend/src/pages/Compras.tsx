import { useState, type FormEvent } from 'react'
import { api } from '../api/client'
import type { EstadoOrdenCompra, OrdenCompra, OrdenCompraRequest, Producto, Proveedor, Sugerencia, Ubicacion } from '../api/types'
import { useSession } from '../session'
import { Badge, ErrorMsg, EstadoBadge, Field, Heading, Pager, ProductPicker, Table } from '../components/ui'
import { ESTADO_ORDEN, fecha } from '../labels'
import { useAction, useAsync, usePaged } from '../hooks/useAsync'

export default function Compras() {
  const { puedeEscribir } = useSession()
  const [estado, setEstado] = useState('')
  const [version, setVersion] = useState(0)
  const proveedores = useAsync(() => api.get<Proveedor[]>('/proveedores'), 'proveedores')
  const sugerencias = useAsync(() => api.get<Sugerencia[]>('/ordenes-compra/sugerencias'), `sugerencias${version}`)
  const ubicaciones = useAsync(() => api.get<Ubicacion[]>('/ubicaciones'), 'ubicaciones')
  const ordenes = usePaged<OrdenCompra>('/ordenes-compra', { estado }, version)
  const alCrear = () => setVersion((v) => v + 1)

  return (
    <>
      <Heading title="Órdenes de compra">Pedidos a proveedores para reponer el stock.</Heading>

      {puedeEscribir && (
        <>
          <section className="section">
            <h2>Sugeridas por reposición</h2>
            <p className="muted">Agrupadas por proveedor preferido. Ajustá las cantidades y generá la orden.</p>
            <ErrorMsg error={sugerencias.error} />
            {sugerencias.data?.length === 0 && <p className="muted">No hay productos pendientes de reposición.</p>}
            <div className="grid-2">
              {sugerencias.data?.map((g) => (
                <Grupo key={g.proveedorId ?? 'sin'} grupo={g} proveedores={proveedores.data ?? []} onCreada={alCrear} />
              ))}
            </div>
          </section>
          <OrdenManual proveedores={proveedores.data ?? []} onCreada={alCrear} />
        </>
      )}

      <section className="section">
        <h2>Órdenes</h2>
        <div className="toolbar">
          <select value={estado} onChange={(e) => setEstado(e.target.value)} aria-label="Estado">
            <option value="">Todos los estados</option>
            {(Object.keys(ESTADO_ORDEN) as EstadoOrdenCompra[]).map((e) => <option key={e} value={e}>{ESTADO_ORDEN[e].texto}</option>)}
          </select>
        </div>
        <ErrorMsg error={ordenes.error} />
        <Table empty={!ordenes.loading && ordenes.rows.length === 0 ? 'No hay órdenes de compra.' : undefined}>
          <thead>
            <tr><th>N°</th><th>Fecha</th><th>Proveedor</th><th>Estado</th><th>Ítems</th><th>Usuario</th>{puedeEscribir && <th />}</tr>
          </thead>
          <tbody>
            {ordenes.rows.map((o) => (
              <FilaOrden key={o.id} orden={o} ubicaciones={ubicaciones.data ?? []} puedeEscribir={puedeEscribir} onCambio={alCrear} />
            ))}
          </tbody>
        </Table>
        <Pager info={ordenes.info} onPage={ordenes.setPage} />
      </section>
    </>
  )
}

function Grupo({ grupo, proveedores, onCreada }: { grupo: Sugerencia; proveedores: Proveedor[]; onCreada: () => void }) {
  const { run, error, pending } = useAction()
  const [cantidades, setCantidades] = useState<Record<number, string>>(
    Object.fromEntries(grupo.items.map((i) => [i.productoId, String(i.cantidadSugerida)])),
  )
  // Los productos sin proveedor preferido necesitan que el usuario elija a quién comprarles.
  const [proveedorId, setProveedorId] = useState(grupo.proveedorId ? String(grupo.proveedorId) : '')

  const crear = async () => {
    const body: OrdenCompraRequest = {
      proveedorId: Number(proveedorId),
      items: grupo.items.map((i) => ({ productoId: i.productoId, cantidad: Number(cantidades[i.productoId]) })),
    }
    if (await run(() => api.post('/ordenes-compra', body))) onCreada()
  }

  return (
    <div className="card section">
      <h3>{grupo.proveedor ?? 'Sin proveedor preferido'}</h3>
      {!grupo.proveedorId && (
        <Field label="Comprar a">
          <select value={proveedorId} onChange={(e) => setProveedorId(e.target.value)}>
            <option value="" disabled>Elegí un proveedor</option>
            {proveedores.map((p) => <option key={p.id} value={p.id}>{p.razonSocial}</option>)}
          </select>
        </Field>
      )}
      <ul className="list">
        {grupo.items.map((i) => (
          <li key={i.productoId} className="line">
            <span>
              <EstadoBadge estado={i.estado} /> {i.productoCodigo} · {i.productoNombre} <small className="muted">(stock {i.stockTotal} · punto de pedido {i.puntoPedido})</small>
            </span>
            <input
              type="number"
              min="1"
              step="1"
              className="qty"
              aria-label={`Cantidad de ${i.productoCodigo}`}
              value={cantidades[i.productoId]}
              onChange={(e) => setCantidades((c) => ({ ...c, [i.productoId]: e.target.value }))}
            />
          </li>
        ))}
      </ul>
      <ErrorMsg error={error} />
      <button type="button" className="button" disabled={pending || !proveedorId} onClick={() => void crear()}>
        {pending ? 'Creando…' : 'Crear orden de compra'}
      </button>
    </div>
  )
}

function OrdenManual({ proveedores, onCreada }: { proveedores: Proveedor[]; onCreada: () => void }) {
  const [abierto, setAbierto] = useState(false)
  const { run, error, pending } = useAction()
  const [proveedorId, setProveedorId] = useState('')
  const [observacion, setObservacion] = useState('')
  const [items, setItems] = useState<{ producto: Producto; cantidad: string }[]>([])
  const [candidato, setCandidato] = useState<Producto | null>(null)

  if (!abierto) {
    return (
      <div className="actions">
        <button type="button" className="button button--ghost" onClick={() => setAbierto(true)}>Crear orden manual</button>
      </div>
    )
  }

  const agregar = (p: Producto | null) => {
    if (p && !items.some((i) => i.producto.id === p.id)) setItems((s) => [...s, { producto: p, cantidad: '1' }])
    setCandidato(null)
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const body: OrdenCompraRequest = {
      proveedorId: Number(proveedorId),
      observacion: observacion.trim() || undefined,
      items: items.map((i) => ({ productoId: i.producto.id, cantidad: Number(i.cantidad) })),
    }
    if (await run(() => api.post('/ordenes-compra', body))) {
      setItems([])
      setObservacion('')
      setAbierto(false)
      onCreada()
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>Orden manual</h2>
      <div className="form__grid">
        <Field label="Proveedor">
          <select value={proveedorId} onChange={(e) => setProveedorId(e.target.value)} required>
            <option value="" disabled>Elegí un proveedor</option>
            {proveedores.map((p) => <option key={p.id} value={p.id}>{p.razonSocial}</option>)}
          </select>
        </Field>
        <Field label="Observación"><input value={observacion} onChange={(e) => setObservacion(e.target.value)} maxLength={255} /></Field>
      </div>
      <Field label="Agregar producto" wide>
        <ProductPicker value={candidato} onChange={agregar} />
      </Field>
      <ul className="list">
        {items.map((i) => (
          <li key={i.producto.id} className="line">
            <span>{i.producto.codigo} · {i.producto.nombre}</span>
            <span className="inline">
              <input
                type="number"
                min="1"
                step="1"
                className="qty"
                aria-label={`Cantidad de ${i.producto.codigo}`}
                value={i.cantidad}
                onChange={(e) => setItems((s) => s.map((x) => (x === i ? { ...x, cantidad: e.target.value } : x)))}
              />
              <button type="button" className="button button--ghost" onClick={() => setItems((s) => s.filter((x) => x !== i))}>Quitar</button>
            </span>
          </li>
        ))}
      </ul>
      <ErrorMsg error={error} />
      <div className="actions">
        <button type="submit" className="button" disabled={pending || items.length === 0}>{pending ? 'Creando…' : 'Crear orden'}</button>
        <button type="button" className="button button--ghost" onClick={() => setAbierto(false)}>Cancelar</button>
      </div>
    </form>
  )
}

function FilaOrden({ orden, ubicaciones, puedeEscribir, onCambio }: { orden: OrdenCompra; ubicaciones: Ubicacion[]; puedeEscribir: boolean; onCambio: () => void }) {
  const { run, error, pending } = useAction()
  const [recibiendo, setRecibiendo] = useState(false)
  const [ubicacionId, setUbicacionId] = useState('')
  const { texto, clase } = ESTADO_ORDEN[orden.estado]

  const accion = async (path: string, body?: unknown) => {
    if (await run(() => api.post(`/ordenes-compra/${orden.id}/${path}`, body))) {
      setRecibiendo(false)
      onCambio()
    }
  }
  const abierta = orden.estado === 'BORRADOR' || orden.estado === 'ENVIADA'

  return (
    <tr>
      <td>{orden.id}</td>
      <td>
        {fecha(orden.fechaCreacion)}
        {orden.fechaRecepcion && <small className="muted"> · recibida {fecha(orden.fechaRecepcion)} en {orden.ubicacionRecepcion}</small>}
      </td>
      <td>{orden.proveedor}</td>
      <td><Badge clase={clase}>{texto}</Badge></td>
      <td>
        <ul className="plain">
          {orden.items.map((i) => <li key={i.productoId}>{i.cantidad} × {i.productoCodigo} {i.productoNombre}</li>)}
        </ul>
      </td>
      <td>{orden.usuario}</td>
      {puedeEscribir && (
        <td className="row-actions">
          {orden.estado === 'BORRADOR' && <button type="button" className="button" disabled={pending} onClick={() => void accion('enviar')}>Enviar</button>}
          {orden.estado === 'ENVIADA' && !recibiendo && <button type="button" className="button" onClick={() => setRecibiendo(true)}>Recibir</button>}
          {recibiendo && (
            <span className="inline">
              <select value={ubicacionId} onChange={(e) => setUbicacionId(e.target.value)} aria-label="Ubicación de recepción">
                <option value="" disabled>¿Dónde se recibe?</option>
                {ubicaciones.map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}
              </select>
              <button type="button" className="button" disabled={pending || !ubicacionId} onClick={() => void accion('recibir', { ubicacionId: Number(ubicacionId) })}>Confirmar</button>
              <button type="button" className="button button--ghost" onClick={() => setRecibiendo(false)}>Volver</button>
            </span>
          )}
          {abierta && !recibiendo && (
            <button type="button" className="button button--danger" disabled={pending} onClick={() => confirm(`¿Cancelar la orden ${orden.id}?`) && void accion('cancelar')}>Cancelar</button>
          )}
          <ErrorMsg error={error} />
        </td>
      )}
    </tr>
  )
}
