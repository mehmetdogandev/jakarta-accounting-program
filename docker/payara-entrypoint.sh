#!/bin/bash
set -euo pipefail

POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-accounting}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

POOL_PROPS="serverName=${POSTGRES_HOST}:portNumber=${POSTGRES_PORT}:databaseName=${POSTGRES_DB}:user=${POSTGRES_USER}:password=${POSTGRES_PASSWORD}"

CONFIG_FILE="${CONFIG_FILE:-/opt/payara/config/post-boot-commands.asadmin}"
mkdir -p "$(dirname "${CONFIG_FILE}")"

cat > "${CONFIG_FILE}" <<EOF
create-jdbc-connection-pool --datasourceclassname org.postgresql.ds.PGSimpleDataSource --restype javax.sql.DataSource --property ${POOL_PROPS} AccountingPool
create-jdbc-resource --connectionpoolid AccountingPool jdbc/testPSQL
EOF

exec /opt/payara/scripts/startInForeground.sh "$@"
