/** Tipos que espejan los DTOs y entidades que expone el backend. */

export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE'

export type Rol = 'ADMINISTRADOR' | 'ENCARGADO_DEPOSITO' | 'VENDEDOR'

/** ProductoDTO */
export interface Producto {
  id: number
  codigo: string
  descripcion: string
  precio: number | null
  stockActual: number
  stockMinimo: number
  proveedorId: number | null
}

export type ProductoInput = Pick<Producto, 'codigo' | 'descripcion' | 'precio' | 'stockMinimo' | 'proveedorId'>

/** MovimientoStockDTO. `fecha` llega como LocalDateTime ISO sin zona. */
export interface Movimiento {
  id: number
  productoId: number
  tipo: TipoMovimiento
  cantidad: number
  fecha: string
  observacion: string | null
}

export interface MovimientoInput {
  productoId: number
  tipo: TipoMovimiento
  cantidad: number
  observacion?: string
}

/** Entidad Proveedor */
export interface Proveedor {
  id: number
  razonSocial: string
  cuit: string | null
  telefono: string | null
  email: string | null
}

export type ProveedorInput = Omit<Proveedor, 'id'>

/** Entidad Usuario */
export interface Usuario {
  id: number
  username: string
  nombre: string
  rol: Rol
  activo: boolean
}

export type UsuarioInput = Omit<Usuario, 'id'>
