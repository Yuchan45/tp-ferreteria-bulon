import { createContext, useContext } from 'react'
import type { TipoMovimiento } from '@/api/types'

export interface PresetMovimiento {
  productoId?: number
  tipo?: TipoMovimiento
  cantidad?: number
}

/** Abre el diálogo global "Registrar movimiento" (vive en AppLayout). */
export const RegistrarMovimientoContext = createContext<(preset?: PresetMovimiento) => void>(() => {})

export const useRegistrarMovimiento = () => useContext(RegistrarMovimientoContext)
