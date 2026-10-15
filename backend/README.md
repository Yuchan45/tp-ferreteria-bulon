# Bulón S.R.L. — Backend de gestión de stock

API REST en Java 21 + Spring Boot 3, persistencia en SQL Server vía JPA.
Monolito modular con arquitectura en capas (Controller → Service → Repository).

## Estructura

```
src/main/java/com/bulon/stock/
├── StockApplication.java
├── common/exception/      # Excepciones y manejo global de errores
├── productos/             # Catálogo, stock mínimo, consulta de bajo stock
├── movimientos/           # Entradas, salidas y ajustes de stock
├── proveedores/           # Alta y consulta de proveedores
└── usuarios/              # Usuarios y roles
```

Cada módulo agrupa su entidad, repositorio, servicio, controlador y DTOs.
Un módulo solo accede a otro a través de su `Service`, nunca a su `Repository`.

## Base de datos

SQL Server 2022 corre en Docker (`docker-compose.yml` en la raíz del repositorio):

```bash
docker compose up -d        # desde la raíz: levanta SQL Server y crea la base bulon_stock
docker compose down         # detiene (los datos se conservan en el volumen)
docker compose down -v      # detiene y BORRA los datos
```

El esquema lo manejan las migraciones de **Flyway** en `src/main/resources/db/migration`,
que se aplican solas al iniciar el backend. Hibernate solo valida que las entidades coincidan
(`ddl-auto: validate`). Para cambiar el modelo se agrega un script nuevo (`V2__descripcion.sql`, ...);
nunca se modifica uno ya aplicado.

## Configuración

Las credenciales de la base se leen de `backend/.env`, que **no se sube al repositorio**.
La primera vez:

```bash
cp .env.example .env        # desde backend/, y completar DB_PASSWORD
```

| Variable      | Ejemplo                                                       |
|---------------|---------------------------------------------------------------|
| `DB_URL`      | `jdbc:sqlserver://localhost:1433;databaseName=bulon_stock;...` |
| `DB_USER`     | `sa`                                                          |
| `DB_PASSWORD` | la misma que `MSSQL_SA_PASSWORD` de `docker-compose.yml` (por defecto `Bulon_Dev_2026!`) |

Las variables de entorno del sistema tienen prioridad sobre el `.env`.
Si falta el archivo, el backend no arranca y muestra un error como
`Driver ... claims to not accept jdbcUrl, ${DB_URL}`.

## Ejecutar

```bash
docker compose up -d        # 1. desde la raíz del repo
./mvnw spring-boot:run      # 2. desde backend/ — API en http://localhost:8080/api
./mvnw test                 # Tests (usan H2 en memoria, no requieren SQL Server)
```

Para conectarse con un cliente (Azure Data Studio, DBeaver, SSMS): `localhost,1433`, usuario `sa`.
Si se usa `sqlcmd`, agregar `-I` (lo requiere el índice filtrado de `proveedores.cuit`).

## Endpoints iniciales

| Método | Ruta                          | Descripción                     |
|--------|-------------------------------|---------------------------------|
| GET    | `/api/health`                 | Estado de la API y de la base (200 `UP` / 503 `DOWN`) |
| GET    | `/api/productos`              | Listar productos                |
| GET    | `/api/productos/{id}`         | Obtener producto                |
| GET    | `/api/productos/bajo-stock`   | Productos a reponer             |
| POST   | `/api/productos`              | Crear producto                  |
| PUT    | `/api/productos/{id}`         | Actualizar producto             |
| GET    | `/api/movimientos?productoId=`| Historial de un producto        |
| POST   | `/api/movimientos`            | Registrar entrada/salida/ajuste |
| GET/POST | `/api/proveedores`          | Listar / crear proveedores      |
| GET/POST | `/api/usuarios`             | Listar / crear usuarios         |
