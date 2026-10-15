# Bulón S.R.L. — Backend de gestión de stock

API REST en Java 21 + Spring Boot 3, persistencia en SQL Server vía JPA.
Monolito modular con arquitectura en capas (Controller → Service → Repository).

## Estructura

```
src/main/java/com/bulon/stock/
├── StockApplication.java
├── common/         # Excepciones → ProblemDetail, Swagger y paginación
├── seguridad/      # Spring Security (HTTP Basic) y matriz de permisos por rol
├── usuarios/       # Usuarios, roles y contraseñas (BCrypt)
├── ubicaciones/    # Sucursal y Depósito
├── productos/      # Catálogo, categorías, stock mínimo y punto de pedido
├── proveedores/    # Proveedores (N:M con productos, uno preferido)
├── stock/          # Existencias por producto y ubicación (@Version)
├── movimientos/    # Motor de movimientos: factory/ (Factory Method) y strategy/ (Strategy)
├── alertas/        # Alertas de reposición: Observer + Decorator
├── compras/        # Órdenes de compra y sugerencias de reposición
├── reportes/       # Dashboard, rotación y exportación PDF/Excel (Adapter)
└── seed/           # Datos de ejemplo para desarrollo
```

Cada módulo agrupa su entidad, repositorio, servicio, controlador y DTOs.
Un módulo solo accede a otro a través de su `Service`, nunca a su `Repository`.
Los patrones de la Entrega 2 y dónde están: [`docs/patrones.md`](../docs/patrones.md).

## Base de datos

SQL Server 2022 corre en Docker (`docker-compose.yml` en la raíz del repositorio):

```bash
docker compose up -d        # desde la raíz: levanta SQL Server y crea la base bulon_stock
docker compose down         # detiene (los datos se conservan en el volumen)
docker compose down -v      # detiene y BORRA los datos
```

> **macOS con Apple Silicon (M1/M2/M3…):** Microsoft solo publica la imagen de SQL Server para `amd64`, por eso
> el compose fija `platform: linux/amd64`. En Docker Desktop activá *Settings → General → "Use Rosetta for
> x86_64/amd64 emulation on Apple Silicon"* para que corra emulado con buen rendimiento.

El esquema lo manejan las migraciones de **Flyway** en `src/main/resources/db/migration`,
que se aplican solas al iniciar el backend. Hibernate solo valida que las entidades coincidan
(`ddl-auto: validate`). Para cambiar el modelo se agrega un script nuevo (`V2__descripcion.sql`, ...);
nunca se modifica uno ya aplicado.

## Configuración

Las credenciales de la base se leen de `backend/.env`, que **no se sube al repositorio**.
La primera vez:

```bash
cp .env.example .env        # desde backend/ (ya trae la contraseña por defecto de Docker)
```

| Variable      | Ejemplo                                                       |
|---------------|---------------------------------------------------------------|
| `DB_URL`      | `jdbc:sqlserver://localhost:1433;databaseName=bulon_stock;...` |
| `DB_USER`     | `sa`                                                          |
| `DB_PASSWORD` | la misma que `MSSQL_SA_PASSWORD` de `docker-compose.yml` (por defecto `Bulon_Dev_2026!`) |
| `SEED_ENABLED`| `true` carga datos de ejemplo de la ferretería si la base está vacía (default `false`) |
| `ALERTAS_EMAIL` | destinatario de las alertas por email; requiere además `SPRING_MAIL_HOST` (opcional) |

El seeder (`seed/DataSeeder.java`) carga 3 usuarios con contraseña, 5 proveedores, 5 categorías y
25 productos, con el stock inicial registrado como ingresos (70 % depósito, 30 % sucursal) y algunas
ventas. Varios productos quedan bajo el mínimo o en el punto de pedido para probar las alertas.
Es idempotente: a usuarios existentes sin contraseña les asigna la del seeder.
Para volver a cargarlo desde cero: `docker compose down -v`.
Una base creada antes del login (solo V1) queda con usuarios sin contraseña: arrancar una vez con
`SEED_ENABLED=true` les asigna la del seeder (o cargar `usuarios.password_hash` con un hash BCrypt).

Las variables de entorno del sistema tienen prioridad sobre el `.env`.
Si falta el archivo, el backend no arranca y muestra un error como
`Driver ... claims to not accept jdbcUrl, ${DB_URL}`.

## Ejecutar

```bash
docker compose up -d                 # 1. desde la raíz del repo
./mvnw spring-boot:run               # 2. desde backend/ — API en http://localhost:8080/api
./mvnw test                          # tests sobre H2 en memoria (no requieren Docker)
../scripts/test-sqlserver.sh         # los mismos tests contra SQL Server real con Flyway
```

