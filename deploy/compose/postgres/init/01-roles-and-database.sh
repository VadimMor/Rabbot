#!/bin/bash
# Выполняется один раз при первом запуске контейнера (пустой volume).
# Роли:
#   rabbot_migrator — владелец схемы, под ним работает Flyway (DDL);
#   rabbot_app      — роль приложений, только DML (SELECT/INSERT/UPDATE/DELETE).
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v migrator_pw="$RABBOT_MIGRATOR_PASSWORD" -v app_pw="$RABBOT_APP_PASSWORD" <<'EOSQL'
CREATE ROLE rabbot_migrator LOGIN PASSWORD :'migrator_pw';
CREATE ROLE rabbot_app      LOGIN PASSWORD :'app_pw';
CREATE DATABASE rabbot OWNER rabbot_migrator;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname rabbot <<'EOSQL'
REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE rabbot FROM PUBLIC;
GRANT CONNECT ON DATABASE rabbot TO rabbot_app;

CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

CREATE SCHEMA core AUTHORIZATION rabbot_migrator;
GRANT USAGE ON SCHEMA core TO rabbot_app;

ALTER DEFAULT PRIVILEGES FOR ROLE rabbot_migrator IN SCHEMA core
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rabbot_app;
ALTER DEFAULT PRIVILEGES FOR ROLE rabbot_migrator IN SCHEMA core
  GRANT USAGE, SELECT ON SEQUENCES TO rabbot_app;
ALTER DEFAULT PRIVILEGES FOR ROLE rabbot_migrator IN SCHEMA core
  GRANT EXECUTE ON FUNCTIONS TO rabbot_app;

ALTER ROLE rabbot_migrator IN DATABASE rabbot SET search_path = core;
ALTER ROLE rabbot_app      IN DATABASE rabbot SET search_path = core;
EOSQL

echo "rabbot: roles, database and schema created"
