import { api, post } from './client'
import type { Proveedor, ProveedorInput } from './types'

export const listarProveedores = (signal?: AbortSignal) =>
  api<Proveedor[]>('/proveedores', { signal })

export const crearProveedor = (input: ProveedorInput) => post<Proveedor>('/proveedores', input)
