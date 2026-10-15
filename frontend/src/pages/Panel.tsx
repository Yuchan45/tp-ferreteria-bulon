import { api } from '../api/client'
import type { Alerta, Dashboard, Page } from '../api/types'
import { ConnectionStatus } from '../components/ConnectionStatus'
import { Badge, ErrorMsg, EstadoBadge, Heading, Table } from '../components/ui'
import { TIPOS, fecha } from '../labels'
import { useAsync } from '../hooks/useAsync'

export default function Panel() {
  const dash = useAsync(() => api.get<Dashboard>('/dashboard'), 'dashboard')
  const alertas = useAsync(() => api.get<Page<Alerta>>('/alertas', { abiertas: true, size: 6 }), 'alertas')
  const d = dash.data

  return (
    <>
      <Heading title="Panel general">Resumen del stock y la actividad de hoy.</Heading>
      <ErrorMsg error={dash.error ?? alertas.error} />

      {d && (
        <>
          <div className="kpis">
            <Kpi label="Productos activos" value={d.productosActivos} />
            <Kpi label="Pendientes de reposición" value={d.pendientesReposicion} tono="warn" />
            <Kpi label="Bajo mínimo" value={d.bajoMinimo} tono="error" />
            <Kpi label="Movimientos hoy" value={d.movimientosHoy} />
            {d.unidadesPorUbicacion.map((u) => (
              <Kpi key={u.ubicacionId} label={`Unidades en ${u.ubicacion}`} value={u.unidades} />
            ))}
          </div>

          <div className="grid-2">
            <section className="card section">
              <h2>Alertas abiertas ({d.alertasAbiertas})</h2>
              <ul className="list">
                {alertas.data?.content.map((a) => (
                  <li key={a.id}>
                    <EstadoBadge estado={a.estado} /> <span>{a.mensaje}</span>
                  </li>
                ))}
                {alertas.data?.content.length === 0 && <li className="muted">No hay alertas abiertas.</li>}
              </ul>
              <a href="#/reposicion">Ver reposición</a>
            </section>

            <section className="card section">
              <h2>Últimos movimientos</h2>
              <Table>
                <tbody>
                  {d.ultimosMovimientos.map((m) => (
                    <tr key={m.id}>
                      <td>{fecha(m.fecha)}</td>
                      <td><Badge clase="pending">{TIPOS[m.tipo]}</Badge></td>
                      <td className="code">{m.productoCodigo}</td>
                      <td className="num">{m.cantidad}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
              <a href="#/movimientos">Ver historial</a>
            </section>
          </div>
        </>
      )}

      <ConnectionStatus />
    </>
  )
}

function Kpi({ label, value, tono }: { label: string; value: number; tono?: 'warn' | 'error' }) {
  return (
    <div className={`card kpi${tono ? ` kpi--${tono}` : ''}`}>
      <strong>{value.toLocaleString('es-AR')}</strong>
      <span>{label}</span>
    </div>
  )
}
