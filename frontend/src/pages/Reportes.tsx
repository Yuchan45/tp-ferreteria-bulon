import { useState } from 'react'
import { api, download } from '../api/client'
import type { Rotacion } from '../api/types'
import { ErrorMsg, Field, Heading, Table } from '../components/ui'
import { useAction, useAsync } from '../hooks/useAsync'

export default function Reportes() {
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [limite, setLimite] = useState('10')
  const exportar = useAction()

  const rotacion = useAsync(
    () => api.get<Rotacion[]>('/reportes/rotacion', { desde, hasta, limite }),
    `${desde}|${hasta}|${limite}`,
  )

  return (
    <>
      <Heading title="Reportes">Rotación de productos e inventario completo.</Heading>

      <section className="section">
        <h2>Rotación</h2>
        <p className="muted">Productos más vendidos del período (por defecto, los últimos 30 días) y el punto de pedido que se sugiere para cada uno.</p>
        <div className="toolbar">
          <Field label="Desde"><input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} /></Field>
          <Field label="Hasta"><input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} /></Field>
          <Field label="Cantidad de productos">
            <input type="number" min="1" max="100" value={limite} onChange={(e) => setLimite(e.target.value)} />
          </Field>
        </div>
        <ErrorMsg error={rotacion.error} />
        <Table empty={!rotacion.loading && rotacion.data?.length === 0 ? 'No hubo ventas en el período.' : undefined}>
          <thead>
            <tr>
              <th>Código</th><th>Producto</th><th className="num">Unidades vendidas</th>
              <th className="num">Stock actual</th><th className="num">Pto. pedido</th><th className="num">Pto. pedido sugerido</th>
            </tr>
          </thead>
          <tbody>
            {rotacion.data?.map((r) => (
              <tr key={r.productoId}>
                <td>{r.codigo}</td>
                <td>{r.nombre}</td>
                <td className="num">{r.unidadesVendidas}</td>
                <td className="num">{r.stockTotal}</td>
                <td className="num">{r.puntoPedido}</td>
                <td className="num"><strong>{r.puntoPedidoSugerido}</strong></td>
              </tr>
            ))}
          </tbody>
        </Table>
      </section>

      <section className="section">
        <h2>Inventario</h2>
        <p className="muted">Stock de todos los productos activos por ubicación.</p>
        <div className="actions">
          {(['PDF', 'EXCEL'] as const).map((formato) => (
            <button key={formato} type="button" className="button" disabled={exportar.pending} onClick={() => void exportar.run(() => download('/reportes/inventario', { formato }))}>
              Descargar {formato === 'PDF' ? 'PDF' : 'Excel'}
            </button>
          ))}
        </div>
        <ErrorMsg error={exportar.error} />
      </section>
    </>
  )
}