Swagger UI: http://localhost:8080/api/swagger-ui.html (botón *Authorize* con un usuario del seeder).

Para conectarse con un cliente (Azure Data Studio, DBeaver, SSMS): `localhost,1433`, usuario `sa`.
Si se usa `sqlcmd`, agregar `-I` (lo requieren los índices filtrados).

## Autenticación y permisos

HTTP Basic sin sesión: cada request lleva `Authorization: Basic base64(usuario:contraseña)`.
El frontend valida el login con `GET /api/auth/me`. Usuarios del seeder:

| Usuario    | Contraseña    | Rol                | Puede                                                    |
|------------|---------------|--------------------|----------------------------------------------------------|
| `admin`    | `admin123`    | ADMINISTRADOR      | Todo, incluida la gestión de usuarios                    |
| `deposito` | `deposito123` | ENCARGADO_DEPOSITO | Todo menos usuarios                                      |
| `ventas`   | `ventas123`   | VENDEDOR           | Consultar y registrar ventas y devoluciones              |

La matriz está en `seguridad/SecurityConfig.java` y, por tipo de movimiento, en `movimientos/TipoMovimiento.java`.

## Endpoints

Todas las rutas cuelgan de `/api`. Los listados paginados aceptan `page`, `size` y `sort` y devuelven
`{content, page: {size, number, totalElements, totalPages}}`. Los errores son `ProblemDetail` (`{status, detail}`):
400 validación, 401 sin login, 403 rol sin permiso, 404 no existe, 409 conflicto (concurrencia o registro en uso),
422 regla de negocio (stock insuficiente, código duplicado…).

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/health` | Estado de la API y de la base (público) |
| GET | `/auth/me` | Usuario autenticado |
| GET | `/ubicaciones` | Sucursal y Depósito |
| GET/POST | `/categorias` | Categorías |
| GET | `/productos?q=&categoriaId=&activo=&reposicion=` | Productos con stock por ubicación y estado (paginado) |
| GET/POST/PUT | `/productos`, `/productos/{id}` | Alta, consulta y edición (la baja es `activo: false`) |
| GET/POST/PUT/DELETE | `/proveedores`, `/proveedores/{id}` | Proveedores (DELETE da 409 si está en uso) |
| GET | `/movimientos?productoId=&tipo=&usuarioId=&ubicacionId=&desde=&hasta=` | Historial y auditoría (paginado) |
| POST | `/movimientos` | Registrar uno de los 6 tipos; inmutable una vez creado |
| GET | `/alertas?abiertas=` | Alertas de reposición (paginado) |
| GET | `/dashboard` | Indicadores del panel |
| GET | `/reportes/rotacion?desde=&hasta=&limite=` | Más vendidos y punto de pedido sugerido |
| GET | `/reportes/movimientos?formato=PDF\|EXCEL&…filtros` | Exportar historial |
| GET | `/reportes/inventario?formato=PDF\|EXCEL` | Exportar inventario |
| GET/POST | `/ordenes-compra`, `/ordenes-compra/{id}` | Órdenes de compra |
| POST | `/ordenes-compra/{id}/enviar` \| `/recibir` \| `/cancelar` | Cambios de estado; recibir ingresa el stock |
| GET | `/ordenes-compra/sugerencias` | Qué reponer, agrupado por proveedor preferido |
| GET/POST/PUT | `/usuarios`, `/usuarios/{id}` | Solo administrador |

## Movimientos de stock

| Tipo | Efecto | Ubicaciones | Reglas |
|------|--------|-------------|--------|
| `INGRESO_COMPRA` | suma | `ubicacionId` | cantidad > 0 |
| `DEVOLUCION` | suma | `ubicacionId` | cantidad > 0 |
| `SALIDA_VENTA` | resta | `ubicacionId` | cantidad > 0, stock suficiente en esa ubicación |
| `ROTURA_PERDIDA` | resta | `ubicacionId` | cantidad > 0, stock suficiente |
| `AJUSTE` | suma o resta | `ubicacionId` | cantidad ≠ 0 con signo, `motivo` obligatorio |
| `TRASLADO` | resta en origen y suma en destino | `ubicacionId` → `ubicacionDestinoId` | cantidad > 0, origen ≠ destino |

El usuario responsable es siempre el autenticado. Dos movimientos simultáneos sobre la misma existencia no
pueden dejarla inconsistente: el segundo recibe 409 (bloqueo optimista con `@Version`) y se reintenta.
Después de cada movimiento se evalúa el estado del producto (stock total vs. mínimo y punto de pedido) y se
abre, escala o cierra su alerta de reposición.
