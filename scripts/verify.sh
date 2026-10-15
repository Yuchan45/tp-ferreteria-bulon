#!/usr/bin/env bash
# Verificación completa antes de commitear: tests del backend y build + lint del frontend.
# Con --sqlserver también corre los tests del backend contra SQL Server real (Docker).
set -euo pipefail
cd "$(dirname "$0")/.."

echo "== Backend: tests (H2)"
(cd backend && ./mvnw -q test)

if [[ "${1:-}" == "--sqlserver" ]]; then
  echo "== Backend: tests (SQL Server)"
  scripts/test-sqlserver.sh
fi

echo "== Frontend: build y lint"
(cd frontend && { [ -d node_modules ] || npm ci --silent; } && npm run -s build && npm run -s lint)

echo "OK: todo en verde"
