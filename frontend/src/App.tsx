import { RouterProvider, createBrowserRouter } from 'react-router'
import { AppLayout, type RouteHandle } from '@/components/layout/AppLayout'
import { EstadoVacio } from '@/components/common'
import { MovimientosPage } from '@/pages/MovimientosPage'
import { PanelPage } from '@/pages/PanelPage'
import { ProductoDetallePage } from '@/pages/ProductoDetallePage'
import { ProductosPage } from '@/pages/ProductosPage'
import { ProveedoresPage } from '@/pages/ProveedoresPage'
import { ReponerPage } from '@/pages/ReponerPage'
import { UsuariosPage } from '@/pages/UsuariosPage'

const titulo = (title: string): RouteHandle => ({ title })

const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <PanelPage />, handle: titulo('Panel') },
      { path: 'productos', element: <ProductosPage />, handle: titulo('Productos') },
      { path: 'productos/:id', element: <ProductoDetallePage />, handle: titulo('Detalle de producto') },
      { path: 'reponer', element: <ReponerPage />, handle: titulo('A reponer') },
      { path: 'movimientos', element: <MovimientosPage />, handle: titulo('Movimientos') },
      { path: 'proveedores', element: <ProveedoresPage />, handle: titulo('Proveedores') },
      { path: 'usuarios', element: <UsuariosPage />, handle: titulo('Usuarios') },
      { path: '*', element: <EstadoVacio titulo="Página no encontrada" descripcion="La dirección no corresponde a ninguna vista." />, handle: titulo('No encontrado') },
    ],
  },
])

export default function App() {
  return <RouterProvider router={router} />
}
