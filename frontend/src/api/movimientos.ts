import { api, post } from './client'
import type { FiltroMovimientos, Movimiento, MovimientoInput } from './types'

/** Historial (del más reciente al más antiguo) con el stock resultante de cada movimiento. */
export function listarMovimientos(filtros: FiltroMovimientos, signal?: AbortSignal) {
  const params = new URLSearchParams()
  for (const [clave, valor] of Object.entries(filtros)) {
    if (valor != null && String(valor).trim() !== '') params.set(clave, String(valor).trim())
  }
  return api<Movimiento[]>(`/movimientos?${params}`, { signal })
}

export const registrarMovimiento = (input: MovimientoInput) => post<unknown>('/movimientos', input)
