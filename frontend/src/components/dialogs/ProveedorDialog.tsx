import { useState } from 'react'
import { CircleX, Info } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError } from '@/api/client'
import { crearProveedor } from '@/api/proveedores'
import { Aviso } from '@/components/common'
import { Campo, Modal } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useDataVersion } from '@/hooks/data-context'

const CUIT = /^\d{2}-\d{8}-\d$/
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function ProveedorDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const { invalidate } = useDataVersion()
  const [razonSocial, setRazonSocial] = useState('')
  const [cuit, setCuit] = useState('')
  const [telefono, setTelefono] = useState('')
  const [email, setEmail] = useState('')
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
      const p = await crearProveedor({
        razonSocial: razonSocial.trim(),
        cuit: cuit.trim() || null,
        telefono: telefono.trim() || null,
        email: email.trim() || null,
      })
      toast.success('Proveedor creado', { description: p.razonSocial })
      invalidate()
      onOpenChange(false)
    } catch (e) {
      // Un CUIT repetido viola el índice único y el backend responde 500 sin detalle
      setErrorServidor(e instanceof ApiError && e.status === 500 ? 'No se pudo guardar. Verificá que el CUIT no esté cargado en otro proveedor.' : (e as Error).message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      titulo="Nuevo proveedor"
      className="sm:max-w-130"
      onSubmit={enviar}
      acciones={
        <>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar proveedor'}
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
      <Campo label="Email" htmlFor="prov-mail" error={err('email')}>
        <Input id="prov-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="ventas@proveedor.com.ar" aria-invalid={!!err('email')} />
      </Campo>
      <p className="flex items-center gap-2 text-[13px] text-muted-foreground">
        <Info className="size-4 text-primary" /> El CUIT es opcional, pero si se carga debe ser único.
      </p>
      {errorServidor && (
        <Aviso icon={CircleX} tono="error" titulo="No se pudo guardar">
          {errorServidor}
        </Aviso>
      )}
    </Modal>
  )
}
