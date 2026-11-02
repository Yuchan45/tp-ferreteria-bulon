import { api, post, put } from './client'
import type { Proveedor, ProveedorInput } from './types'

export const listarProveedores = (signal?: AbortSignal) =>
  api<Proveedor[]>('/proveedores', { signal })

export const crearProveedor = (input: ProveedorInput) => post<Proveedor>('/proveedores', input)

/** Edita los datos y el estado (un proveedor inactivo no admite compras). */
export const actualizarProveedor = (id: number, input: ProveedorInput & { activo: boolean }) =>
  put<Proveedor>(`/proveedores/${id}`, input)
