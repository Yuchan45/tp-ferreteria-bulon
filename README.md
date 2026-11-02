# Ferretería Bulón — Sistema de gestión de stock

Sistema de gestión de stock para **Bulón S.R.L.**, una ferretería: catálogo de productos,
movimientos de stock (entradas, salidas y ajustes), proveedores, usuarios con roles y
alertas de reposición cuando un producto llega a su stock mínimo.

Trabajo práctico de **Proceso de Desarrollo de Software** — UADE · Grupo 6.

## Stack

| Capa          | Tecnología                                                   |
|---------------|--------------------------------------------------------------|
| Frontend      | React 19 + TypeScript + Vite                                 |
| Backend       | Java 21 + Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) |
| Reportes      | OpenPDF (PDF) y Apache POI (Excel)                           |
| Base de datos | SQL Server 2022 (Docker) + migraciones con Flyway            |
| Tests         | JUnit 5 + H2 en memoria                                      |

## Arquitectura

Aplicación cliente-servidor. El backend es un **monolito modular**: el código se organiza
por módulo de dominio y cada módulo sigue una arquitectura en capas.

```
┌──────────────┐   HTTP/JSON    ┌──────────────────────────────────────────┐   JDBC    ┌────────────┐
│   Frontend   │ ─────────────► │ Backend (Spring Boot)                    │ ────────► │ SQL Server │
│ React + Vite │   /api/...     │                                          │  (pool    │  (Docker)  │
│  :5173       │                │  Controller → Service → Repository (JPA) │  Hikari)  │   :1433    │
└──────────────┘                │                                          │           └────────────┘
                                │  Módulos: auth · sucursales · categorías │
                                │  productos · stock · movimientos ·       │
                                │  alertas · notificaciones · reportes ·   │
                                │  proveedores · usuarios                  │
                                └──────────────────────────────────────────┘
                                               :8080
```

En desarrollo, Vite redirige las llamadas a `/api` hacia el backend, por lo que no hace falta configurar CORS.

## Estructura del repositorio

```
ferreteria-bulon/
├── backend/                     # API REST (Spring Boot)
│   ├── src/main/java/com/bulon/stock/
│   │   ├── sucursales/          # Sucursales con stock
│   │   ├── categorias/
│   │   ├── productos/           # Catálogo y proveedores de cada producto
│   │   ├── stock/               # Stock por sucursal y reposición
│   │   ├── alertas/             # Alertas de reposición (observan el stock)
│   │   ├── notificaciones/      # Avisos in-app (observan las alertas)
│   │   ├── reportes/            # Reportes en PDF y Excel
│   │   ├── movimientos/         # Compras, ventas, devoluciones, roturas y ajustes
│   │   ├── proveedores/
│   │   ├── usuarios/            # Usuarios y roles
│   │   ├── common/              # Errores globales, auditoría, contraseñas
│   │   └── seed/                # Datos de ejemplo para desarrollo
│   └── src/main/resources/
│       ├── application.yml
│       └── db/
│           ├── migration/       # Migraciones Flyway (esquema de la base)
│           ├── scripts/         # Consultas SQL de uso manual
│           └── dbdiagram/       # Modelo de datos en DBML (dbdiagram.io)
├── frontend/                    # Interfaz web (React)
├── docker/sqlserver/init.sql    # Crea la base bulon_stock
├── docker-compose.yml           # SQL Server para desarrollo
└── docs/informes/               # Informes de cada entrega
```

Cada parte tiene su propio README con más detalle: [backend](backend/README.md) · [frontend](frontend/README.md).

## Requisitos

- **Docker Desktop**, para la base de datos
- **JDK 21 o superior**. El proyecto compila para Java 21; Maven se incluye con el wrapper `mvnw`.
- **Node.js 20.19+ o 22.12+**

## Puesta en marcha

### 1. Base de datos

Desde la raíz del repositorio:

```bash
docker compose up -d
```

Levanta SQL Server en `localhost:1433` y crea la base `bulon_stock`. Las tablas las crea el backend al iniciar.

### 2. Backend

```bash
cd backend
cp .env.example .env      # completar DB_PASSWORD (por defecto: Bulon_Dev_2026!) y JWT_SECRET
./mvnw spring-boot:run    # en Windows: .\mvnw.cmd spring-boot:run
```

- API en `http://localhost:8080/api`
- Estado del sistema: `http://localhost:8080/api/health`
- Con `SEED_ENABLED=true` en el `.env`, la primera vez carga datos de ejemplo: 1 sucursal,
  3 usuarios (`admin` / `admin` y empleados con contraseña `empleado`), 6 proveedores y 27 productos de ferretería.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev               # http://localhost:5173
```

Se entra con `admin` / `admin` (administrador) o `ventas` / `empleado` (empleado), o
creando una cuenta en `/registro`, donde se elige el rol (por defecto administrador; el empleado
elige su sucursal). El menú y los botones de alta se
adaptan al rol, pero la seguridad real la aplica el backend.

Si el backend corre en otro puerto: `BACKEND_URL=http://localhost:8081 npm run dev`.

### Tests

```bash
cd backend && ./mvnw test           # usan H2 en memoria, no requieren Docker
cd frontend && npm run build && npm run lint
```

## Endpoints principales

