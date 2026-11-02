import { Plus } from 'lucide-react'
import { useRegistrarMovimiento } from '@/components/dialogs/registrar-context'
import { Button } from '@/components/ui/button'
import { Notificaciones } from './Notificaciones'

export function Topbar({ titulo }: { titulo: string }) {
  const abrirMovimiento = useRegistrarMovimiento()
  return (
    <header className="sticky top-0 z-10 flex h-16 items-center gap-4 border-b bg-card px-8">
      <h1 className="flex-1 text-2xl font-semibold">{titulo}</h1>
      <Notificaciones />
      <Button onClick={() => abrirMovimiento()}>
        <Plus /> Registrar movimiento
      </Button>
    </header>
  )
}
