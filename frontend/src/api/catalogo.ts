import { api } from './client'
import type { Categoria, StockSucursal, Sucursal } from './types'

/** Público: lo usa también el formulario de registro. */
export const listarSucursales = (signal?: AbortSignal) => api<Sucursal[]>('/sucursales', { signal })

export const listarCategorias = (signal?: AbortSignal) => api<Categoria[]>('/categorias', { signal })

/** Stock de todos los productos en una sucursal, con su estado. */
export const listarStock = (sucursalId: number, signal?: AbortSignal) =>
  api<StockSucursal[]>(`/stock?sucursalId=${sucursalId}`, { signal })
