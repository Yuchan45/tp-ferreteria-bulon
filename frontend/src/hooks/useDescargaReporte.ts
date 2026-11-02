import { useState } from 'react'
import { toast } from 'sonner'
import { descargarReporte, type FiltrosReporte, type FormatoReporte, type TipoReporte } from '@/api/reportes'

/** Genera y descarga un reporte; mientras tanto indica qué formato se está generando. Si falla, avisa con un toast. */
export function useDescargaReporte() {
  const [generando, setGenerando] = useState<FormatoReporte | null>(null)

  async function descargar(tipo: TipoReporte, formato: FormatoReporte, filtros: FiltrosReporte) {
    setGenerando(formato)
    try {
      await descargarReporte(tipo, formato, filtros)
    } catch (e) {
      toast.error('No se pudo generar el reporte', { description: (e as Error).message })
    } finally {
      setGenerando(null)
    }
  }

  return { generando, descargar }
}
