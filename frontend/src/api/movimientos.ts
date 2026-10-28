import { api, post } from './client'
import type { Movimiento, MovimientoInput } from './types'

/** Historial de un producto, del más reciente al más antiguo. */
export const listarMovimientos = (productoId: number, signal?: AbortSignal) =>
  api<Movimiento[]>(`/movimientos?productoId=${productoId}`, { signal })

export const registrarMovimiento = (input: MovimientoInput) =>
  post<Movimiento>('/movimientos', input)
