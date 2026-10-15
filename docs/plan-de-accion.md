# Plan de acción — Sistema de Gestión de Stock Bulón

Estado al 2026-10-15. Compara el código del repo con la definición del sistema
([Entrega 1](informes/Grupo%206%20-%20Entrega%201%20-%20Ferreter%C3%ADa%20Bul%C3%B3n.pdf))
y con los patrones comprometidos en la
[Entrega 2](informes/Grupo%206%20-%20Entrega%202%20-%20Ferreter%C3%ADa%20Bul%C3%B3n.pdf).

## 1. Qué está hecho

| Área | Estado | Detalle |
|---|---|---|
| Infra | ✅ | Docker con SQL Server (`platform: linux/amd64`, corre con Rosetta en Apple Silicon), Flyway V1, Spring Boot 3.5 / Java 21, `/api/health`, `GlobalExceptionHandler` con `ProblemDetail` |
| Productos | 🟡 | Listar, obtener, crear, actualizar, `GET /productos/bajo-stock`. Campos: `codigo`, `descripcion`, `precio`, `stockActual`, `stockMinimo`, un solo `proveedor` |
| Movimientos | 🟡 | Tipos `ENTRADA` / `SALIDA` / `AJUSTE`, stock único por producto, listado por producto. Lógica en un `switch` en `MovimientoStockService` |
| Proveedores | 🟡 | Listar, obtener, crear. Sin update ni baja; el controller recibe y devuelve la entidad JPA, sin `@Valid` |
| Usuarios | 🟡 | Entidad + enum `Rol` (los 3 roles), listar, obtener, crear. Sin autenticación ni permisos |
| Tests | 🟡 | `contextLoads` + health, sobre H2 con `ddl-auto: create-drop` (no corre las migraciones de Flyway) |
| Frontend | ❌ | No existe |

Cambios sin commitear en el working tree: `docker-compose.yml` y `backend/README.md` (fix de amd64 para macOS).

## 2. Qué falta según la definición

### Modelo de datos (el cambio de mayor impacto)

- **Producto (4.1):** faltan `nombre`, `categoria`, `unidad de medida`, `punto de pedido`, `activo/inactivo`.
- **Ubicaciones (4.1):** no existen. El stock tiene que salir de `productos.stock_actual` y pasar a
  `existencias (producto_id, ubicacion_id, cantidad)` para separar sucursal y depósito.
- **Producto ↔ Proveedor (4.3):** hoy es 1:N (`productos.proveedor_id`). Debe ser N:M con un proveedor preferido.

### Movimientos (4.2)

- Faltan 3 tipos: hoy hay 3, se piden 6 → ingreso por compra, salida por venta, devolución, rotura/pérdida, ajuste manual, traslado.
- Campos faltantes: **usuario responsable**, **ubicación** (origen y destino en traslado), **motivo** (obligatorio en ajustes), observaciones.
- Traslado atómico: resta en origen y suma en destino en la misma transacción.
- Factory Method + Strategy (Entrega 2, 3.2 y 3.3) no implementados: reemplazar el `switch`.
- Concurrencia: dos salidas simultáneas pueden dejar el stock inconsistente → `@Version` (bloqueo optimista) en la existencia.
- Movimientos inmutables: sin update ni delete; las correcciones se hacen con un ajuste.
- Historial/auditoría: hoy solo filtra por producto. Faltan filtros por fecha, tipo, usuario y ubicación, y paginación (~2.800 productos).

### Usuarios y roles (4.4)

- Spring Security, password (hash), login.
- El usuario responsable del movimiento sale de la sesión, no del body del request.
- Permisos por rol (Administrador, Encargado de depósito, Vendedor).

### Alertas (4.5 + Observer/Decorator de la Entrega 2)

- Distinguir stock mínimo de punto de pedido; marcar "pendiente de reposición".
- Observer (3.4) y Decorator (3.5) no implementados. Hay un `TODO` en
  `backend/src/main/java/com/bulon/stock/movimientos/MovimientoStockService.java:47`.

### Detalles para arreglar de paso

- `ProductoService.actualizar` no valida unicidad del código → un duplicado termina en 500 en vez de 422.
- Falta CORS para el frontend, datos semilla (seed) y Swagger/OpenAPI.

### Nota de diseño: Singleton (Entrega 2, 3.1)

No escribir un `DatabaseConnectionManager` a mano: el `DataSource` (pool HikariCP) ya es un bean singleton
administrado por Spring, y una clase propia saltearía JPA y Flyway. Justificarlo así en el informe.

## 3. Orden de ejecución sugerido

