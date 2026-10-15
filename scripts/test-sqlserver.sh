#!/usr/bin/env bash
# Corre la suite de tests del backend contra SQL Server real (Docker) en vez de H2:
# aplica las migraciones de Flyway sobre la base descartable bulon_stock_test y
# Hibernate valida que las entidades coincidan con el esquema.
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] && set -a && . ./.env && set +a
password="${MSSQL_SA_PASSWORD:-Bulon_Dev_2026!}"
port="${MSSQL_PORT:-1433}"

docker compose up -d --wait sqlserver
docker compose run --rm sqlserver-init >/dev/null

cd backend
TEST_DB_URL="jdbc:sqlserver://localhost:${port};databaseName=bulon_stock_test;encrypt=true;trustServerCertificate=true" \
TEST_DB_USER=sa \
TEST_DB_PASSWORD="$password" \
TEST_DB_DRIVER=com.microsoft.sqlserver.jdbc.SQLServerDriver \
TEST_FLYWAY=true \
TEST_DDL_AUTO=validate \
TEST_SQL_INIT=never \
TEST_DEFER_INIT=false \
./mvnw -q test "$@"
echo "OK: tests en verde contra SQL Server"
