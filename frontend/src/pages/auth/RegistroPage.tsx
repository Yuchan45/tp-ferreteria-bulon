import { useState } from 'react'
import { CircleX } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { registrar } from '@/api/auth'
import { listarSucursales } from '@/api/catalogo'
import { Aviso } from '@/components/common'
import { Campo } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useApi } from '@/hooks/useApi'
import { useAuth } from '@/hooks/auth-context'
import { AuthLayout } from './AuthLayout'

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Registro con acceso inmediato. El backend siempre crea un EMPLEADO de la sucursal elegida. */
export function RegistroPage() {
  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const sucursales = useApi((signal) => listarSucursales(signal))
  const [nombre, setNombre] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [sucursalElegida, setSucursalElegida] = useState('')
  const [intentado, setIntentado] = useState(false)
  const [error, setError] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  // Con una sola sucursal no hace falta elegir
  const sucursalId = sucursalElegida || (sucursales.data?.length === 1 ? String(sucursales.data[0].id) : '')

  const errores = {
    nombre: !nombre.trim() ? 'Ingresá tu nombre' : undefined,
    username: !username.trim() ? 'Elegí un usuario' : /\s/.test(username.trim()) ? 'Sin espacios' : undefined,
    email: email.trim() && !EMAIL.test(email.trim()) ? 'Email inválido' : undefined,
    password: password.length < 8 ? 'Mínimo 8 caracteres' : undefined,
    confirmacion: confirmacion !== password ? 'Las contraseñas no coinciden' : undefined,
    sucursal: !sucursalId ? 'Elegí tu sucursal' : undefined,
  }
  const err = (c: keyof typeof errores) => (intentado ? errores[c] : undefined)

  async function enviar(e: React.FormEvent) {
    e.preventDefault()
    setIntentado(true)
    setError(undefined)
    if (Object.values(errores).some(Boolean)) return
    setEnviando(true)
    try {
      iniciarSesion(
        await registrar({
          username: username.trim(),
          nombre: nombre.trim(),
          email: email.trim() || null,
          password,
          sucursalId: Number(sucursalId),
        }),
      )
      navigate('/', { replace: true })
    } catch (er) {
      setError((er as Error).message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <AuthLayout
      titulo="Crear cuenta"
      descripcion="Te registrás como empleado de una sucursal."
      pie={
        <>
          ¿Ya tenés cuenta?{' '}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Iniciá sesión
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={enviar} className="flex flex-col gap-4">
        <Campo label="Nombre y apellido" htmlFor="reg-nombre" error={err('nombre')}>
          <Input id="reg-nombre" autoComplete="name" autoFocus value={nombre} onChange={(e) => setNombre(e.target.value)} aria-invalid={!!err('nombre')} />
        </Campo>
        <div className="grid grid-cols-2 items-start gap-4">
          <Campo label="Usuario" htmlFor="reg-usuario" error={err('username')}>
            <Input id="reg-usuario" autoComplete="username" className="font-mono" value={username} onChange={(e) => setUsername(e.target.value)} aria-invalid={!!err('username')} />
          </Campo>
          <Campo label="Email (opcional)" htmlFor="reg-email" error={err('email')}>
            <Input id="reg-email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} aria-invalid={!!err('email')} />
          </Campo>
        </div>
        <div className="grid grid-cols-2 items-start gap-4">
          <Campo label="Contraseña" htmlFor="reg-password" error={err('password')} ayuda="Mínimo 8 caracteres">
            <Input id="reg-password" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} aria-invalid={!!err('password')} />
          </Campo>
          <Campo label="Repetir contraseña" htmlFor="reg-confirmacion" error={err('confirmacion')}>
            <Input id="reg-confirmacion" type="password" autoComplete="new-password" value={confirmacion} onChange={(e) => setConfirmacion(e.target.value)} aria-invalid={!!err('confirmacion')} />
          </Campo>
        </div>
        <Campo label="Sucursal" htmlFor="reg-sucursal" error={err('sucursal') ?? sucursales.error?.message}>
          <Select value={sucursalId} onValueChange={setSucursalElegida} disabled={!sucursales.data}>
            <SelectTrigger id="reg-sucursal" className="w-full" aria-invalid={!!err('sucursal')}>
              <SelectValue placeholder={sucursales.data ? 'Elegí tu sucursal' : 'Cargando…'} />
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
        {error && (
          <Aviso icon={CircleX} tono="error" titulo="No se pudo crear la cuenta">
            {error}
          </Aviso>
        )}
        <Button type="submit" disabled={enviando} className="w-full">
          {enviando ? 'Creando cuenta…' : 'Crear cuenta'}
        </Button>
      </form>
    </AuthLayout>
  )
}
