# Bulón S.R.L. — Frontend

React 19 + TypeScript + Vite, con Tailwind CSS 4 y componentes de shadcn/ui (Radix) e íconos lucide.
El diseño sigue el mockup de Penpot (design system "industrial limpia", solo modo claro).

## Ejecutar

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # compila a dist/
npm run lint
```

El backend tiene que estar corriendo en `http://localhost:8080` (ver `../backend/README.md`).
En desarrollo, Vite redirige las llamadas a `/api` al backend (proxy en `vite.config.ts`),
por lo que no hace falta configurar CORS.

## Estructura

```
src/
├── api/              # Cliente HTTP (con el token de la sesión) y un módulo por recurso
├── hooks/            # useApi (carga + recarga tras cambios), sesión, useHealth,
│                     # useMovimientosGlobales, useDescargaReporte
├── lib/              # Formato (es-AR) y reglas de stock (estado, faltante, stock resultante)
├── components/
│   ├── ui/           # Componentes de shadcn/ui
│   ├── auth/         # AuthProvider y guards de rutas (sesión iniciada, solo administrador)
│   ├── layout/       # AppLayout, Sidebar (con el rol del usuario), Topbar, Notificaciones
│   ├── dialogs/      # Registrar movimiento, alta/edición de producto, proveedor y usuario
│   ├── ProductoSelector.tsx  # Buscador de productos del modal de movimientos
│   ├── FiltroEstado.tsx      # Filtro por estado de stock con pastillas de color
│   ├── ExportarReporte.tsx   # Botón "Exportar" (PDF / Excel) de las vistas
│   ├── badges.tsx    # Estado de stock, tipo de movimiento y su filtro
│   └── common.tsx    # KPI, tablas cargando, estados vacíos, paginación, avisos
├── pages/            # Una página por vista (auth/: login y registro)
├── App.tsx           # Rutas
└── index.css         # Tokens del design system
```

## Vistas

| Ruta              | Vista                | Acceso | Notas |
|-------------------|----------------------|--------|-------|
| `/login`, `/registro` | Iniciar sesión / crear cuenta | Público | En el registro se elige el rol (por defecto administrador) |
| `/`               | Panel                | Todos | KPIs, alertas de reposición y últimos movimientos |
| `/productos`      | Productos            | Todos | Filtros y paginación en el cliente; alta solo para administradores |
| `/productos/:id`  | Detalle de producto  | Todos | Stock, umbrales, proveedor preferido e historial |
| `/reponer`        | A reponer            | Todos | `GET /productos/bajo-stock`; "Registrar compra" precarga el faltante |
| `/movimientos`    | Movimientos          | Todos | Historial global, ver nota |
| `/proveedores`    | Proveedores          | Todos | Alta solo para administradores |
| `/reportes`       | Reportes             | Todos | Stock, movimientos y a reponer en PDF o Excel |
| `/usuarios`       | Usuarios             | Administrador | |

"Registrar movimiento" se abre desde la barra superior y desde cada vista; el producto se elige
con un buscador (por código o nombre) y un filtro por estado. Después de guardar cualquier
cambio, todas las vistas recargan sus datos. Productos, Movimientos y A reponer tienen además un
botón **Exportar** que descarga el reporte con los filtros que se están viendo.

La **campanita** de la barra superior muestra las notificaciones del usuario (se abren al
generarse una alerta de reposición); se consulta cada 30 segundos y después de cada movimiento.

**Notas sobre los datos**

- **Historial global:** el backend solo expone `GET /movimientos?productoId=`, así que la vista
  Movimientos (y los "Últimos movimientos" del panel) consulta el historial de cada producto y los
  combina. Cuando exista un `GET /movimientos` global alcanza con cambiar `useMovimientosGlobales`.
- **Stock resultante:** el backend no lo guarda; se reconstruye hacia atrás desde el stock actual.

## Solo datos reales

La interfaz muestra únicamente lo que provee el backend; no hay datos simulados.

## Sesión y permisos

El login guarda el JWT en `localStorage` y el cliente lo envía en cada pedido. El menú y los
botones de alta se adaptan al rol, y las rutas de administrador están protegidas, pero la
seguridad real la aplica el backend. Si el token vence, se cierra la sesión.

## Estado de conexión

La app consulta `GET /api/health` cada 15 segundos. Si el backend o la base no responden,
aparece un aviso arriba del contenido con el botón "Reintentar".
