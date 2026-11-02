import { useState } from 'react'
import { CircleX } from 'lucide-react'
import { toast } from 'sonner'
import { listarSucursales } from '@/api/catalogo'
import type { Rol, Usuario } from '@/api/types'
import { actualizarUsuario, crearUsuario } from '@/api/usuarios'
import { Aviso } from '@/components/common'
import { Campo, Modal } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useDataVersion } from '@/hooks/data-context'
import { useApi } from '@/hooks/useApi'
import { ROL_LABEL } from '@/lib/format'

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * Alta de usuario por un administrador, con su rol y sucursal. Si recibe `usuario`, lo
 * edita: el username no cambia y la contraseña solo se reemplaza si se escribe una nueva.
 */
export function UsuarioDialog({
  open,
  onOpenChange,
  usuario,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  usuario?: Usuario
}) {
  const { invalidate } = useDataVersion()
  const sucursales = useApi((signal) => listarSucursales(signal))
  const editando = usuario != null
  const [username, setUsername] = useState(usuario?.username ?? '')
  const [nombre, setNombre] = useState(usuario?.nombre ?? '')
  const [email, setEmail] = useState(usuario?.email ?? '')
  const [password, setPassword] = useState('')
  const [rol, setRol] = useState<Rol>(usuario?.rol ?? 'EMPLEADO')
  const [sucursalElegida, setSucursalElegida] = useState(usuario?.sucursalId != null ? String(usuario.sucursalId) : '')
  const [activo, setActivo] = useState(usuario?.activo ?? true)
  const [intentado, setIntentado] = useState(false)
  const [errorServidor, setErrorServidor] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  const sucursalId = sucursalElegida || (sucursales.data?.length === 1 ? String(sucursales.data[0].id) : '')
  const errores = {
    username: !username.trim() ? 'El usuario es obligatorio' : /\s/.test(username.trim()) ? 'Sin espacios' : undefined,
    nombre: !nombre.trim() ? 'El nombre es obligatorio' : undefined,
    email: email.trim() && !EMAIL.test(email.trim()) ? 'Email inválido' : undefined,
    // Al editar, vacía = se mantiene la contraseña actual
    password: (!editando || password) && password.length < 8 ? 'Mínimo 8 caracteres' : undefined,
    sucursal: rol === 'EMPLEADO' && !sucursalId ? 'Elegí la sucursal del empleado' : undefined,
  }
  const err = (c: keyof typeof errores) => (intentado ? errores[c] : undefined)

  async function enviar() {
    setIntentado(true)
    setErrorServidor(undefined)
    if (Object.values(errores).some(Boolean)) return
    setEnviando(true)
    try {
      const datos = {
        nombre: nombre.trim(),
        email: email.trim() || null,
        rol,
        // El administrador no se asigna a una sucursal: ve todas
        sucursalId: rol === 'EMPLEADO' ? Number(sucursalId) : null,
        activo,
      }
      const u = editando
        ? await actualizarUsuario(usuario.id, { ...datos, password: password || null })
        : await crearUsuario({ ...datos, username: username.trim(), password })
      toast.success(editando ? 'Usuario actualizado' : 'Usuario creado', { description: `${u.nombre} · ${ROL_LABEL[u.rol]}` })
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
      titulo={editando ? 'Editar usuario' : 'Nuevo usuario'}
      className="sm:max-w-130"
      onSubmit={enviar}
      acciones={
        <>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" disabled={enviando}>
            {enviando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear usuario'}
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label="Usuario" htmlFor="usr-username" error={err('username')} ayuda={editando ? 'El usuario no se puede cambiar' : undefined}>
          <Input id="usr-username" className="font-mono" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="encargado2" disabled={editando} aria-invalid={!!err('username')} />
        </Campo>
        <Campo label="Nombre" htmlFor="usr-nombre" error={err('nombre')}>
          <Input id="usr-nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Encargado Sucursal" aria-invalid={!!err('nombre')} />
        </Campo>
      </div>
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label="Email" htmlFor="usr-email" error={err('email')}>
          <Input id="usr-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Opcional" aria-invalid={!!err('email')} />
        </Campo>
        <Campo
          label={editando ? 'Nueva contraseña' : 'Contraseña inicial'}
          htmlFor="usr-password"
          error={err('password')}
          ayuda={editando ? 'Vacía para no cambiarla · mínimo 8 caracteres' : 'Mínimo 8 caracteres'}
        >
          <Input id="usr-password" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} aria-invalid={!!err('password')} />
        </Campo>
      </div>
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label="Rol" htmlFor="usr-rol">
          <Select value={rol} onValueChange={(v) => setRol(v as Rol)}>
            <SelectTrigger id="usr-rol" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper">
              {(Object.keys(ROL_LABEL) as Rol[]).map((r) => (
                <SelectItem key={r} value={r}>
                  {ROL_LABEL[r]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Campo>
        <Campo label="Sucursal" htmlFor="usr-sucursal" error={err('sucursal')} ayuda={rol === 'ADMINISTRADOR' ? 'El administrador ve todas' : undefined}>
          <Select value={rol === 'EMPLEADO' ? sucursalId : ''} onValueChange={setSucursalElegida} disabled={rol === 'ADMINISTRADOR'}>
            <SelectTrigger id="usr-sucursal" className="w-full" aria-invalid={!!err('sucursal')}>
              <SelectValue placeholder={rol === 'ADMINISTRADOR' ? 'Todas' : 'Elegí la sucursal'} />
            </SelectTrigger>
            <SelectContent position="popper">
              {sucursales.data?.map((s) => (
                <SelectItem key={s.id} value={String(s.id)}>
                  {s.nombre}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Campo>
      </div>
      <div className="flex flex-col gap-1">
        <div className="flex items-center gap-2">
          <Checkbox id="usr-activo" checked={activo} onCheckedChange={(c) => setActivo(c === true)} />
          <Label htmlFor="usr-activo">Usuario activo</Label>
        </div>
        {!activo && <p className="pl-6 text-[13px] text-muted-foreground">No podrá iniciar sesión ni recibirá notificaciones.</p>}
      </div>
      {errorServidor && (
        <Aviso icon={CircleX} tono="error" titulo="No se pudo guardar">
          {errorServidor}
        </Aviso>
      )}
    </Modal>
  )
}
