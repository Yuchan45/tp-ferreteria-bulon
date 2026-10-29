import { api, post } from './client'
import type { Movimiento, MovimientoInput } from './types'

/** Historial de un producto en una sucursal, del más reciente al más antiguo. */
export const listarMovimientos = (productoId: number, sucursalId: number, signal?: AbortSignal) =>
  api<Movimiento[]>(`/movimientos?productoId=${productoId}&sucursalId=${sucursalId}`, { signal })

export const registrarMovimiento = (input: MovimientoInput) =>
  post<Movimiento>('/movimientos', input)
