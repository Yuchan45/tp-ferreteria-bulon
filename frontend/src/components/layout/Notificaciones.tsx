import { useEffect, useState } from 'react'
import { Bell, CheckCheck } from 'lucide-react'
import { useNavigate } from 'react-router'
import { marcarLeida, marcarTodasLeidas, obtenerBandeja } from '@/api/notificaciones'
import type { Notificacion } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { useApi } from '@/hooks/useApi'
import { fmtFechaHora } from '@/lib/format'
import { cn } from '@/lib/utils'

/** Cada cuánto se consulta si hay notificaciones nuevas (las genera el backend al abrirse una alerta). */
const INTERVALO_MS = 30_000

/** Campanita de la barra superior: las notificaciones del usuario logueado. */
export function Notificaciones() {
  const bandeja = useApi((signal) => obtenerBandeja(signal))
  const [abierto, setAbierto] = useState(false)
  const navigate = useNavigate()
  const { reload } = bandeja

  useEffect(() => {
    const id = setInterval(reload, INTERVALO_MS)
    return () => clearInterval(id)
  }, [reload])

  const noLeidas = bandeja.data?.noLeidas ?? 0
  const lista = bandeja.data?.notificaciones ?? []

  async function abrir(n: Notificacion) {
    setAbierto(false)
    if (!n.leida) await marcarLeida(n.id).catch(() => {})
    reload()
    if (n.productoId != null) navigate(`/productos/${n.productoId}`)
  }

  async function leerTodas() {
    await marcarTodasLeidas().catch(() => {})
    reload()
  }

  return (
    <Popover open={abierto} onOpenChange={setAbierto}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative" aria-label={`Notificaciones${noLeidas ? `: ${noLeidas} sin leer` : ''}`}>
          <Bell />
          {noLeidas > 0 && (
            <span className="absolute -top-0.5 -right-0.5 flex h-4.5 min-w-4.5 items-center justify-center rounded-full bg-stock-out px-1 text-[11px] font-semibold text-white tabular-nums">
              {noLeidas > 99 ? '99+' : noLeidas}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-96 p-0">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <p className="font-semibold">Notificaciones</p>
          {noLeidas > 0 && (
            <Button variant="ghost" size="sm" onClick={leerTodas}>
              <CheckCheck /> Marcar todas como leídas
            </Button>
          )}
        </div>
        {bandeja.error ? (
          <p className="px-4 py-6 text-center text-muted-foreground">No se pudieron cargar las notificaciones.</p>
        ) : lista.length === 0 ? (
          <p className="px-4 py-6 text-center text-muted-foreground">No hay notificaciones.</p>
        ) : (
          <ul className="max-h-96 overflow-y-auto">
            {lista.map((n) => (
              <li key={n.id} className="border-b last:border-b-0">
                <button
                  type="button"
                  onClick={() => abrir(n)}
                  className={cn('flex w-full gap-3 px-4 py-3 text-left hover:bg-muted', n.leida && 'text-muted-foreground')}
                >
                  <span
                    className={cn(
                      'mt-1.5 size-2 shrink-0 rounded-full',
                      n.leida ? 'bg-transparent' : n.nivel === 'CRITICO' ? 'bg-stock-out' : 'bg-stock-low',
                    )}
                    aria-hidden
                  />
                  <span className="min-w-0 flex-1">
                    <span className={cn('block', !n.leida && 'font-medium')}>{n.titulo}</span>
                    <span className="block text-[13px] text-muted-foreground">{n.mensaje}</span>
                    <span className="block text-[12px] text-muted-foreground tabular-nums">{fmtFechaHora(n.creada)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </PopoverContent>
    </Popover>
  )
}
