#!/bin/bash
# Выполняется один раз при первом запуске контейнера (пустой volume).
# Роли:
#   rabbot_migrator — владелец схемы, под ним работает Flyway (DDL);
#   rabbot_app      — роль приложений, только DML (SELECT/INSERT/UPDATE/DELETE).
set -euo pipefail

# Создание ролей и базы данных
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v migrator_pw="$RABBOT_MIGRATOR_PASSWORD" -v app_pw="$RABBOT_APP_PASSWORD" <<'EOSQL'
-- Создание роли rabbot_migrator с правами на вход и паролем из переменной окружения RABBOT_MIGRATOR_PASSWORD.
CREATE ROLE rabbot_migrator LOGIN PASSWORD :'migrator_pw';
-- Создание роли rabbot_app с правами на вход и паролем из переменной окружения RABBOT_APP_PASSWORD.
CREATE ROLE rabbot_app      LOGIN PASSWORD :'app_pw';
-- Создание базы данных rabbot с владельцем rabbot_migrator.
CREATE DATABASE rabbot OWNER rabbot_migrator;
EOSQL

# Создание схемы core и предоставление прав роли rabbot_app
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname rabbot <<'EOSQL'
-- Запрет доступа к схеме public и базе данных rabbot для всех пользователей (PUBLIC).
REVOKE ALL ON SCHEMA public FROM PUBLIC;
-- Запрет подключения к базе данных rabbot для всех пользователей (PUBLIC).
REVOKE ALL ON DATABASE rabbot FROM PUBLIC;
-- Предоставление роли rabbot_app права на подключение к базе данных rabbot.
GRANT CONNECT ON DATABASE rabbot TO rabbot_app;

-- Создание расширения pg_stat_statements для сбора статистики выполнения запросов.
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Создание схемы core и предоставление прав роли rabbot_app
CREATE SCHEMA core AUTHORIZATION rabbot_migrator;
-- Предоставление роли rabbot_app права на использование схемы core.
GRANT USAGE ON SCHEMA core TO rabbot_app;

-- Предоставление роли rabbot_app прав на таблицы, последовательности и функции в схеме core, созданные ролью rabbot_migrator.
ALTER DEFAULT PRIVILEGES FOR ROLE rabbot_migrator IN SCHEMA core
  -- Предоставление роли rabbot_app прав на таблицы в схеме core, созданные ролью rabbot_migrator.
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rabbot_app;
-- Предоставление роли rabbot_app прав на последовательности в схеме core, созданные ролью rabbot_migrator.
ALTER DEFAULT PRIVILEGES FOR ROLE rabbot_migrator IN SCHEMA core
  -- Предоставление роли rabbot_app прав на последовательности в схеме core, созданные ролью rabbot_migrator.
  GRANT USAGE, SELECT ON SEQUENCES TO rabbot_app;
-- Предоставление роли rabbot_app прав на функции в схеме core, созданные ролью rabbot_migrator.
ALTER DEFAULT PRIVILEGES FOR ROLE rabbot_migrator IN SCHEMA core
  -- Предоставление роли rabbot_app прав на функции в схеме core, созданные ролью rabbot_migrator.
  GRANT EXECUTE ON FUNCTIONS TO rabbot_app;

-- Установка схемы core как search_path для ролей rabbot_migrator и rabbot_app в базе данных rabbot.
ALTER ROLE rabbot_migrator IN DATABASE rabbot SET search_path = core;
-- Установка схемы core как search_path для роли rabbot_app в базе данных rabbot.
ALTER ROLE rabbot_app      IN DATABASE rabbot SET search_path = core;
EOSQL

echo "rabbot: roles, database and schema created"