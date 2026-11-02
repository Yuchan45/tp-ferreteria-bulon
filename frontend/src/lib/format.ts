import type { Rol, TipoMovimiento, UnidadMedida } from '@/api/types'

// Hasta 3 decimales: las cantidades por metro, kilo o litro pueden ser fraccionarias
const enteros = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 })
const pesos = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

export const fmtNumero = (n: number) => enteros.format(n)

/** Cantidad con signo explícito: +500, −2 (con signo menos tipográfico). */
export const fmtDelta = (n: number) => (n > 0 ? `+${fmtNumero(n)}` : n < 0 ? `−${fmtNumero(-n)}` : '0')

/** Convierte el texto de un input numérico; '' o inválido devuelve null. */
export function aNumero(valor: string): number | null {
  if (valor.trim() === '') return null
  const n = Number(valor.replace(',', '.'))
  return Number.isFinite(n) ? n : null
}

export const fmtPrecio = (n: number | null) => (n == null ? '—' : pesos.format(n))

// El backend envía LocalDateTime sin zona: se interpreta como hora local.
const fecha = (iso: string) => new Date(iso)

export const fmtFechaHora = (iso: string) =>
  fecha(iso).toLocaleString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).replace(',', '')

export const fmtHora = (iso: string) =>
  fecha(iso).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })

export const esHoy = (iso: string) => fecha(iso).toDateString() === new Date().toDateString()

/** yyyy-mm-dd en hora local, para comparar con <input type="date">. */
export const diaLocal = (iso: string) => {
  const d = fecha(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export const TIPO_LABEL: Record<TipoMovimiento, string> = {
  INGRESO_COMPRA: 'Compra',
  SALIDA_VENTA: 'Venta',
  DEVOLUCION_CLIENTE: 'Devolución de cliente',
  DEVOLUCION_PROVEEDOR: 'Devolución a proveedor',
  ROTURA: 'Rotura',
  AJUSTE: 'Ajuste',
}

export const ROL_LABEL: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  EMPLEADO: 'Empleado',
}

export const UNIDAD_LABEL: Record<UnidadMedida, string> = {
  UNIDAD: 'Unidad',
  METRO: 'Metro',
  KILOGRAMO: 'Kilogramo',
  LITRO: 'Litro',
  CAJA: 'Caja',
  ROLLO: 'Rollo',
}

/** Solo estas unidades admiten cantidades con decimales (igual que el backend). */
export const esFraccionable = (u: UnidadMedida) => u === 'METRO' || u === 'KILOGRAMO' || u === 'LITRO'

export const iniciales = (nombre: string) =>
  nombre
    .split(/\s+/)
    .filter((p) => p.length > 2 || /^[A-ZÁÉÍÓÚ]/.test(p))
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('')

/** Comparación de texto sin acentos ni mayúsculas, para los buscadores. */
export const incluye = (texto: string | null | undefined, busqueda: string) =>
  (texto ?? '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .includes(busqueda.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim())
