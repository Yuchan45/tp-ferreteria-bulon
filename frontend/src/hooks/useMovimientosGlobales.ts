import { listarStock } from '@/api/catalogo'
import { listarMovimientos } from '@/api/movimientos'
import type { StockSucursal } from '@/api/types'
import { conStockResultante, type MovimientoConStock } from '@/lib/stock'
import { useApi } from './useApi'

export interface MovimientoGlobal extends MovimientoConStock {
  producto: StockSucursal
}

/**
 * Historial de todos los productos de una sucursal, del más reciente al más antiguo.
 *
 * El backend solo expone GET /movimientos?productoId=, así que se consulta el
 * historial de cada producto y se combinan. Son datos reales; cuando exista un
 * GET /movimientos global, reemplazar esta función por una sola llamada.
 */
export function useMovimientosGlobales(sucursalId: number) {
  return useApi(
    async (signal) => {
      const stock = await listarStock(sucursalId, signal)
      const historiales = await Promise.all(
        stock.map(async (producto) => {
          const movimientos = await listarMovimientos(producto.productoId, sucursalId, signal)
          return conStockResultante(movimientos, producto.cantidad).map((m) => ({ ...m, producto }))
        }),
      )
      return historiales
        .flat()
        .sort((a, b) => b.fecha.localeCompare(a.fecha) || b.id - a.id) satisfies MovimientoGlobal[]
    },
    [sucursalId],
  )
}
