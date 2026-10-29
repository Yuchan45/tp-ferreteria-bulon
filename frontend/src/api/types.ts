/** Tipos que espejan los DTOs y entidades que expone el backend (modelo v2). */

export type TipoMovimiento =
  | 'INGRESO_COMPRA'
  | 'SALIDA_VENTA'
  | 'DEVOLUCION_CLIENTE'
  | 'DEVOLUCION_PROVEEDOR'
  | 'ROTURA'
  | 'AJUSTE'

export type Rol = 'ADMINISTRADOR' | 'EMPLEADO'

export type UnidadMedida = 'UNIDAD' | 'METRO' | 'KILOGRAMO' | 'LITRO' | 'CAJA' | 'ROLLO'

/** Estado calculado por el backend: CRITICO ≤ mínimo, REPONER ≤ punto de pedido. */
export type EstadoStock = 'OK' | 'REPONER' | 'CRITICO'

export interface Sucursal {
  id: number
  codigo: string
  nombre: string
  direccion: string | null
  telefono: string | null
  activa: boolean
}

export interface Categoria {
  id: number
  nombre: string
}

/** ProductoDTO. `stockTotal` suma todas las sucursales (solo lectura). */
export interface Producto {
  id: number
  codigo: string
  nombre: string
  descripcion: string | null
  categoriaId: number | null
  unidadMedida: UnidadMedida
  precio: number | null
  activo: boolean
  stockTotal: number
}

export type ProductoInput = Pick<Producto, 'codigo' | 'nombre' | 'descripcion' | 'categoriaId' | 'unidadMedida' | 'precio'> & {
  activo?: boolean
}

/** StockSucursalDTO: stock de un producto en una sucursal. */
export interface StockSucursal {
  productoId: number
  codigo: string
  nombre: string
  sucursalId: number
  sucursal: string
  cantidad: number
  stockMinimo: number
  puntoPedido: number
  estado: EstadoStock
}

/** ProductoProveedorDTO: un proveedor de un producto y sus condiciones. */
export interface ProductoProveedor {
  proveedorId: number
  razonSocial: string
  preferido: boolean
  codigoProveedor: string | null
  precioCosto: number | null
  plazoEntregaDias: number | null
}

/** MovimientoStockDTO. `fecha` llega como LocalDateTime ISO sin zona. */
export interface Movimiento {
  id: number
  productoId: number
  sucursalId: number
  usuarioId: number
  tipo: TipoMovimiento
  cantidad: number
  fecha: string
  motivo: string | null
  observaciones: string | null
  referencia: string | null
  proveedorId: number | null
}

/** El responsable no se envía: el backend lo toma del token del usuario logueado. */
export interface MovimientoInput {
  productoId: number
  sucursalId: number
  tipo: TipoMovimiento
  cantidad: number
  motivo?: string
  observaciones?: string
  referencia?: string
  proveedorId?: number
}

export interface Proveedor {
  id: number
  razonSocial: string
  cuit: string | null
  telefono: string | null
  email: string | null
  contacto: string | null
  activo: boolean
}

export type ProveedorInput = Omit<Proveedor, 'id' | 'activo'>

/** UsuarioDTO: nunca incluye la contraseña. */
export interface Usuario {
  id: number
  username: string
  nombre: string
  email: string | null
  rol: Rol
  sucursalId: number | null
  activo: boolean
  creado: string
}

export interface UsuarioInput {
  username: string
  nombre: string
  email: string | null
  password: string
  rol: Rol
  sucursalId: number | null
  activo: boolean
}

/** Respuesta de /auth/login y /auth/registro. */
export interface Sesion {
  token: string
  expira: string
  usuario: Usuario
}
