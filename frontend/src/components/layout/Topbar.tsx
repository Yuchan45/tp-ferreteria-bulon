import { Plus } from 'lucide-react'
import { useRegistrarMovimiento } from '@/components/dialogs/registrar-context'
import { Button } from '@/components/ui/button'
import type { Connection } from '@/hooks/useHealth'
import { cn } from '@/lib/utils'

const CONEXION: Record<Connection, { label: string; dot: string }> = {
  checking: { label: 'Verificando…', dot: 'bg-muted-foreground' },
  online: { label: 'Conectado', dot: 'bg-stock-ok' },
  degraded: { label: 'Base de datos caída', dot: 'bg-stock-low' },
  offline: { label: 'Sin conexión', dot: 'bg-stock-out' },
}

export function Topbar({ titulo, conexion }: { titulo: string; conexion: Connection }) {
  const abrirMovimiento = useRegistrarMovimiento()
  const { label, dot } = CONEXION[conexion]
  return (
    <header className="sticky top-0 z-10 flex h-16 items-center gap-4 border-b bg-card px-8">
      <h1 className="flex-1 text-2xl font-semibold">{titulo}</h1>
      <span className="flex items-center gap-1.5 rounded-full bg-muted px-2.5 py-1 text-[13px] text-muted-foreground" aria-live="polite">
        <span className={cn('size-2 rounded-full', dot)} aria-hidden />
        {label}
      </span>
      <Button onClick={() => abrirMovimiento()}>
        <Plus /> Registrar movimiento
      </Button>
    </header>
  )
}
