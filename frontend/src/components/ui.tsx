import { useEffect, useState, type ReactNode } from 'react'
import { api } from '../api/client'
import type { EstadoStock, Page, Producto } from '../api/types'
import { ESTADO_STOCK } from '../labels'

export function Badge({ clase, children }: { clase: string; children: ReactNode }) {
  return <span className={`badge badge--${clase}`}>{children}</span>
}

export function EstadoBadge({ estado }: { estado: EstadoStock }) {
  const { texto, clase } = ESTADO_STOCK[estado]
  return <Badge clase={clase}>{texto}</Badge>
}

export function Heading({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="page-heading">
      <h1>{title}</h1>
      {children && <p>{children}</p>}
    </div>
  )
}

export function ErrorMsg({ error }: { error: string | null }) {
  return error ? <p className="msg msg--error" role="alert">{error}</p> : null
}

export function Field({ label, children, wide }: { label: string; children: ReactNode; wide?: boolean }) {
  return (
    <label className={`field${wide ? ' field--wide' : ''}`}>
      <span>{label}</span>
      {children}
    </label>
  )
}

export function Table({ children, empty }: { children: ReactNode; empty?: string }) {
  return (
    <div className="table-wrap">
      <table>{children}</table>
      {empty && <p className="empty">{empty}</p>}
    </div>
  )
}

export function Pager({ info, onPage }: { info: Page<unknown>['page'] | undefined; onPage: (p: number) => void }) {
  if (!info || info.totalPages <= 1) return null
  return (
    <div className="pager">
      <button type="button" className="button button--ghost" disabled={info.number === 0} onClick={() => onPage(info.number - 1)}>
        Anterior
      </button>
      <span>
        Página {info.number + 1} de {info.totalPages} · {info.totalElements} resultados
      </span>
      <button type="button" className="button button--ghost" disabled={info.number + 1 >= info.totalPages} onClick={() => onPage(info.number + 1)}>
        Siguiente
      </button>
    </div>
  )
}

/** Busca productos activos por código o nombre (GET /productos?q=) y devuelve el elegido. */
export function ProductPicker({ value, onChange }: { value: Producto | null; onChange: (p: Producto | null) => void }) {
  const [q, setQ] = useState('')
  const [results, setResults] = useState<Producto[]>([])

  useEffect(() => {
    if (!q.trim()) return
    let vigente = true
    const timer = setTimeout(() => {
      api.get<Page<Producto>>('/productos', { q, activo: true, size: 8 }).then(
        (r) => vigente && setResults(r.content),
        () => vigente && setResults([]),
      )
    }, 250)
    return () => {
      vigente = false
      clearTimeout(timer)
    }
  }, [q])

  if (value) {
    return (
      <div className="picked">
        <span>
          <strong>{value.codigo}</strong> · {value.nombre}
        </span>
        <button type="button" className="button button--ghost" onClick={() => onChange(null)}>
          Cambiar
        </button>
      </div>
    )
  }
  return (
    <div className="picker">
      <input
        type="search"
        placeholder="Buscar producto por código o nombre"
        value={q}
        onChange={(e) => {
          setQ(e.target.value)
          if (!e.target.value.trim()) setResults([])
        }}
      />
      {q.trim() && results.length > 0 && (
        <ul className="picker__list">
          {results.map((p) => (
            <li key={p.id}>
              <button type="button" onClick={() => onChange(p)}>
                <strong>{p.codigo}</strong> · {p.nombre} <small>(stock {p.stockTotal})</small>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
