import { listarMovimientos } from '@/api/movimientos'
import { useApi } from './useApi'

/**
 * Historial de todos los productos de una sucursal, del más reciente al más antiguo, en una
 * sola llamada (GET /movimientos?sucursalId=). Los filtros de la vista se aplican en el
 * cliente para poder contar cuántos movimientos hay de cada tipo.
 */
export function useMovimientosGlobales(sucursalId: number) {
  return useApi((signal) => listarMovimientos({ sucursalId }, signal), [sucursalId])
}
