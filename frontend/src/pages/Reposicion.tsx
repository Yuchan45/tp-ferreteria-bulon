import { useState } from 'react'
import type { Alerta, Producto } from '../api/types'
import { Badge, ErrorMsg, EstadoBadge, Heading, Pager, Table } from '../components/ui'
import { fecha } from '../labels'
import { usePaged } from '../hooks/useAsync'

export default function Reposicion() {
  const productos = usePaged<Producto>('/productos', { reposicion: true, activo: true })
  const [abiertas, setAbiertas] = useState(true)
  // abiertas=false lista todas las alertas (abiertas y resueltas), no solo las resueltas
  const alertas = usePaged<Alerta>('/alertas', { abiertas })

  return (
    <>
      <Heading title="Reposición">Productos con stock en o por debajo del punto de pedido, y el historial de alertas.</Heading>

      <section className="section">
        <h2>Pendientes de reposición</h2>
        <ErrorMsg error={productos.error} />
        <Table empty={!productos.loading && productos.rows.length === 0 ? 'No hay productos pendientes de reposición.' : undefined}>
          <thead>
            <tr>
              <th>Código</th><th>Producto</th><th className="num">Stock total</th>
              <th className="num">Mínimo</th><th className="num">Pto. pedido</th><th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {productos.rows.map((p) => (
              <tr key={p.id}>
                <td className="code">{p.codigo}</td>
                <td>{p.nombre}</td>
                <td className="num"><strong>{p.stockTotal}</strong></td>
                <td className="num">{p.stockMinimo}</td>
                <td className="num">{p.puntoPedido}</td>
                <td><EstadoBadge estado={p.estadoStock} /></td>
              </tr>
            ))}
          </tbody>
        </Table>
        <Pager info={productos.info} onPage={productos.setPage} />
        <a href="#/compras">Generar órdenes de compra</a>
      </section>

      <section className="section">
        <h2>Alertas</h2>
        <div className="toolbar">
          <label className="check"><input type="radio" checked={abiertas} onChange={() => setAbiertas(true)} /> Abiertas</label>
          <label className="check"><input type="radio" checked={!abiertas} onChange={() => setAbiertas(false)} /> Todas</label>
        </div>
        <ErrorMsg error={alertas.error} />
        <Table empty={!alertas.loading && alertas.rows.length === 0 ? 'No hay alertas.' : undefined}>
          <thead>
            <tr><th>Fecha</th><th>Nivel</th><th>Detalle</th><th>Situación</th></tr>
          </thead>
          <tbody>
            {alertas.rows.map((a) => (
              <tr key={a.id}>
                <td>{fecha(a.fecha)}</td>
                <td><EstadoBadge estado={a.estado} /></td>
                <td>{a.mensaje}</td>
                <td>{a.resueltaEn ? <Badge clase="ok">Resuelta {fecha(a.resueltaEn)}</Badge> : <Badge clase="warn">Abierta</Badge>}</td>
              </tr>
            ))}
          </tbody>
        </Table>
        <Pager info={alertas.info} onPage={alertas.setPage} />
      </section>
    </>
  )
}
