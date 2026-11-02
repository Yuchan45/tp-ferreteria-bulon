import { descargar } from './client'
import type { EstadoStock, TipoMovimiento } from './types'

export type TipoReporte = 'stock' | 'movimientos' | 'a-reponer'
export type FormatoReporte = 'pdf' | 'excel'

/** Filtros que acepta cada reporte; los vacíos no se envían. */
export interface FiltrosReporte {
  sucursalId: number
  categoriaId?: number | null
  estado?: EstadoStock | null
  tipo?: TipoMovimiento | null
  /** yyyy-mm-dd */
  desde?: string
  hasta?: string
  /** Búsqueda por código o nombre de producto. */
  q?: string
}

/** Genera el reporte en el backend y lo descarga (el backend elige el adaptador del formato). */
export function descargarReporte(tipo: TipoReporte, formato: FormatoReporte, filtros: FiltrosReporte) {
  const params = new URLSearchParams({ formato })
  for (const [clave, valor] of Object.entries(filtros)) {
    if (valor != null && String(valor).trim() !== '') params.set(clave, String(valor).trim())
  }
  return descargar(`/reportes/${tipo}?${params}`, `${tipo}.${formato === 'pdf' ? 'pdf' : 'xlsx'}`)
}
