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
                                │  Módulos: productos · movimientos ·      │
                                │           proveedores · usuarios         │
                                └──────────────────────────────────────────┘
                                               :8080
```

En desarrollo, Vite redirige las llamadas a `/api` hacia el backend, por lo que no hace falta configurar CORS.

## Estructura del repositorio

```
ferreteria-bulon/
├── backend/                     # API REST (Spring Boot)
│   ├── src/main/java/com/bulon/stock/
│   │   ├── productos/           # Catálogo y stock mínimo
│   │   ├── movimientos/         # Entradas, salidas y ajustes de stock
│   │   ├── proveedores/
│   │   ├── usuarios/            # Usuarios y roles
│   │   ├── common/exception/    # Manejo global de errores
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
cp .env.example .env      # completar DB_PASSWORD (por defecto: Bulon_Dev_2026!)
./mvnw spring-boot:run    # en Windows: .\mvnw.cmd spring-boot:run
```

- API en `http://localhost:8080/api`
- Estado del sistema: `http://localhost:8080/api/health`
- Con `SEED_ENABLED=true` en el `.env`, la primera vez carga datos de ejemplo: 3 usuarios,
  5 proveedores y 25 productos de ferretería.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev               # http://localhost:5173
```

El panel muestra si el sistema está conectado al backend y a la base de datos.

### Tests

```bash
cd backend && ./mvnw test           # usan H2 en memoria, no requieren Docker
cd frontend && npm run build && npm run lint
```

## Endpoints principales

| Método   | Ruta                           | Descripción                          |
|----------|--------------------------------|--------------------------------------|
| GET      | `/api/health`                  | Estado de la API y de la base        |
| GET/POST | `/api/productos`               | Listar / crear productos             |
| GET/PUT  | `/api/productos/{id}`          | Ver / actualizar un producto         |
| GET      | `/api/productos/bajo-stock`    | Productos que hay que reponer        |
| GET      | `/api/movimientos?productoId=` | Historial de movimientos             |
| POST     | `/api/movimientos`             | Registrar entrada, salida o ajuste   |
| GET/POST | `/api/proveedores`             | Listar / crear proveedores           |
| GET/POST | `/api/usuarios`                | Listar / crear usuarios              |

Listado completo y reglas de negocio en el [README del backend](backend/README.md).

## Patrones de diseño

Patrones definidos en la Entrega 2 ([informe](docs/informes/)) y su estado de implementación:

| Patrón         | Tipo           | Problema que resuelve                                  | Estado    |
|----------------|----------------|--------------------------------------------------------|-----------|
| Singleton      | Creacional     | Única instancia del pool de conexiones a SQL Server    | Pendiente |
| Factory Method | Creacional     | Creación de movimientos de stock según su tipo         | Pendiente |
| Strategy       | Comportamiento | Validación y efecto sobre el stock de cada movimiento  | Pendiente |
| Observer       | Comportamiento | Aviso de stock mínimo a distintos interesados          | Pendiente |
| Decorator      | Estructural    | Combinación de canales de notificación de alertas      | Pendiente |
| Adapter        | Estructural    | Exportación de reportes a PDF / Excel                  | Pendiente |

## Base de datos

- El esquema se versiona con **Flyway** en `backend/src/main/resources/db/migration`.
  Para cambiarlo se agrega un script nuevo (`V2__descripcion.sql`, …); nunca se edita uno ya aplicado.
- Hibernate solo **valida** que las entidades coincidan con el esquema (`ddl-auto: validate`).
- Para borrar todos los datos y empezar de cero: `docker compose down -v` y luego `docker compose up -d`.

## Documentación

- [`docs/informes/`](docs/informes/): informes de las entregas (relevamiento, arquitectura y patrones)
- [`backend/src/main/resources/db/dbdiagram/`](backend/src/main/resources/db/dbdiagram/): modelo de datos para [dbdiagram.io](https://dbdiagram.io)
- [`backend/src/main/resources/db/scripts/queries.sql`](backend/src/main/resources/db/scripts/queries.sql): consultas SQL de ejemplo
