# Bulón S.R.L. — Frontend

Aplicación web interna del sistema de stock. React 19 + TypeScript + Vite, sin librerías de router, UI ni estado.

## Ejecutar

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # tsc + compila a dist/
npm run lint       # oxlint
```

El backend tiene que estar corriendo en `http://localhost:8080` (ver `../backend/README.md`; con
`SEED_ENABLED=true` crea los usuarios de abajo). Vite redirige `/api` al backend (`vite.config.ts`),
así que no hace falta CORS.

## Usuarios de prueba

| Usuario    | Contraseña    | Rol                | Puede                                                         |
|------------|---------------|--------------------|---------------------------------------------------------------|
| `admin`    | `admin123`    | Administrador      | Todo, incluida la gestión de usuarios                         |
| `deposito` | `deposito123` | Encargado depósito | Todo menos usuarios                                           |
| `ventas`   | `ventas123`   | Vendedor           | Consultar todo (menos usuarios); registrar ventas y devoluciones |

La interfaz oculta lo que el rol no puede hacer; el backend lo vuelve a validar.

## Estructura

```
src/
├── api/
│   ├── types.ts      # DTO del backend (uniones de strings para los enums)
│   ├── client.ts     # fetch con Basic auth, errores ProblemDetail -> ApiError, descargas
│   └── health.ts     # GET /api/health
├── hooks/            # useAsync / usePaged / useAction, useHealth
├── components/       # ui.tsx (tabla, paginador, selector de producto…), ConnectionStatus
├── pages/            # una pantalla por archivo
├── labels.ts         # textos de estados y tipos de movimiento
├── auth.tsx, session.ts   # sesión (credenciales en sessionStorage) y permisos por rol
├── App.tsx           # cabecera, navegación y router por hash (#/productos)
└── index.css         # estilos y colores
```

Cada request lleva `Authorization: Basic …`. Ante un 401 se cierra la sesión; los demás errores
muestran el `detail` que manda el backend.

## Pantallas

1. **Login**: valida con `GET /api/auth/me`.
2. **Panel**: indicadores, alertas abiertas, últimos movimientos y el estado de conexión.
3. **Productos**: búsqueda y filtros, stock por ubicación, estado; alta y edición (no vendedor) con categoría nueva y proveedores.
4. **Movimientos**: registro según tipo y rol, e historial con filtros, paginación y exportación PDF/Excel.
5. **Reposición**: productos pendientes y alertas (abiertas / todas).
6. **Órdenes de compra**: sugeridas por proveedor con cantidades editables, orden manual, enviar, recibir y cancelar.
7. **Reportes**: rotación por período y exportación del inventario.
8. **Proveedores**: ABM.
9. **Usuarios** (solo administrador): alta, edición, cambio de contraseña, activar y desactivar.
