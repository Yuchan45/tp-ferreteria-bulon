import { listarMovimientos } from '@/api/movimientos'
import { listarProductos } from '@/api/productos'
import type { Producto } from '@/api/types'
import { conStockResultante, type MovimientoConStock } from '@/lib/stock'
import { useApi } from './useApi'

export interface MovimientoGlobal extends MovimientoConStock {
  producto: Producto
}

/**
 * Historial de todos los productos, del más reciente al más antiguo.
 *
 * El backend solo expone GET /movimientos?productoId=, así que se consulta el
 * historial de cada producto y se combinan. Son datos reales; cuando exista un
 * GET /movimientos global, reemplazar esta función por una sola llamada.
 */
export function useMovimientosGlobales() {
  return useApi(async (signal) => {
    const productos = await listarProductos(signal)
    const historiales = await Promise.all(
      productos.map(async (producto) => {
        const movimientos = await listarMovimientos(producto.id, signal)
        return conStockResultante(movimientos, producto.stockActual).map((m) => ({ ...m, producto }))
      }),
    )
    return historiales
      .flat()
      .sort((a, b) => b.fecha.localeCompare(a.fecha) || b.id - a.id) satisfies MovimientoGlobal[]
  })
}
