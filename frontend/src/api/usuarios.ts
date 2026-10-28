import { api, post } from './client'
import type { Usuario, UsuarioInput } from './types'

export const listarUsuarios = (signal?: AbortSignal) => api<Usuario[]>('/usuarios', { signal })

export const crearUsuario = (input: UsuarioInput) => post<Usuario>('/usuarios', input)
