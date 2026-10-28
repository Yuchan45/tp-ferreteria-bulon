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
├── api/              # Cliente HTTP y un módulo por recurso (productos, movimientos, ...)
├── hooks/            # useApi (carga + recarga tras cambios), useHealth, useMovimientosGlobales
├── lib/              # Formato (es-AR) y reglas de stock (estado, faltante, stock resultante)
├── components/
│   ├── ui/           # Componentes de shadcn/ui
│   ├── layout/       # AppLayout, Sidebar, Topbar
│   ├── dialogs/      # Registrar movimiento, alta/edición de producto, proveedor y usuario
│   ├── badges.tsx    # Estado de stock y tipo de movimiento
│   └── common.tsx    # KPI, tablas cargando, estados vacíos, paginación, avisos
├── pages/            # Una página por vista
├── App.tsx           # Rutas
└── index.css         # Tokens del design system
```

## Vistas

| Ruta              | Vista                | Datos |
|-------------------|----------------------|-------|
| `/`               | Panel                | Real |
| `/productos`      | Productos            | Real (filtros y paginación en el cliente) |
| `/productos/:id`  | Detalle de producto  | Real |
| `/reponer`        | A reponer            | Real (`GET /productos/bajo-stock`) |
| `/movimientos`    | Movimientos          | Real, ver nota |
| `/proveedores`    | Proveedores          | Real |
| `/usuarios`       | Usuarios             | Real |

"Registrar movimiento" se abre desde la barra superior y desde cada vista. Después de guardar
cualquier cambio, todas las vistas recargan sus datos.

**Notas sobre los datos**

- **Historial global:** el backend solo expone `GET /movimientos?productoId=`, así que la vista
  Movimientos (y los "Últimos movimientos" del panel) consulta el historial de cada producto y los
  combina. Cuando exista un `GET /movimientos` global alcanza con cambiar `useMovimientosGlobales`.
- **Stock resultante:** el backend no lo guarda; se reconstruye hacia atrás desde el stock actual.

## Solo datos reales

La interfaz muestra únicamente lo que provee el backend; no hay datos simulados.
Lo que todavía no existe en la API (login, permisos por rol) no aparece en pantalla.

## Estado de conexión

La barra superior consulta `GET /api/health` cada 15 segundos. Si el backend o la base no
responden, aparece un aviso con el botón "Reintentar".
