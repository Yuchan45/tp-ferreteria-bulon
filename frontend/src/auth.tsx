import { useEffect, useState, type ReactNode } from 'react'
import * as client from './api/client'
import type { Usuario } from './api/types'
import { AuthContext } from './session'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Usuario | null>(null)
  const [ready, setReady] = useState(!client.hasCredentials())

  useEffect(() => {
    client.setOnUnauthorized(() => setUser(null))
    if (!client.hasCredentials()) return
    client
      .me()
      .then(setUser, () => client.logout())
      .finally(() => setReady(true))
  }, [])

  const signIn = async (username: string, password: string) => setUser(await client.login(username, password))
  const signOut = () => {
    client.logout()
    setUser(null)
  }

  return <AuthContext value={{ user, ready, signIn, signOut }}>{children}</AuthContext>
}
