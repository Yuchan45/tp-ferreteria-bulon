# AGENTS.md

Sistema de gestión de stock de Bulón S.R.L. (TP de Proceso de Desarrollo de Software, UADE).
Monorepo: `backend/` (Java 21, Spring Boot 3.5, SQL Server, Flyway) y `frontend/` (React 19 + TS + Vite).
Requisitos del dominio: `docs/informes/` (Entrega 1 = qué hace, Entrega 2 = patrones de diseño).
Código, mensajes y commits en español.

## Comandos

| Para | Comando (desde la raíz) |
|------|-------------------------|
| Verificar todo antes de commitear | `scripts/verify.sh` (tests H2 + build y lint del frontend) |
| Lo mismo + tests contra SQL Server real | `scripts/verify.sh --sqlserver` |
| Solo tests del backend (H2, sin Docker) | `cd backend && ./mvnw test` |
| Base de datos | `docker compose up -d` |
| Backend con datos de ejemplo | `cd backend && cp .env.example .env` y `./mvnw spring-boot:run` |
| Frontend | `cd frontend && npm install && npm run dev` → http://localhost:5173 |
| API interactiva | http://localhost:8080/api/swagger-ui.html |

Usuarios de desarrollo: `admin/admin123`, `deposito/deposito123`, `ventas/ventas123`.
Si `./mvnw test` falla con `ClassFormatError ... Code length 0`, es compilación incremental rota: `./mvnw clean test`.

## Reglas que no se rompen

- **Esquema solo por Flyway.** Un cambio de modelo es un archivo nuevo `backend/src/main/resources/db/migration/V<n>__descripcion.sql`.
  Nunca editar una migración existente. Hibernate corre en `validate`: si la entidad no coincide, el backend no arranca.
  Separar con `GO` los `ALTER TABLE ... ADD` de las sentencias que usan esas columnas.
- **El stock solo cambia por un movimiento.** `stock.StockService.mover` es la única escritura de existencias y solo la llaman
  las estrategias de `movimientos/strategy/`. Los movimientos son inmutables: un error se corrige con un `AJUSTE`.
- **Un tipo de movimiento nuevo** = valor en `TipoMovimiento` + un creator en `movimientos/factory/` + una estrategia en
  `movimientos/strategy/` (o sumarlo a los `tipos()` de uno existente) + el valor en el CHECK de una migración nueva.
  `MovimientoStockService` falla al arrancar si a un tipo le falta su creator o su estrategia.
- **Módulos:** un paquete solo usa otro a través de su `*Service`, nunca de su `Repository`.
- **Permisos:** por URL y método en `seguridad/SecurityConfig.java`; por tipo de movimiento en `TipoMovimiento.permitidoPara`.
- **Responsable de un movimiento u orden:** siempre el `Principal`, nunca un campo del body.
- **Errores:** `BusinessException` → 422, `ResourceNotFoundException` → 404 (`common/exception/GlobalExceptionHandler`).

## Tests

- Prueban comportamiento por HTTP (`ApiTest`: MockMvc + HTTP Basic real + usuarios del seeder). Cada test crea sus propios
  productos con `crearProducto` para no depender del orden; aseveran valores exactos (stock por ubicación, status, `detail`).
- Corren en H2 por defecto y contra SQL Server con `scripts/test-sqlserver.sh` (misma suite, base `bulon_stock_test`, Flyway + `validate`).
  Un cambio en migraciones, queries nativas o concurrencia se verifica con el script, no solo con H2:
  H2 en modo MSSQLServer no aplica los CHECK ni los índices filtrados de las migraciones.
- Un comportamiento nuevo del backend va con su test en el paquete del módulo (`src/test/java/com/bulon/stock/<módulo>/`).

## Dónde está cada cosa

- Patrones de diseño → clases: `docs/patrones.md`.
- Endpoints, errores, tabla de movimientos y permisos: `backend/README.md`.
- Pantallas y estructura del frontend: `frontend/README.md`.
- Modelo de datos: migraciones `V1`–`V4` y `backend/src/main/resources/db/dbdiagram/`.
