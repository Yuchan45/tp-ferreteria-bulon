import { api, post, put } from './client'
import type { Producto, ProductoInput, ProductoProveedor, StockSucursal } from './types'

export const listarProductos = (signal?: AbortSignal) => api<Producto[]>('/productos', { signal })

export const obtenerProducto = (id: number, signal?: AbortSignal) =>
  api<Producto>(`/productos/${id}`, { signal })

/** En o por debajo del punto de pedido en una sucursal. */
export const listarAReponer = (sucursalId: number, signal?: AbortSignal) =>
  api<StockSucursal[]>(`/productos/bajo-stock?sucursalId=${sucursalId}`, { signal })

export const crearProducto = (input: ProductoInput) => post<Producto>('/productos', input)

export const actualizarProducto = (id: number, input: ProductoInput) =>
  put<Producto>(`/productos/${id}`, input)

/** Stock del producto en cada sucursal. */
export const stockDeProducto = (id: number, signal?: AbortSignal) =>
  api<StockSucursal[]>(`/productos/${id}/stock`, { signal })

export const configurarUmbrales = (id: number, sucursalId: number, stockMinimo: number, puntoPedido: number) =>
  put<StockSucursal>(`/productos/${id}/stock/${sucursalId}`, { stockMinimo, puntoPedido })

/** Proveedores del producto, con el preferido primero. */
export const proveedoresDeProducto = (id: number, signal?: AbortSignal) =>
  api<ProductoProveedor[]>(`/productos/${id}/proveedores`, { signal })

export const asignarProveedor = (id: number, proveedorId: number, condiciones: Omit<ProductoProveedor, 'proveedorId' | 'razonSocial'>) =>
  put<ProductoProveedor>(`/productos/${id}/proveedores/${proveedorId}`, condiciones)
