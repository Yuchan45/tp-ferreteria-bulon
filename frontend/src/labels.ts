import type { EstadoOrdenCompra, EstadoStock, TipoMovimiento } from './api/types'

export const ESTADO_STOCK: Record<EstadoStock, { texto: string; clase: string }> = {
  OK: { texto: 'OK', clase: 'ok' },
  PUNTO_PEDIDO: { texto: 'Punto de pedido', clase: 'warn' },
  BAJO_MINIMO: { texto: 'Bajo mínimo', clase: 'error' },
}

export const ESTADO_ORDEN: Record<EstadoOrdenCompra, { texto: string; clase: string }> = {
  BORRADOR: { texto: 'Borrador', clase: 'pending' },
  ENVIADA: { texto: 'Enviada', clase: 'warn' },
  RECIBIDA: { texto: 'Recibida', clase: 'ok' },
  CANCELADA: { texto: 'Cancelada', clase: 'error' },
}

export const TIPOS: Record<TipoMovimiento, string> = {
  INGRESO_COMPRA: 'Ingreso por compra',
  SALIDA_VENTA: 'Salida por venta',
  DEVOLUCION: 'Devolución',
  ROTURA_PERDIDA: 'Rotura o pérdida',
  AJUSTE: 'Ajuste de inventario',
  TRASLADO: 'Traslado',
}

export const fecha = (iso: string) =>
  new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short', hourCycle: 'h23' })
