import { useState } from 'react'
import { CircleX } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError } from '@/api/client'
import type { Rol } from '@/api/types'
import { crearUsuario } from '@/api/usuarios'
import { Aviso } from '@/components/common'
import { Campo, Modal } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useDataVersion } from '@/hooks/data-context'
import { ROL_LABEL } from '@/lib/format'

export function UsuarioDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const { invalidate } = useDataVersion()
  const [username, setUsername] = useState('')
  const [nombre, setNombre] = useState('')
  const [rol, setRol] = useState<Rol>('VENDEDOR')
  const [activo, setActivo] = useState(true)
  const [intentado, setIntentado] = useState(false)
  const [errorServidor, setErrorServidor] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  const errores = {
    username: !username.trim() ? 'El usuario es obligatorio' : /\s/.test(username.trim()) ? 'Sin espacios' : undefined,
    nombre: !nombre.trim() ? 'El nombre es obligatorio' : undefined,
  }
  const err = (c: keyof typeof errores) => (intentado ? errores[c] : undefined)

  async function enviar() {
    setIntentado(true)
    setErrorServidor(undefined)
    if (errores.username || errores.nombre) return
    setEnviando(true)
    try {
      const u = await crearUsuario({ username: username.trim(), nombre: nombre.trim(), rol, activo })
      toast.success('Usuario creado', { description: `${u.nombre} · ${ROL_LABEL[u.rol]}` })
      invalidate()
      onOpenChange(false)
    } catch (e) {
      // Un username repetido viola la restricción única y el backend responde 500 sin detalle
      setErrorServidor(e instanceof ApiError && e.status === 500 ? 'No se pudo guardar. Verificá que el usuario no exista.' : (e as Error).message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      titulo="Nuevo usuario"
      className="sm:max-w-130"
      onSubmit={enviar}
      acciones={
        <>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" disabled={enviando}>
            {enviando ? 'Creando…' : 'Crear usuario'}
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label="Usuario" htmlFor="usr-username" error={err('username')}>
          <Input id="usr-username" className="font-mono" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="deposito2" aria-invalid={!!err('username')} />
        </Campo>
        <Campo label="Nombre" htmlFor="usr-nombre" error={err('nombre')}>
          <Input id="usr-nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Encargado Sucursal" aria-invalid={!!err('nombre')} />
        </Campo>
      </div>
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
      <div className="flex items-center gap-2">
        <Checkbox id="usr-activo" checked={activo} onCheckedChange={(c) => setActivo(c === true)} />
        <Label htmlFor="usr-activo">Usuario activo</Label>
      </div>
      {errorServidor && (
        <Aviso icon={CircleX} tono="error" titulo="No se pudo guardar">
          {errorServidor}
        </Aviso>
      )}
    </Modal>
  )
}
