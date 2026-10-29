import { RouterProvider, createBrowserRouter } from 'react-router'
import { AuthProvider } from '@/components/auth/AuthProvider'
import { RequireAdmin, RequireAuth, SoloInvitados } from '@/components/auth/guards'
import { EstadoVacio } from '@/components/common'
import { AppLayout, type RouteHandle } from '@/components/layout/AppLayout'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegistroPage } from '@/pages/auth/RegistroPage'
import { MovimientosPage } from '@/pages/MovimientosPage'
import { PanelPage } from '@/pages/PanelPage'
import { ProductoDetallePage } from '@/pages/ProductoDetallePage'
import { ProductosPage } from '@/pages/ProductosPage'
import { ProveedoresPage } from '@/pages/ProveedoresPage'
import { ReponerPage } from '@/pages/ReponerPage'
import { UsuariosPage } from '@/pages/UsuariosPage'

const titulo = (title: string): RouteHandle => ({ title })

const router = createBrowserRouter([
  // Públicas
  { path: '/login', element: <SoloInvitados><LoginPage /></SoloInvitados> },
  { path: '/registro', element: <SoloInvitados><RegistroPage /></SoloInvitados> },
  // Requieren sesión
  {
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <PanelPage />, handle: titulo('Panel') },
      { path: 'productos', element: <ProductosPage />, handle: titulo('Productos') },
      { path: 'productos/:id', element: <ProductoDetallePage />, handle: titulo('Detalle de producto') },
      { path: 'reponer', element: <ReponerPage />, handle: titulo('A reponer') },
      { path: 'movimientos', element: <MovimientosPage />, handle: titulo('Movimientos') },
      { path: 'proveedores', element: <ProveedoresPage />, handle: titulo('Proveedores') },
      // Solo administradores
      { path: 'usuarios', element: <RequireAdmin><UsuariosPage /></RequireAdmin>, handle: titulo('Usuarios') },
      { path: '*', element: <EstadoVacio titulo="Página no encontrada" descripcion="La dirección no corresponde a ninguna vista." />, handle: titulo('No encontrado') },
    ],
  },
])

export default function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  )
}
