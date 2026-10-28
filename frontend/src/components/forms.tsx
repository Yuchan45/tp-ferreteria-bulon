import type { ReactNode } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

/** Etiqueta + control + texto de ayuda o error, como el componente Input del diseño. */
export function Campo({ label, htmlFor, error, ayuda, children, className }: { label: string; htmlFor: string; error?: string; ayuda?: string; children: ReactNode; className?: string }) {
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <Label htmlFor={htmlFor} className="text-[13px] font-medium">
        {label}
      </Label>
      {children}
      {error ? (
        <p id={`${htmlFor}-error`} className="text-[13px] text-stock-out">
          {error}
        </p>
      ) : (
        ayuda && <p className="text-[13px] text-muted-foreground">{ayuda}</p>
      )}
    </div>
  )
}

/** Ventana emergente con encabezado, cuerpo y barra de acciones, envuelta en un <form>. */
export function Modal({ open, onOpenChange, titulo, descripcion, onSubmit, acciones, children, className }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  titulo: string
  descripcion?: string
  onSubmit: () => void
  acciones: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={cn('gap-0 overflow-hidden p-0 sm:max-w-140', className)}>
        <form
          noValidate
          onSubmit={(e) => {
            e.preventDefault()
            onSubmit()
          }}
        >
          <DialogHeader className="gap-1 border-b px-6 pt-5 pb-4">
            <DialogTitle className="text-lg font-semibold">{titulo}</DialogTitle>
            {descripcion && <DialogDescription className="text-[13px]">{descripcion}</DialogDescription>}
          </DialogHeader>
          <div className="flex flex-col gap-4 p-6">{children}</div>
          <div className="flex justify-end gap-2 border-t px-6 py-4">{acciones}</div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
