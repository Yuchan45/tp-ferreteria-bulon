import type { Movimiento, Producto, TipoMovimiento } from '@/api/types'

export type EstadoStock = 'OK' | 'BAJO' | 'SIN_STOCK'

/** Misma regla que Producto.requiereReposicion() en el backend: stock ≤ mínimo. */
export function estadoStock(p: Pick<Producto, 'stockActual' | 'stockMinimo'>): EstadoStock {
  if (p.stockActual <= 0) return 'SIN_STOCK'
  if (p.stockActual <= p.stockMinimo) return 'BAJO'
  return 'OK'
}

export const faltante = (p: Pick<Producto, 'stockActual' | 'stockMinimo'>) =>
  Math.max(p.stockMinimo - p.stockActual, 0)

/** Ordena por urgencia: primero sin stock, luego por mayor proporción faltante. */
export function porUrgencia(a: Producto, b: Producto) {
  const ratio = (p: Producto) => (p.stockMinimo > 0 ? p.stockActual / p.stockMinimo : 1)
  return ratio(a) - ratio(b)
}

/** Efecto de un movimiento sobre el stock (igual que MovimientoStockService.registrar). */
export function delta(tipo: TipoMovimiento, cantidad: number) {
  switch (tipo) {
    case 'ENTRADA':
      return Math.abs(cantidad)
    case 'SALIDA':
      return -Math.abs(cantidad)
    case 'AJUSTE':
      return cantidad
  }
}

export interface MovimientoConStock extends Movimiento {
  /** Stock del producto inmediatamente después del movimiento. */
  stockResultante: number
}

/**
 * El backend no guarda el stock resultante de cada movimiento: se reconstruye
 * hacia atrás partiendo del stock actual. `movimientos` debe venir del más
 * reciente al más antiguo (como lo devuelve GET /movimientos?productoId=).
 */
export function conStockResultante(movimientos: Movimiento[], stockActual: number): MovimientoConStock[] {
  let stock = stockActual
  return movimientos.map((m) => {
    const fila = { ...m, stockResultante: stock }
    stock -= delta(m.tipo, m.cantidad)
    return fila
  })
}
