import { useState, type FormEvent } from 'react'
import { api } from '../api/client'
import type { Rol, Usuario, UsuarioRequest } from '../api/types'
import { Badge, ErrorMsg, Field, Heading, Table } from '../components/ui'
import { useAction, useAsync } from '../hooks/useAsync'

const ROLES: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  ENCARGADO_DEPOSITO: 'Encargado de depósito',
  VENDEDOR: 'Vendedor',
}

export default function Usuarios() {
  const lista = useAsync(() => api.get<Usuario[]>('/usuarios'), 'usuarios')
  const [editando, setEditando] = useState<Usuario | null | undefined>(undefined)
  const estado = useAction()

  const alternarActivo = async (u: Usuario) => {
    const body: UsuarioRequest = { username: u.username, nombre: u.nombre, rol: u.rol, activo: !u.activo }
    if (await estado.run(() => api.put(`/usuarios/${u.id}`, body))) lista.reload()
  }

  return (
    <>
      <Heading title="Usuarios">Quién puede entrar al sistema y qué rol tiene.</Heading>

      {editando !== undefined && (
        <UsuarioForm
          key={editando?.id ?? 'nuevo'}
          usuario={editando}
          onClose={(guardado) => {
            setEditando(undefined)
            if (guardado) lista.reload()
          }}
        />
      )}
      {editando === undefined && (
        <div className="actions">
          <button type="button" className="button" onClick={() => setEditando(null)}>Nuevo usuario</button>
        </div>
      )}

      <ErrorMsg error={lista.error ?? estado.error} />
      <Table>
        <thead>
          <tr><th>Usuario</th><th>Nombre</th><th>Rol</th><th>Estado</th><th /></tr>
        </thead>
        <tbody>
          {lista.data?.map((u) => (
            <tr key={u.id}>
              <td>{u.username}</td>
              <td>{u.nombre}</td>
              <td>{ROLES[u.rol]}</td>
              <td>{u.activo ? <Badge clase="ok">Activo</Badge> : <Badge clase="pending">Inactivo</Badge>}</td>
              <td className="row-actions">
                <button type="button" className="button button--ghost" onClick={() => setEditando(u)}>Editar</button>
                <button type="button" className="button button--ghost" disabled={estado.pending} onClick={() => void alternarActivo(u)}>
                  {u.activo ? 'Desactivar' : 'Activar'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </>
  )
}

function UsuarioForm({ usuario, onClose }: { usuario: Usuario | null; onClose: (guardado: boolean) => void }) {
  const { run, error, pending } = useAction()
  const [f, setF] = useState({
    username: usuario?.username ?? '',
    nombre: usuario?.nombre ?? '',
    rol: usuario?.rol ?? ('VENDEDOR' as Rol),
    activo: usuario?.activo ?? true,
    password: '',
  })
  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF((s) => ({ ...s, [k]: v }))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const body: UsuarioRequest = {
      username: f.username.trim(),
      nombre: f.nombre.trim(),
      rol: f.rol,
      activo: f.activo,
      password: f.password || undefined,
    }
    const ok = await run(() => (usuario ? api.put(`/usuarios/${usuario.id}`, body) : api.post('/usuarios', body)))
    if (ok) onClose(true)
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>{usuario ? `Editar ${usuario.username}` : 'Nuevo usuario'}</h2>
      <div className="form__grid">
        <Field label="Usuario"><input value={f.username} onChange={(e) => set('username', e.target.value)} maxLength={50} required /></Field>
        <Field label="Nombre"><input value={f.nombre} onChange={(e) => set('nombre', e.target.value)} maxLength={100} required /></Field>
        <Field label="Rol">
          <select value={f.rol} onChange={(e) => set('rol', e.target.value as Rol)}>
            {(Object.keys(ROLES) as Rol[]).map((r) => <option key={r} value={r}>{ROLES[r]}</option>)}
          </select>
        </Field>
        <Field label={usuario ? 'Nueva contraseña (vacío = no cambiarla)' : 'Contraseña (mínimo 6 caracteres)'}>
          <input type="password" value={f.password} onChange={(e) => set('password', e.target.value)} minLength={6} maxLength={72} autoComplete="new-password" required={!usuario} />
        </Field>
        <label className="check"><input type="checkbox" checked={f.activo} onChange={(e) => set('activo', e.target.checked)} /> Usuario activo</label>
      </div>
      <ErrorMsg error={error} />
      <div className="actions">
        <button type="submit" className="button" disabled={pending}>{pending ? 'Guardando…' : 'Guardar'}</button>
        <button type="button" className="button button--ghost" onClick={() => onClose(false)}>Cancelar</button>
      </div>
    </form>
  )
}
