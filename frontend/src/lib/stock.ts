import type { EstadoStock, StockSucursal, TipoMovimiento } from '@/api/types'

/** Color de cada estado de stock (el punto que se usa en listas y filtros). */
export const PUNTO_ESTADO: Record<EstadoStock, string> = {
  OK: 'bg-stock-ok',
  REPONER: 'bg-stock-low',
  CRITICO: 'bg-stock-out',
}

/** Nombre de cada estado en los filtros. */
export const ESTADO_LABEL: Record<EstadoStock, string> = {
  OK: 'Normal',
  REPONER: 'A reponer',
  CRITICO: 'Crítico',
}

/** Cuánto falta para volver al punto de pedido. */
export const faltante = (s: Pick<StockSucursal, 'cantidad' | 'puntoPedido'>) => Math.max(s.puntoPedido - s.cantidad, 0)

/** Ordena por urgencia: primero críticos, luego por menor proporción respecto del punto de pedido. */
export function porUrgencia(a: StockSucursal, b: StockSucursal) {
  const peso = (s: StockSucursal) => (s.estado === 'CRITICO' ? 0 : 1)
  const ratio = (s: StockSucursal) => (s.puntoPedido > 0 ? s.cantidad / s.puntoPedido : 1)
  return peso(a) - peso(b) || ratio(a) - ratio(b)
}

/** Si el tipo suma o resta stock (igual que MovimientoStockService.registrar). */
const SIGNO: Record<TipoMovimiento, 1 | -1 | 0> = {
  INGRESO_COMPRA: 1,
  DEVOLUCION_CLIENTE: 1,
  SALIDA_VENTA: -1,
  DEVOLUCION_PROVEEDOR: -1,
  ROTURA: -1,
  AJUSTE: 0, // aplica la cantidad con su signo
}

export function delta(tipo: TipoMovimiento, cantidad: number) {
  const signo = SIGNO[tipo]
  return signo === 0 ? cantidad : signo * Math.abs(cantidad)
}

export const requiereProveedor = (tipo: TipoMovimiento) => tipo === 'INGRESO_COMPRA' || tipo === 'DEVOLUCION_PROVEEDOR'

export const requiereMotivo = (tipo: TipoMovimiento) =>
  tipo === 'AJUSTE' || tipo === 'ROTURA' || tipo === 'DEVOLUCION_CLIENTE' || tipo === 'DEVOLUCION_PROVEEDOR'

/** Evita errores de coma flotante con cantidades de hasta 3 decimales. */
export const redondear = (n: number) => Math.round(n * 1000) / 1000
