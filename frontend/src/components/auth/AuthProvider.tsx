import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { listarSucursales } from '@/api/catalogo'
import { configurarSesion } from '@/api/client'
import type { Sesion, Sucursal } from '@/api/types'
import { AuthContext } from '@/hooks/auth-context'

const CLAVE = 'bulon.sesion'

/**
 * La sesión se guarda en localStorage para sobrevivir a recargas de la página.
 * Es lo más simple para un TP; en producción conviene una cookie httpOnly, que el
 * JavaScript de la página no puede leer (protege el token ante ataques XSS).
 */
function leerSesion(): Sesion | null {
  try {
    const guardada = localStorage.getItem(CLAVE)
    if (!guardada) return null
    const sesion = JSON.parse(guardada) as Sesion
    if (new Date(sesion.expira) <= new Date()) {
      localStorage.removeItem(CLAVE)
      return null
    }
    return sesion
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(() => {
    const inicial = leerSesion()
    // El cliente HTTP necesita el token antes del primer request de cualquier vista
    configurarSesion(inicial?.token ?? null)
    return inicial
  })
  const [sucursal, setSucursal] = useState<Sucursal | null>(null)

  const cerrarSesion = useCallback(() => {
    try {
      localStorage.removeItem(CLAVE)
    } catch {
      // almacenamiento no disponible: alcanza con limpiar el estado
    }
    configurarSesion(null)
    setSesion(null)
    setSucursal(null)
  }, [])

  const iniciarSesion = useCallback((nueva: Sesion) => {
    try {
      localStorage.setItem(CLAVE, JSON.stringify(nueva))
    } catch {
      // sin almacenamiento la sesión dura hasta recargar la página
    }
    configurarSesion(nueva.token)
    setSesion(nueva)
  }, [])

  // Si el backend rechaza el token (venció o se invalidó), se cierra la sesión
  useEffect(() => configurarSesion(sesion?.token ?? null, cerrarSesion), [sesion, cerrarSesion])

  // Sucursal activa: la del empleado; el administrador trabaja con la primera (hoy hay una)
  const usuario = sesion?.usuario ?? null
  useEffect(() => {
    if (!usuario) return
    const controller = new AbortController()
    listarSucursales(controller.signal).then(
      (lista) => setSucursal(lista.find((s) => s.id === usuario.sucursalId) ?? lista[0] ?? null),
      () => {},
    )
    return () => controller.abort()
  }, [usuario])

  const valor = useMemo(
    () => ({
      usuario,
      sucursal,
      esAdmin: usuario?.rol === 'ADMINISTRADOR',
      cargando: usuario != null && sucursal == null,
      iniciarSesion,
      cerrarSesion,
    }),
    [usuario, sucursal, iniciarSesion, cerrarSesion],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
