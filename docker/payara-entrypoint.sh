#!/bin/bash
set -euo pipefail

POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-accounting}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

POOL_PROPS="serverName=${POSTGRES_HOST}:portNumber=${POSTGRES_PORT}:databaseName=${POSTGRES_DB}:user=${POSTGRES_USER}:password=${POSTGRES_PASSWORD}"

# Must match image env POSTBOOT_COMMANDS / POSTBOOT_COMMANDS_FINAL (see startInForeground.sh).
mkdir -p "$(dirname "${POSTBOOT_COMMANDS}")"

cat > "${POSTBOOT_COMMANDS}" <<EOF
create-jdbc-connection-pool --datasourceclassname org.postgresql.ds.PGSimpleDataSource --restype javax.sql.DataSource --property ${POOL_PROPS} AccountingPool
create-jdbc-resource --connectionpoolid AccountingPool jdbc/testPSQL
EOF

# Default Payara entrypoint runs this; we skipped it by using a custom ENTRYPOINT.
# It copies POSTBOOT_COMMANDS -> POSTBOOT_COMMANDS_FINAL and appends deploy lines for DEPLOY_DIR.
# shellcheck source=/dev/null
. /opt/payara/scripts/init_1_generate_deploy_commands.sh

exec /opt/payara/scripts/startInForeground.sh "$@"