| Método   | Ruta                           | Descripción                          |
|----------|--------------------------------|--------------------------------------|
| GET      | `/api/health`                  | Estado de la API y de la base        |
| POST     | `/api/auth/login`, `/api/auth/registro` | Iniciar sesión / crear cuenta (devuelven un JWT) |
| GET      | `/api/sucursales`              | Sucursales                           |
| GET/POST | `/api/productos`               | Listar / crear productos             |
| GET/PUT  | `/api/productos/{id}`          | Ver / actualizar un producto         |
| GET      | `/api/productos/{id}/stock`    | Stock del producto por sucursal      |
| GET      | `/api/productos/bajo-stock`    | Productos que hay que reponer        |
| GET      | `/api/alertas`                 | Alertas de reposición activas        |
| GET/PUT  | `/api/notificaciones`          | Notificaciones del usuario / marcarlas como leídas |
| GET      | `/api/reportes/{stock,movimientos,a-reponer}` | Descargar un reporte en PDF o Excel |
| GET      | `/api/movimientos?sucursalId=` | Historial (de una sucursal o un producto, con filtros y stock resultante) |
| POST     | `/api/movimientos`             | Registrar compra, venta, devolución, rotura o ajuste |
| GET/POST/PUT | `/api/proveedores`         | Listar / crear / editar o desactivar proveedores |
| GET/POST/PUT | `/api/usuarios`            | Listar / crear / editar o desactivar usuarios |

Salvo `health`, `auth` y `sucursales`, todos exigen token; las altas y cambios de catálogo,
proveedores y usuarios son solo para `ADMINISTRADOR`. Listado completo, permisos por endpoint y
reglas de negocio en el [README del backend](backend/README.md).

## Patrones de diseño

Patrones definidos en la Entrega 2 ([informe](docs/informes/)) y su estado de implementación:

| Patrón         | Tipo           | Problema que resuelve                                  | Estado    |
|----------------|----------------|--------------------------------------------------------|-----------|
| Singleton      | Creacional     | Única instancia del pool de conexiones a SQL Server    | Implementado por Spring (un solo pool HikariCP y beans singleton; ver abajo) |
| Simple Factory | Creacional     | Elegir la estrategia y crear el movimiento según su tipo | Implementado (`movimientos/estrategias/FabricaMovimientos`) |
| Strategy       | Comportamiento | Validación y efecto sobre el stock de cada movimiento  | Implementado (`movimientos/estrategias/`, una clase por tipo) |
| Observer       | Comportamiento | Aviso de stock mínimo a distintos interesados          | Implementado (`stock/StockObserver` → `alertas/` → `notificaciones/`) |
| Decorator      | Estructural    | Combinación de canales de notificación de alertas      | Implementado (`notificaciones/canales/`: campanita + email según el nivel) |
| Adapter        | Estructural    | Exportación de reportes a PDF / Excel                  | Implementado (`reportes/`: `ExportadorReporte` + adaptadores de OpenPDF y Apache POI) |

### Singleton: lo resuelve Spring

El problema era tener **una sola instancia del pool de conexiones** a SQL Server, compartida por
todos los repositorios. En Spring Boot no se escribe un Singleton clásico (constructor privado +
`getInstance()`): el contenedor de Spring ya crea **un único `DataSource`** (el pool de HikariCP,
configurado en `application.yml`) y lo inyecta donde haga falta. Lo mismo pasa con todos los
`@Service`, `@Repository` y `@Component`: el alcance por defecto de un bean es *singleton*.

Escribirlo a mano sería peor: un `getInstance()` estático no se puede reemplazar en los tests y
esquiva la inyección de dependencias. `SingletonTests` comprueba que hay un solo `DataSource`,
que es el pool de HikariCP y que los servicios son la misma instancia en toda la aplicación.

### Fuera de alcance

Las **solicitudes de reposición a proveedores** (pedido → enviado → recibido → ingreso automático),
que el modelo v2 planteaba con los patrones **State** (ciclo de vida de la solicitud) y **Strategy**
(elección del proveedor), quedan fuera del alcance del TP por tiempo y porque no forman parte de
lo comprometido en la Entrega 2. La migración `V3__quitar_solicitudes_reposicion.sql` borró lo
que la V2 había creado para ellas: las tablas `solicitudes_reposicion` y `solicitud_items`, las
columnas `solicitud_id` de alertas y movimientos y el estado `EN_GESTION` de las alertas (que
ahora solo pueden estar `ABIERTA` o `RESUELTA`). La reposición se registra como un ingreso por
compra, que el botón "Registrar compra" de la vista A reponer precarga con el faltante y el
proveedor preferido.

También quedan afuera, por tiempo:

- **Crear categorías desde la interfaz**: el endpoint `POST /api/categorias` existe (solo
  administrador), pero no hay pantalla; las categorías son las que carga el seeder.
- **Alta de sucursales y traslados entre ellas**: el modelo ya guarda el stock por sucursal
  (preparado para N), pero hoy hay una sola sucursal y no hay ABM ni un tipo de movimiento de
  traslado. Sumarlo sería una estrategia más (Strategy) y una pantalla de sucursales.

## Base de datos

- El esquema se versiona con **Flyway** en `backend/src/main/resources/db/migration`.
  Hay tres: V1 (esquema inicial), V2 (sucursales, categorías, alertas y notificaciones) y V3 (quita las solicitudes de reposición, fuera de alcance). Para cambiarlo se agrega un script nuevo (`V4__descripcion.sql`, …); nunca se edita uno ya aplicado.
- Hibernate solo **valida** que las entidades coincidan con el esquema (`ddl-auto: validate`).
- Para borrar todos los datos y empezar de cero: `docker compose down -v` y luego `docker compose up -d`.

## Documentación

- [`docs/informes/`](docs/informes/): informes de las entregas (relevamiento, arquitectura y patrones)
- [`backend/src/main/resources/db/dbdiagram/`](backend/src/main/resources/db/dbdiagram/): modelo de datos para [dbdiagram.io](https://dbdiagram.io)
- [`backend/src/main/resources/db/scripts/queries.sql`](backend/src/main/resources/db/scripts/queries.sql): consultas SQL de ejemplo
