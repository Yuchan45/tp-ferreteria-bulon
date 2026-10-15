# Bulón S.R.L. · Sistema de gestión de stock

Aplicación web interna para controlar el inventario de la ferretería: productos, stock por sucursal y depósito,
movimientos con trazabilidad, alertas de reposición, órdenes de compra y reportes.

## Puesta en marcha

```bash
docker compose up -d                      # SQL Server en Docker
cd backend && cp .env.example .env        # completar DB_PASSWORD (por defecto Bulon_Dev_2026!)
./mvnw spring-boot:run                    # API en http://localhost:8080/api
cd ../frontend && npm install && npm run dev   # http://localhost:5173
```

Usuarios de ejemplo: `admin/admin123` (administrador), `deposito/deposito123` (encargado), `ventas/ventas123` (vendedor).

## Documentación

- [`backend/README.md`](backend/README.md): API, permisos, movimientos y base de datos.
- [`frontend/README.md`](frontend/README.md): pantallas y estructura.
- [`docs/patrones.md`](docs/patrones.md): patrones de diseño de la Entrega 2 en el código.
- [`docs/informes/`](docs/informes/): Entregas 1 y 2.
- [`AGENTS.md`](AGENTS.md): comandos y reglas para quien modifique el código (personas o agentes).

Verificación completa: `scripts/verify.sh` (agregar `--sqlserver` para correr los tests contra SQL Server real).
