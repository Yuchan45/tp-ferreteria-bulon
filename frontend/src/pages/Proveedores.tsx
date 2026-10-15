import { useState, type FormEvent } from 'react'
import { api } from '../api/client'
import type { Proveedor, ProveedorRequest } from '../api/types'
import { useSession } from '../session'
import { ErrorMsg, Field, Heading, Table } from '../components/ui'
import { useAction, useAsync } from '../hooks/useAsync'

export default function Proveedores() {
  const { puedeEscribir } = useSession()
  const lista = useAsync(() => api.get<Proveedor[]>('/proveedores'), 'proveedores')
  const [editando, setEditando] = useState<Proveedor | null | undefined>(undefined)
  const borrar = useAction()

  const eliminar = async (p: Proveedor) => {
    if (!confirm(`¿Eliminar a ${p.razonSocial}?`)) return
    if (await borrar.run(() => api.delete(`/proveedores/${p.id}`))) lista.reload()
  }

  return (
    <>
      <Heading title="Proveedores">Empresas a las que se les compra mercadería.</Heading>

      {editando !== undefined && (
        <ProveedorForm
          key={editando?.id ?? 'nuevo'}
          proveedor={editando}
          onClose={(guardado) => {
            setEditando(undefined)
            if (guardado) lista.reload()
          }}
        />
      )}
      {puedeEscribir && editando === undefined && (
        <div className="actions">
          <button type="button" className="button" onClick={() => setEditando(null)}>Nuevo proveedor</button>
        </div>
      )}

      <ErrorMsg error={lista.error ?? borrar.error} />
      <Table empty={!lista.loading && lista.data?.length === 0 ? 'Todavía no hay proveedores.' : undefined}>
        <thead>
          <tr><th>Razón social</th><th>CUIT</th><th>Teléfono</th><th>Email</th>{puedeEscribir && <th />}</tr>
        </thead>
        <tbody>
          {lista.data?.map((p) => (
            <tr key={p.id}>
              <td>{p.razonSocial}</td>
              <td>{p.cuit}</td>
              <td>{p.telefono}</td>
              <td>{p.email}</td>
              {puedeEscribir && (
                <td className="row-actions">
                  <button type="button" className="button button--ghost" onClick={() => setEditando(p)}>Editar</button>
                  <button type="button" className="button button--danger" disabled={borrar.pending} onClick={() => void eliminar(p)}>Eliminar</button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </Table>
    </>
  )
}

function ProveedorForm({ proveedor, onClose }: { proveedor: Proveedor | null; onClose: (guardado: boolean) => void }) {
  const { run, error, pending } = useAction()
  const [f, setF] = useState({
    razonSocial: proveedor?.razonSocial ?? '',
    cuit: proveedor?.cuit ?? '',
    telefono: proveedor?.telefono ?? '',
    email: proveedor?.email ?? '',
  })
  const set = (k: keyof typeof f, v: string) => setF((s) => ({ ...s, [k]: v }))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const body: ProveedorRequest = {
      razonSocial: f.razonSocial.trim(),
      cuit: f.cuit.trim() || null,
      telefono: f.telefono.trim() || null,
      email: f.email.trim() || null,
    }
    const ok = await run(() => (proveedor ? api.put(`/proveedores/${proveedor.id}`, body) : api.post('/proveedores', body)))
    if (ok) onClose(true)
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>{proveedor ? 'Editar proveedor' : 'Nuevo proveedor'}</h2>
      <div className="form__grid">
        <Field label="Razón social"><input value={f.razonSocial} onChange={(e) => set('razonSocial', e.target.value)} maxLength={150} required /></Field>
        <Field label="CUIT (30-12345678-9)"><input value={f.cuit} onChange={(e) => set('cuit', e.target.value)} placeholder="30-12345678-9" /></Field>
        <Field label="Teléfono"><input value={f.telefono} onChange={(e) => set('telefono', e.target.value)} /></Field>
        <Field label="Email"><input type="email" value={f.email} onChange={(e) => set('email', e.target.value)} /></Field>
      </div>
      <ErrorMsg error={error} />
      <div className="actions">
        <button type="submit" className="button" disabled={pending}>{pending ? 'Guardando…' : 'Guardar'}</button>
        <button type="button" className="button button--ghost" onClick={() => onClose(false)}>Cancelar</button>
      </div>
    </form>
  )
}
