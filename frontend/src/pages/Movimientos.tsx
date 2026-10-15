import { useState, type FormEvent } from 'react'
import { api, download } from '../api/client'
import type { Movimiento, MovimientoRequest, Producto, TipoMovimiento, Ubicacion, Usuario } from '../api/types'
import { useSession } from '../session'
import { Badge, ErrorMsg, Field, Heading, Pager, ProductPicker, Table } from '../components/ui'
import { TIPOS, fecha } from '../labels'
import { useAction, useAsync, usePaged } from '../hooks/useAsync'

const VENDEDOR_PUEDE: TipoMovimiento[] = ['SALIDA_VENTA', 'DEVOLUCION']
const TODOS = Object.keys(TIPOS) as TipoMovimiento[]

export default function Movimientos() {
  const ubicaciones = useAsync(() => api.get<Ubicacion[]>('/ubicaciones'), 'ubicaciones')
  const [version, setVersion] = useState(0)

  return (
    <>
      <Heading title="Movimientos">Registrá entradas, salidas y traslados. El historial no se puede modificar.</Heading>
      <RegistroForm ubicaciones={ubicaciones.data ?? []} onRegistrado={() => setVersion((v) => v + 1)} />
      <Historial ubicaciones={ubicaciones.data ?? []} version={version} />
    </>
  )
}

function RegistroForm({ ubicaciones, onRegistrado }: { ubicaciones: Ubicacion[]; onRegistrado: () => void }) {
  const { rol } = useSession()
  const tipos = rol === 'VENDEDOR' ? VENDEDOR_PUEDE : TODOS
  const { run, error, pending, clearError } = useAction()

  const [producto, setProducto] = useState<Producto | null>(null)
  const [tipo, setTipo] = useState<TipoMovimiento>(tipos[0])
  const [cantidad, setCantidad] = useState('')
  const [ubicacionId, setUbicacionId] = useState('')
  const [destinoId, setDestinoId] = useState('')
  const [motivo, setMotivo] = useState('')
  const [observacion, setObservacion] = useState('')
  const [ok, setOk] = useState<string | null>(null)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (!producto) return
    const body: MovimientoRequest = {
      productoId: producto.id,
      tipo,
      cantidad: Number(cantidad),
      ubicacionId: Number(ubicacionId),
      ubicacionDestinoId: tipo === 'TRASLADO' ? Number(destinoId) : undefined,
      motivo: motivo.trim() || undefined,
      observacion: observacion.trim() || undefined,
    }
    setOk(null)
    const exito = await run(() => api.post('/movimientos', body))
    if (exito) {
      setOk(`Movimiento registrado: ${TIPOS[tipo]} de ${cantidad} × ${producto.codigo}.`)
      setProducto(null)
      setCantidad('')
      setMotivo('')
      setObservacion('')
      onRegistrado()
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>Registrar movimiento</h2>
      <Field label="Producto" wide>
        <ProductPicker value={producto} onChange={setProducto} />
      </Field>
      {producto && (
        <p className="muted">
          Stock actual: {producto.existencias.map((x) => `${x.ubicacion} ${x.cantidad}`).join(' · ')} · Total {producto.stockTotal}
        </p>
      )}
      <div className="form__grid">
        <Field label="Tipo">
          <select
            value={tipo}
            onChange={(e) => {
              setTipo(e.target.value as TipoMovimiento)
              clearError()
            }}
          >
            {tipos.map((t) => <option key={t} value={t}>{TIPOS[t]}</option>)}
          </select>
        </Field>
        <Field label={tipo === 'AJUSTE' ? 'Cantidad (negativa para restar)' : 'Cantidad'}>
          <input
            type="number"
            step="1"
            min={tipo === 'AJUSTE' ? undefined : 1}
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            required
          />
        </Field>
        <Field label={tipo === 'TRASLADO' ? 'Origen' : 'Ubicación'}>
          <select value={ubicacionId} onChange={(e) => setUbicacionId(e.target.value)} required>
            <option value="" disabled>Elegí una ubicación</option>
            {ubicaciones.map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}
          </select>
        </Field>
        {tipo === 'TRASLADO' && (
          <Field label="Destino">
            <select value={destinoId} onChange={(e) => setDestinoId(e.target.value)} required>
              <option value="" disabled>Elegí el destino</option>
              {ubicaciones.filter((u) => String(u.id) !== ubicacionId).map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}
            </select>
          </Field>
        )}
        {/* El backend rechaza un AJUSTE sin motivo; no se bloquea acá para mostrar su mensaje. */}
        <Field label={tipo === 'AJUSTE' ? 'Motivo (obligatorio)' : 'Motivo'}>
          <input value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={255} />
        </Field>
        <Field label="Observación">
          <input value={observacion} onChange={(e) => setObservacion(e.target.value)} maxLength={255} />
        </Field>
      </div>
      <ErrorMsg error={error} />
      {ok && <p className="msg msg--ok" role="status">{ok}</p>}
      <div className="actions">
        <button type="submit" className="button" disabled={pending || !producto}>{pending ? 'Registrando…' : 'Registrar'}</button>
      </div>
    </form>
  )
}

