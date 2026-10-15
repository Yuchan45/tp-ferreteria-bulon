import { useState, type FormEvent } from 'react'
import { useAuth } from '../session'
import { ErrorMsg, Field } from '../components/ui'
import { useAction } from '../hooks/useAsync'

export default function Login() {
  const { signIn } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const { run, error, pending } = useAction()

  const submit = (e: FormEvent) => {
    e.preventDefault()
    void run(() => signIn(username.trim(), password))
  }

  return (
    <main className="container main">
      <form className="card form login" onSubmit={submit}>
        <h1>Iniciá sesión</h1>
        <p className="muted">Ingresá con tu usuario del sistema de stock.</p>
        <Field label="Usuario">
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" autoFocus required />
        </Field>
        <Field label="Contraseña">
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        </Field>
        <ErrorMsg error={error} />
        <button type="submit" className="button" disabled={pending}>
          {pending ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </main>
  )
}
