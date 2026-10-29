import type { ReactNode } from 'react'

function Logo() {
  return (
    <svg className="size-10" viewBox="0 0 32 32" aria-hidden="true">
      <path className="fill-brand" d="M16 1.5 28.6 8.75v14.5L16 30.5 3.4 23.25V8.75z" />
      <circle cx="16" cy="16" r="6" className="fill-card" />
    </svg>
  )
}

/** Marco común de login y registro: tarjeta centrada con la marca. */
export function AuthLayout({ titulo, descripcion, children, pie }: { titulo: string; descripcion: string; children: ReactNode; pie: ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <div className="w-full max-w-100">
        <div className="mb-6 flex items-center justify-center gap-3">
          <Logo />
          <div className="leading-tight">
            <p className="text-lg font-semibold">Ferretería Bulón</p>
            <p className="text-[13px] text-muted-foreground">Gestión de stock</p>
          </div>
        </div>
        <section className="rounded-lg border bg-card p-6">
          <h1 className="text-xl font-semibold">{titulo}</h1>
          <p className="mt-1 mb-5 text-[13px] text-muted-foreground">{descripcion}</p>
          {children}
        </section>
        <p className="mt-4 text-center text-[13px] text-muted-foreground">{pie}</p>
      </div>
    </main>
  )
}
