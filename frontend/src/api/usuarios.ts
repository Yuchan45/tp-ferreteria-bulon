import { api, post, put } from './client'
import type { Usuario, UsuarioInput, UsuarioUpdate } from './types'

export const listarUsuarios = (signal?: AbortSignal) => api<Usuario[]>('/usuarios', { signal })

export const crearUsuario = (input: UsuarioInput) => post<Usuario>('/usuarios', input)

/** Edita un usuario; la contraseña solo se cambia si se envía. */
export const actualizarUsuario = (id: number, input: UsuarioUpdate) => put<Usuario>(`/usuarios/${id}`, input)
