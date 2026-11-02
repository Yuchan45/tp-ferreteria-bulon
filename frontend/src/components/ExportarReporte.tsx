import { useState } from 'react'
import { ChevronDown, Download, FileSpreadsheet, FileText, Loader2 } from 'lucide-react'
import type { FiltrosReporte, FormatoReporte, TipoReporte } from '@/api/reportes'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { useDescargaReporte } from '@/hooks/useDescargaReporte'

const FORMATOS: { formato: FormatoReporte; label: string; detalle: string; icono: typeof FileText }[] = [
  { formato: 'pdf', label: 'PDF', detalle: 'Para imprimir o compartir', icono: FileText },
  { formato: 'excel', label: 'Excel', detalle: 'Para analizar o filtrar los datos', icono: FileSpreadsheet },
]

/**
 * Botón "Exportar" para las vistas: exporta lo que se está viendo (con sus filtros) en PDF o Excel.
 */
export function ExportarReporte({ tipo, filtros }: { tipo: TipoReporte; filtros: FiltrosReporte }) {
  const [abierto, setAbierto] = useState(false)
  const { generando, descargar } = useDescargaReporte()

  return (
    <Popover open={abierto} onOpenChange={setAbierto}>
      <PopoverTrigger asChild>
        <Button variant="outline" disabled={generando != null}>
          {generando ? <Loader2 className="animate-spin" /> : <Download />}
          {generando ? 'Generando…' : 'Exportar'}
          <ChevronDown className="text-muted-foreground" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-64 p-1">
        <p className="px-2 pt-1.5 pb-1 text-[13px] text-muted-foreground">Con los filtros aplicados</p>
        {FORMATOS.map(({ formato, label, detalle, icono: Icono }) => (
          <button
            key={formato}
            type="button"
            onClick={() => {
              setAbierto(false)
              void descargar(tipo, formato, filtros)
            }}
            className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left hover:bg-muted"
          >
            <Icono className={formato === 'pdf' ? 'size-5 text-stock-out' : 'size-5 text-stock-ok'} aria-hidden />
            <span>
              <span className="block text-sm font-medium">{label}</span>
              <span className="block text-[13px] text-muted-foreground">{detalle}</span>
            </span>
          </button>
        ))}
      </PopoverContent>
    </Popover>
  )
}
