import { api, post, put } from './client'
import type { Producto, ProductoInput } from './types'

export const listarProductos = (signal?: AbortSignal) => api<Producto[]>('/productos', { signal })

export const obtenerProducto = (id: number, signal?: AbortSignal) =>
  api<Producto>(`/productos/${id}`, { signal })

/** Productos con stock actual menor o igual al stock mínimo. */
export const listarBajoStock = (signal?: AbortSignal) =>
  api<Producto[]>('/productos/bajo-stock', { signal })

export const crearProducto = (input: ProductoInput) => post<Producto>('/productos', input)

export const actualizarProducto = (id: number, input: ProductoInput) =>
  put<Producto>(`/productos/${id}`, input)
