import { useState } from 'react'
import { CircleX, ShieldCheck, UserRound } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { registrar } from '@/api/auth'
import { listarSucursales } from '@/api/catalogo'
import type { Rol } from '@/api/types'
import { Aviso } from '@/components/common'
import { Campo } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { useApi } from '@/hooks/useApi'
import { useAuth } from '@/hooks/auth-context'
import { ROL_LABEL } from '@/lib/format'
import { AuthLayout } from './AuthLayout'

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const ROLES: { rol: Rol; icono: typeof UserRound; ayuda: string }[] = [
  { rol: 'ADMINISTRADOR', icono: ShieldCheck, ayuda: 'Ve y opera todas las sucursales, y gestiona el catálogo y los usuarios.' },
  { rol: 'EMPLEADO', icono: UserRound, ayuda: 'Opera el stock de su sucursal.' },
]

/**
 * Registro con acceso inmediato. Por ser un proyecto académico se elige el rol para
 * probar el sistema con los dos perfiles; por defecto, administrador.
 */
export function RegistroPage() {
  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const sucursales = useApi((signal) => listarSucursales(signal))
  const [rol, setRol] = useState<Rol>('ADMINISTRADOR')
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
  const esEmpleado = rol === 'EMPLEADO'

  const errores = {
    nombre: !nombre.trim() ? 'Ingresá tu nombre' : undefined,
    username: !username.trim() ? 'Elegí un usuario' : /\s/.test(username.trim()) ? 'Sin espacios' : undefined,
    email: email.trim() && !EMAIL.test(email.trim()) ? 'Email inválido' : undefined,
    password: password.length < 8 ? 'Mínimo 8 caracteres' : undefined,
    confirmacion: confirmacion !== password ? 'Las contraseñas no coinciden' : undefined,
    sucursal: esEmpleado && !sucursalId ? 'Elegí tu sucursal' : undefined,
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
          rol,
          sucursalId: esEmpleado ? Number(sucursalId) : null,
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
      descripcion="Elegí con qué rol querés probar el sistema."
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
        <div className="flex flex-col gap-2">
          <span id="reg-rol" className="text-sm font-medium">
            Rol
          </span>
          <ToggleGroup
            type="single"
            value={rol}
            onValueChange={(v) => v && setRol(v as Rol)}
            spacing={1}
            aria-labelledby="reg-rol"
            className="grid w-full grid-cols-2 rounded-lg bg-muted p-1"
          >
            {ROLES.map(({ rol: r, icono: Icono }) => (
              <ToggleGroupItem
                key={r}
                value={r}
                className="h-9 w-full rounded-md text-[13px] text-muted-foreground data-[state=on]:border data-[state=on]:bg-card data-[state=on]:text-foreground"
              >
                <Icono /> {ROL_LABEL[r]}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          <p className="text-[13px] text-muted-foreground">{ROLES.find((x) => x.rol === rol)?.ayuda}</p>
        </div>
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
        {esEmpleado && (
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
        )}
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