```
                     ┌─────────────────────────────────┐
                     │ F0 · Ajustes de base (1-2 días)  │
                     │ DTOs + @Valid en Proveedor/Usuario│
                     │ CORS, seed, Swagger, fix código  │
                     └────────────────┬────────────────┘
                 ┌────────────────────┴────────────────────┐
                 ▼                                         ▼
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│ F1 · Modelo de dominio (V2 SQL)  │     │ F2 · Autenticación               │
│ Ubicacion, Categoria             │     │ Spring Security + password       │
│ Producto completo (activo, etc.) │     │ login, usuario autenticado       │
│ Existencia (producto×ubicación)  │     │ (en paralelo con F1)             │
│ Producto↔Proveedor N:M+preferido │     │                                  │
└────────────────┬────────────────┘     └────────────────┬────────────────┘
                 └────────────────────┬────────────────────┘
                                      ▼
                     ┌─────────────────────────────────┐
                     │ F3 · Motor de movimientos        │
                     │ 6 tipos · Factory Method+Strategy│
                     │ traslado atómico · motivo        │
                     │ usuario de sesión · @Version     │
                     │ movimientos inmutables           │
                     └────────────────┬────────────────┘
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
┌───────────────────┐   ┌───────────────────────────┐   ┌───────────────────┐
│ F4 · Consultas     │   │ F5 · Alertas               │   │ F6 · Permisos rol │
│ stock por ubicación│   │ Observer + Decorator       │   │ Admin / Encargado │
│ historial+filtros  │   │ pendiente de reposición    │   │ / Vendedor        │
│ paginación         │   │ stock mín. vs pto. pedido  │   │                   │
└─────────┬─────────┘   └─────────────┬─────────────┘   └─────────┬─────────┘
          └───────────────────────────┼───────────────────────────┘
                                      ▼
                     ┌─────────────────────────────────┐
                     │ F7 · Tentativas                  │
                     │ Dashboard · Reportes (Adapter)   │
                     │ Rotación · Órdenes de compra     │
                     └─────────────────────────────────┘

FRONTEND (React + TS), en paralelo apenas se cierre el contrato de la API en F1:
  setup + login ──► productos/proveedores ──► stock por ubicación ──► registrar movimiento
                    (F1)                      (F4)                    (F3)
              ──► historial ──► alertas de reposición ──► dashboard
                  (F4)          (F5)                      (F7)
```

### Por qué este orden

1. **F1 primero:** pasar el stock a existencias por ubicación cambia las tablas sobre las que se apoya todo.
   Hacerlo después obliga a reescribir el motor de movimientos dos veces.
2. **F2 en paralelo con F1:** no comparten tablas, pero tiene que estar lista antes de F3 porque cada
   movimiento necesita un usuario responsable.
3. **F3 es el núcleo** y donde se demuestran Factory Method + Strategy ante la cátedra. Va con tests de
   cada estrategia contra el comportamiento observable (stock resultante, errores), no contra la implementación.
4. **F4, F5 y F6** dependen de F3 pero no entre sí: se pueden repartir entre integrantes.
5. **F7** es lo marcado como tentativo en la Entrega 1; el Adapter de reportes entra recién ahí.

### Criterio de "terminado" por fase

- Migración Flyway nueva (`V2__...`, `V3__...`): nunca editar `V1`.
- `./mvnw test` en verde.
- Probado contra SQL Server real (`docker compose up -d` + backend levantado + requests a la API),
  no solo contra H2.
- Un PR chico por fase o subfase.

## 4. Cómo retomar con pstack

pstack está instalado como plugin en este proyecto (scope `local`, en `.claude/settings.local.json`, fuera de git).
Después de reiniciar la sesión:

```text
/pstack:poteto-mode leé docs/plan-de-accion.md y arrancá la fase F0
```

Prompts sugeridos por fase:

| Fase | Prompt |
|---|---|
| F0 | `/pstack:poteto-mode fase F0 de docs/plan-de-accion.md: DTOs y validación en Proveedor/Usuario, fix de código duplicado en ProductoService.actualizar, CORS, seed y Swagger` |
| F1 | `/pstack:architect fase F1 de docs/plan-de-accion.md: ubicaciones, existencias por ubicación, producto completo y Producto↔Proveedor N:M, como migración V2` |
| F2 | `/pstack:poteto-mode fase F2: Spring Security con login y usuario autenticado disponible para los servicios` |
| F3 | `/pstack:architect fase F3: motor de movimientos con Factory Method + Strategy según la Entrega 2, traslado atómico y @Version` |
| F5 | `/pstack:poteto-mode fase F5: alertas de reposición con Observer + Decorator según la Entrega 2` |

Antes de mergear cambios grandes (sobre todo F1): `/pstack:blast-radius` y `/pstack:interrogate`.
