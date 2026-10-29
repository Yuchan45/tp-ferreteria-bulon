# Bulón S.R.L. — Backend de gestión de stock

API REST en Java 21 + Spring Boot 3, persistencia en SQL Server vía JPA.
Monolito modular con arquitectura en capas (Controller → Service → Repository).

## Estructura

```
src/main/java/com/bulon/stock/
├── StockApplication.java
├── auth/                  # Login, registro, JWT y reglas de seguridad (SecurityConfig)
├── common/                # Errores globales y auditoría
├── sucursales/            # Sucursales (ubicaciones con stock)
├── categorias/            # Categorías de productos
├── productos/             # Catálogo y proveedores de cada producto (N:M, con preferido)
├── stock/                 # Stock por producto y sucursal, umbrales y productos a reponer
├── movimientos/           # Compras, ventas, devoluciones, roturas y ajustes
├── proveedores/           # Alta y consulta de proveedores
├── usuarios/              # Usuarios (ADMINISTRADOR / EMPLEADO de una sucursal)
└── seed/                  # Datos de ejemplo para desarrollo
```

El modelo de datos está en `db/dbdiagram/v2.txt` (v1.txt es el esquema anterior).

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
| `SEED_ENABLED`| `true` carga datos de ejemplo de la ferretería si la base está vacía (default `false`) |
| `JWT_SECRET`  | clave para firmar los tokens, mínimo 32 caracteres. Generarla con `node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"` |

El seeder (`seed/DataSeeder.java`) carga 1 sucursal, 3 usuarios (`admin`, `encargado`, `ventas`;
contraseña de desarrollo `bulon1234`), 5 categorías, 6 proveedores y 27 productos (dos se venden
por metro y por kilo). El stock inicial se registra como compras al proveedor preferido y hay un
movimiento de ejemplo de cada tipo. Algunos productos quedan en o por debajo del punto de pedido
para probar las alertas. Solo carga en tablas vacías; para volver a cargarlo desde cero:
`docker compose down -v`.

Al migrar una base v1 con datos, la V2 crea la sucursal `SUC-01` y le asigna todo el stock, pasa
el proveedor de cada producto a preferido, convierte los roles viejos en `EMPLEADO` y asigna los
movimientos históricos a un administrador. Los usuarios existentes quedan con la contraseña `bulon1234`.

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
Si se usa `sqlcmd`, agregar `-I` (lo requieren los índices filtrados).
Consultas de ejemplo en `db/scripts/queries.sql`.

## Autenticación y roles

La API es *stateless*: `POST /api/auth/login` devuelve un **JWT** (válido 8 h) que el cliente
envía en cada pedido como `Authorization: Bearer <token>`. El token lleva el id del usuario, su
rol y su sucursal; el servidor lo valida con la firma (HS256 + `JWT_SECRET`), sin guardar sesiones.

La protección tiene tres capas:

1. **URL** (`auth/SecurityConfig`): `/health`, `/auth/login`, `/auth/registro` y `GET /sucursales`
   son públicos; todo lo demás exige token → si falta o es inválido, `401`.
2. **Rol** (`@PreAuthorize("hasRole('ADMINISTRADOR')")` en controladores): el claim `rol` del
   token se convierte en la autoridad `ROLE_<rol>`. Si el rol no alcanza → `403`.
3. **Datos** (servicios): un `EMPLEADO` solo opera su sucursal y el responsable de cada
   movimiento sale del token, no del cuerpo del pedido.

El registro público siempre crea un `EMPLEADO` activo de la sucursal elegida (aunque se envíe
otro rol). Los administradores se crean desde `POST /api/usuarios`.

## Endpoints

🔓 público · 👤 cualquier usuario logueado · 🔑 solo `ADMINISTRADOR`

| Método | Ruta                                     | Acceso | Descripción |
|--------|------------------------------------------|--------|-------------|
| GET    | `/api/health`                            | 🔓 | Estado de la API y de la base (200 `UP` / 503 `DOWN`) |
| POST   | `/api/auth/login`                        | 🔓 | `{username, password}` → `{token, expira, usuario}` |
| POST   | `/api/auth/registro`                     | 🔓 | Alta como `EMPLEADO`, devuelve la sesión iniciada (201) |
| GET    | `/api/auth/yo`                           | 👤 | Usuario del token |
| GET    | `/api/sucursales`, `/api/sucursales/{id}`| 🔓 | Sucursales |
| GET / POST | `/api/categorias`                    | 👤 / 🔑 | Listar / crear categorías |
| GET / POST | `/api/productos`                     | 👤 / 🔑 | Listar (con `stockTotal`) / crear producto (crea su stock en 0 en cada sucursal) |
| GET / PUT  | `/api/productos/{id}`                | 👤 / 🔑 | Ver / actualizar producto |
| GET    | `/api/productos/{id}/stock`              | 👤 | Stock del producto en cada sucursal, con su estado |
| PUT    | `/api/productos/{id}/stock/{sucursalId}` | 🔑 | Configurar `stockMinimo` y `puntoPedido` |
| GET    | `/api/productos/{id}/proveedores`        | 👤 | Proveedores del producto (el preferido primero) |
| PUT    | `/api/productos/{id}/proveedores/{proveedorId}` | 🔑 | Asociar proveedor o cambiar sus condiciones |
| GET    | `/api/productos/bajo-stock?sucursalId=`  | 👤 | En o por debajo del punto de pedido (`sucursalId` opcional) |
| GET    | `/api/stock?sucursalId=`                 | 👤 | Stock de todos los productos en una sucursal |
| GET    | `/api/movimientos?productoId=&sucursalId=` | 👤 | Historial de un producto (`sucursalId` opcional) |
| POST   | `/api/movimientos`                       | 👤 | Registrar un movimiento |
| GET / POST | `/api/proveedores`                   | 👤 / 🔑 | Listar / crear proveedores |
| GET/POST | `/api/usuarios`                        | 🔑 | Listar / crear usuarios (la API nunca devuelve la contraseña) |

### Reglas al registrar un movimiento

- Tipos: `INGRESO_COMPRA` y `DEVOLUCION_CLIENTE` suman · `SALIDA_VENTA`, `DEVOLUCION_PROVEEDOR` y
  `ROTURA` restan · `AJUSTE` aplica la cantidad con su signo.
- El responsable es el usuario del token (no se envía `usuarioId`).
- Un `EMPLEADO` solo opera su sucursal; el `ADMINISTRADOR`, todas.
- Compras y devoluciones a proveedor exigen un proveedor que venda ese producto.
- Ajustes, roturas y devoluciones exigen motivo.
- Solo los productos por metro, kilo o litro admiten decimales.
- El stock no puede quedar negativo. Si dos operaciones cambian el mismo stock a la vez, la
  segunda recibe `409` (bloqueo optimista).

Errores: `401` sin sesión o credenciales inválidas · `403` rol sin permiso · `404` recurso
inexistente · `422` regla de negocio · `400` validación · `409` conflicto.
