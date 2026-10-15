// Espejo de los DTO del backend (com.bulon.stock.*). Las fechas llegan como ISO sin zona.

export type Rol = 'ADMINISTRADOR' | 'ENCARGADO_DEPOSITO' | 'VENDEDOR'
export type UnidadMedida = 'UNIDAD' | 'METRO' | 'KILOGRAMO' | 'LITRO' | 'CAJA' | 'ROLLO' | 'PAQUETE'
export type EstadoStock = 'BAJO_MINIMO' | 'PUNTO_PEDIDO' | 'OK'
export type TipoMovimiento =
  | 'INGRESO_COMPRA'
  | 'SALIDA_VENTA'
  | 'DEVOLUCION'
  | 'ROTURA_PERDIDA'
  | 'AJUSTE'
  | 'TRASLADO'
export type EstadoOrdenCompra = 'BORRADOR' | 'ENVIADA' | 'RECIBIDA' | 'CANCELADA'
export type FormatoReporte = 'PDF' | 'EXCEL'

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface Usuario {
  id: number
  username: string
  nombre: string
  rol: Rol
  activo: boolean
}

export interface UsuarioRequest {
  username: string
  nombre: string
  rol: Rol
  activo: boolean
  password?: string
}

export interface Ubicacion {
  id: number
  nombre: string
}

export interface Categoria {
  id: number
  nombre: string
}

export interface Existencia {
  ubicacionId: number
  ubicacion: string
  cantidad: number
}

export interface Producto {
  id: number
  codigo: string
  nombre: string
  descripcion: string | null
  categoriaId: number
  categoria: string
  unidadMedida: UnidadMedida
  precio: number | null
  stockMinimo: number
  puntoPedido: number
  activo: boolean
  proveedorIds: number[]
  proveedorPreferidoId: number | null
  existencias: Existencia[]
  stockTotal: number
  estadoStock: EstadoStock
}

export interface ProductoRequest {
  codigo: string
  nombre: string
  descripcion?: string
  categoriaId: number
  unidadMedida: UnidadMedida
  precio?: number
  stockMinimo: number
  puntoPedido: number
  activo: boolean
  proveedorIds: number[]
  proveedorPreferidoId?: number
}

export interface Proveedor {
  id: number
  razonSocial: string
  cuit: string | null
  telefono: string | null
  email: string | null
}

export type ProveedorRequest = Omit<Proveedor, 'id'>

export interface Movimiento {
  id: number
  fecha: string
  tipo: TipoMovimiento
  productoId: number
  productoCodigo: string
  productoNombre: string
  cantidad: number
  ubicacionId: number
  ubicacion: string
  ubicacionDestinoId: number | null
  ubicacionDestino: string | null
  usuario: string
  motivo: string | null
  observacion: string | null
}

export interface MovimientoRequest {
  productoId: number
  tipo: TipoMovimiento
  cantidad: number
  ubicacionId: number
  ubicacionDestinoId?: number
  motivo?: string
  observacion?: string
}

export interface Alerta {
  id: number
  productoId: number
  productoCodigo: string
  productoNombre: string
  estado: EstadoStock
  stock: number
  fecha: string
  resueltaEn: string | null
  mensaje: string
}

export interface Dashboard {
  productosActivos: number
  pendientesReposicion: number
  bajoMinimo: number
  alertasAbiertas: number
  movimientosHoy: number
  unidadesPorUbicacion: { ubicacionId: number; ubicacion: string; unidades: number }[]
  ultimosMovimientos: Movimiento[]
}

export interface Rotacion {
  productoId: number
  codigo: string
  nombre: string
  unidadesVendidas: number
  stockTotal: number
  puntoPedido: number
  puntoPedidoSugerido: number
}

export interface OrdenCompra {
  id: number
  proveedorId: number
  proveedor: string
  estado: EstadoOrdenCompra
  usuario: string
  fechaCreacion: string
  fechaRecepcion: string | null
  ubicacionRecepcion: string | null
  observacion: string | null
  items: { productoId: number; productoCodigo: string; productoNombre: string; cantidad: number }[]
}

export interface OrdenCompraRequest {
  proveedorId: number
  observacion?: string
  items: { productoId: number; cantidad: number }[]
}

export interface Sugerencia {
  proveedorId: number | null
  proveedor: string | null
  items: {
    productoId: number
    productoCodigo: string
    productoNombre: string
    estado: EstadoStock
    stockTotal: number
    puntoPedido: number
    cantidadSugerida: number
  }[]
}