function Historial({ ubicaciones, version }: { ubicaciones: Ubicacion[]; version: number }) {
  const { esAdmin } = useSession()
  const [producto, setProducto] = useState<Producto | null>(null)
  const [tipo, setTipo] = useState('')
  const [ubicacionId, setUbicacionId] = useState('')
  const [usuarioId, setUsuarioId] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const exportar = useAction()

  // Solo el administrador puede leer /usuarios, así que solo él filtra por usuario.
  const usuarios = useAsync(() => (esAdmin ? api.get<Usuario[]>('/usuarios') : Promise.resolve([])), `usuarios${esAdmin}`)

  const filtros = { productoId: producto?.id, tipo, ubicacionId, usuarioId, desde, hasta }
  const lista = usePaged<Movimiento>('/movimientos', filtros, version)

  return (
    <section className="section">
      <h2>Historial</h2>
      <div className="toolbar">
        <div className="toolbar__picker"><ProductPicker value={producto} onChange={setProducto} /></div>
        <select value={tipo} onChange={(e) => setTipo(e.target.value)} aria-label="Tipo">
          <option value="">Todos los tipos</option>
          {TODOS.map((t) => <option key={t} value={t}>{TIPOS[t]}</option>)}
        </select>
        <select value={ubicacionId} onChange={(e) => setUbicacionId(e.target.value)} aria-label="Ubicación">
          <option value="">Todas las ubicaciones</option>
          {ubicaciones.map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}
        </select>
        {esAdmin && (
          <select value={usuarioId} onChange={(e) => setUsuarioId(e.target.value)} aria-label="Usuario">
            <option value="">Todos los usuarios</option>
            {usuarios.data?.map((u) => <option key={u.id} value={u.id}>{u.username}</option>)}
          </select>
        )}
        <label className="check">Desde <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} /></label>
        <label className="check">Hasta <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} /></label>
      </div>
      <div className="actions">
        {(['PDF', 'EXCEL'] as const).map((formato) => (
          <button
            key={formato}
            type="button"
            className="button button--ghost"
            disabled={exportar.pending}
            onClick={() => void exportar.run(() => download('/reportes/movimientos', { ...filtros, formato }))}
          >
            Exportar {formato === 'PDF' ? 'PDF' : 'Excel'}
          </button>
        ))}
      </div>
      <ErrorMsg error={exportar.error ?? lista.error} />

      <Table empty={!lista.loading && lista.rows.length === 0 ? 'No hay movimientos con esos filtros.' : undefined}>
        <thead>
          <tr>
            <th>Fecha</th><th>Tipo</th><th>Producto</th><th className="num">Cantidad</th>
            <th>Ubicación</th><th>Usuario</th><th>Motivo</th><th>Observación</th>
          </tr>
        </thead>
        <tbody>
          {lista.rows.map((m) => (
            <tr key={m.id}>
              <td>{fecha(m.fecha)}</td>
              <td><Badge clase="pending">{TIPOS[m.tipo]}</Badge></td>
              <td>{m.productoCodigo} · {m.productoNombre}</td>
              <td className="num">{m.cantidad}</td>
              <td>{m.ubicacionDestino ? `${m.ubicacion} → ${m.ubicacionDestino}` : m.ubicacion}</td>
              <td>{m.usuario}</td>
              <td>{m.motivo}</td>
              <td>{m.observacion}</td>
            </tr>
          ))}
        </tbody>
      </Table>
      <Pager info={lista.info} onPage={lista.setPage} />
    </section>
  )
}
