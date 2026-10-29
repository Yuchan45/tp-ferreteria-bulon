import { useState } from 'react'
import { CircleX } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router'
import { login } from '@/api/auth'
import { Aviso } from '@/components/common'
import { Campo } from '@/components/forms'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/hooks/auth-context'
import { AuthLayout } from './AuthLayout'

export function LoginPage() {
  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const desde = (useLocation().state as { desde?: string } | null)?.desde ?? '/'
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  async function enviar(e: React.FormEvent) {
    e.preventDefault()
    if (!username.trim() || !password) {
      setError('Ingresá tu usuario y contraseña.')
      return
    }
    setEnviando(true)
    setError(undefined)
    try {
      iniciarSesion(await login(username.trim(), password))
      navigate(desde, { replace: true })
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <AuthLayout
      titulo="Iniciar sesión"
      descripcion="Ingresá con tu usuario de Bulón."
      pie={
        <>
          ¿No tenés cuenta?{' '}
          <Link to="/registro" className="font-medium text-primary hover:underline">
            Registrate
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={enviar} className="flex flex-col gap-4">
        <Campo label="Usuario" htmlFor="login-usuario">
          <Input id="login-usuario" autoComplete="username" autoFocus value={username} onChange={(e) => setUsername(e.target.value)} />
        </Campo>
        <Campo label="Contraseña" htmlFor="login-password">
          <Input id="login-password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </Campo>
        {error && (
          <Aviso icon={CircleX} tono="error" titulo="No se pudo iniciar sesión">
            {error}
          </Aviso>
        )}
        <Button type="submit" disabled={enviando} className="w-full">
          {enviando ? 'Ingresando…' : 'Ingresar'}
        </Button>
      </form>
    </AuthLayout>
  )
}
