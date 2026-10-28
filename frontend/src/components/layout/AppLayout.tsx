import { useCallback, useMemo, useState } from 'react'
import { RefreshCw, WifiOff } from 'lucide-react'
import { Outlet, useMatches } from 'react-router'
import { Aviso } from '@/components/common'
import { MovimientoDialog } from '@/components/dialogs/MovimientoDialog'
import { RegistrarMovimientoContext, type PresetMovimiento } from '@/components/dialogs/registrar-context'
import { Button } from '@/components/ui/button'
import { Toaster } from '@/components/ui/sonner'
import { DataContext } from '@/hooks/data-context'
import { useHealth } from '@/hooks/useHealth'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

export interface RouteHandle {
  title: string
}

export function AppLayout() {
  const [version, setVersion] = useState(0)
  const invalidate = useCallback(() => setVersion((v) => v + 1), [])
  const data = useMemo(() => ({ version, invalidate }), [version, invalidate])

  // Cada apertura remonta el formulario (key) para que arranque limpio con su preset
  const [dialogo, setDialogo] = useState<{ open: boolean; key: number; preset: PresetMovimiento }>({ open: false, key: 0, preset: {} })
  const abrirMovimiento = useCallback(
    (preset: PresetMovimiento = {}) => setDialogo((d) => ({ open: true, key: d.key + 1, preset })),
    [],
  )

  const health = useHealth()
  const matches = useMatches()
  const titulo = (matches.at(-1)?.handle as RouteHandle | undefined)?.title ?? 'Ferretería Bulón'

  return (
    <DataContext.Provider value={data}>
      <RegistrarMovimientoContext.Provider value={abrirMovimiento}>
        <div className="flex min-h-screen">
          <Sidebar />
          <div className="flex min-w-0 flex-1 flex-col">
            <Topbar titulo={titulo} conexion={health.connection} />
            <main className="flex flex-1 flex-col gap-6 px-8 pt-6 pb-8">
              {(health.connection === 'offline' || health.connection === 'degraded') && (
                <Aviso
                  icon={WifiOff}
                  tono="error"
                  titulo={health.connection === 'offline' ? 'No se pudo conectar con el servidor' : 'La base de datos no está disponible'}
                  accion={
                    <Button
                      variant="outline"
                      disabled={health.checking}
                      onClick={async () => {
                        await health.refresh()
                        invalidate()
                      }}
                    >
                      <RefreshCw /> Reintentar
                    </Button>
                  }
                >
                  Verificá que el backend esté corriendo en el puerto 8080. Reintentamos cada 15 segundos.
                </Aviso>
              )}
              <Outlet />
            </main>
          </div>
        </div>
        <MovimientoDialog
          key={dialogo.key}
          open={dialogo.open}
          onOpenChange={(open) => setDialogo((d) => ({ ...d, open }))}
          preset={dialogo.preset}
        />
        <Toaster position="bottom-right" />
      </RegistrarMovimientoContext.Provider>
    </DataContext.Provider>
  )
}
