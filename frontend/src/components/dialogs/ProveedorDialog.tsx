import { useState } from 'react'
import { CircleX, Info } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError } from '@/api/client'
import { actualizarProveedor, crearProveedor } from '@/api/proveedores'
import type { Proveedor } from '@/api/types'
import { Aviso } from '@/components/common'
import { Campo, Modal } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDataVersion } from '@/hooks/data-context'

const CUIT = /^\d{2}-\d{8}-\d$/
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Alta de proveedor o, si recibe `proveedor`, edición de sus datos y su estado. */
export function ProveedorDialog({
  open,
  onOpenChange,
  proveedor,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  proveedor?: Proveedor
}) {
  const { invalidate } = useDataVersion()
  const editando = proveedor != null
  const [razonSocial, setRazonSocial] = useState(proveedor?.razonSocial ?? '')
  const [cuit, setCuit] = useState(proveedor?.cuit ?? '')
  const [telefono, setTelefono] = useState(proveedor?.telefono ?? '')
  const [email, setEmail] = useState(proveedor?.email ?? '')
  const [contacto, setContacto] = useState(proveedor?.contacto ?? '')
  const [activo, setActivo] = useState(proveedor?.activo ?? true)
  const [intentado, setIntentado] = useState(false)
  const [errorServidor, setErrorServidor] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  const errores = {
    razonSocial: !razonSocial.trim() ? 'La razón social es obligatoria' : undefined,
    cuit: cuit.trim() && !CUIT.test(cuit.trim()) ? 'Formato: 30-12345678-9' : undefined,
    email: email.trim() && !EMAIL.test(email.trim()) ? 'Email inválido' : undefined,
  }
  const err = (c: keyof typeof errores) => (intentado ? errores[c] : undefined)

  async function enviar() {
    setIntentado(true)
    setErrorServidor(undefined)
    if (errores.razonSocial || errores.cuit || errores.email) return
    setEnviando(true)
    try {
      const datos = {
        razonSocial: razonSocial.trim(),
        cuit: cuit.trim() || null,
        telefono: telefono.trim() || null,
        email: email.trim() || null,
        contacto: contacto.trim() || null,
      }
      const p = editando ? await actualizarProveedor(proveedor.id, { ...datos, activo }) : await crearProveedor(datos)
      toast.success(editando ? 'Proveedor actualizado' : 'Proveedor creado', { description: p.razonSocial })
      invalidate()
      onOpenChange(false)
    } catch (e) {
      // Un CUIT repetido viola el índice único: el backend responde 409
      setErrorServidor(e instanceof ApiError && e.status === 409 ? 'No se pudo guardar. Verificá que el CUIT no esté cargado en otro proveedor.' : (e as Error).message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      titulo={editando ? 'Editar proveedor' : 'Nuevo proveedor'}
      className="sm:max-w-130"
      onSubmit={enviar}
      acciones={
        <>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" disabled={enviando}>
            {enviando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Guardar proveedor'}
          </Button>
        </>
      }
    >
      <Campo label="Razón social" htmlFor="prov-rs" error={err('razonSocial')}>
        <Input id="prov-rs" value={razonSocial} onChange={(e) => setRazonSocial(e.target.value)} placeholder="Ferretería Mayorista Oeste S.A." aria-invalid={!!err('razonSocial')} />
      </Campo>
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label="CUIT" htmlFor="prov-cuit" error={err('cuit')}>
          <Input id="prov-cuit" className="font-mono" value={cuit} onChange={(e) => setCuit(e.target.value)} placeholder="30-71000006-6" aria-invalid={!!err('cuit')} />
        </Campo>
        <Campo label="Teléfono" htmlFor="prov-tel">
          <Input id="prov-tel" value={telefono} onChange={(e) => setTelefono(e.target.value)} placeholder="011 4655-6600" />
        </Campo>
      </div>
      <div className="grid grid-cols-2 items-start gap-4">
        <Campo label="Email" htmlFor="prov-mail" error={err('email')}>
          <Input id="prov-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="ventas@proveedor.com.ar" aria-invalid={!!err('email')} />
        </Campo>
        <Campo label="Contacto" htmlFor="prov-contacto">
          <Input id="prov-contacto" value={contacto} onChange={(e) => setContacto(e.target.value)} placeholder="Nombre de la persona" />
        </Campo>
      </div>
      <p className="flex items-center gap-2 text-[13px] text-muted-foreground">
        <Info className="size-4 text-primary" /> El CUIT es opcional, pero si se carga debe ser único.
      </p>
      {editando && (
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <Checkbox id="prov-activo" checked={activo} onCheckedChange={(c) => setActivo(c === true)} />
            <Label htmlFor="prov-activo">Proveedor activo</Label>
          </div>
          {!activo && (
            <p className="pl-6 text-[13px] text-muted-foreground">
              No se le podrán registrar compras. Sus compras y devoluciones anteriores quedan en el historial.
            </p>
          )}
        </div>
      )}
      {errorServidor && (
        <Aviso icon={CircleX} tono="error" titulo="No se pudo guardar">
          {errorServidor}
        </Aviso>
      )}
    </Modal>
  )
}
