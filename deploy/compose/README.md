# Локальная инфраструктура

| Сервис | Образ | Порт на хосте | Профиль |
|---|---|---|---|
| PostgreSQL 17 | `postgres:17-alpine` | 5432 | — |
| Flyway (миграции, одноразовый) | `flyway/flyway:11` | — | — |
| Redis | `redis:7.4-alpine` | 6379 | — |
| Kafka 4 (KRaft) | `apache/kafka:4.0.0` | 29092 | — |
| Создание топиков (одноразовый) | `apache/kafka:4.0.0` | — | — |
| Kafka UI | `ghcr.io/kafbat/kafka-ui` | 8085 | `tools` |
| pgAdmin | `dpage/pgadmin4:9` | 5050 | `tools` |

## Запуск

Все команды — из корня репозитория.

```bash
cp deploy/compose/.env.example deploy/compose/.env

docker compose -f deploy/compose/docker-compose.yml up -d
docker compose -f deploy/compose/docker-compose.yml --profile tools up -d
```

Проверка:

```bash
docker compose -f deploy/compose/docker-compose.yml ps
docker compose -f deploy/compose/docker-compose.yml logs flyway
docker compose -f deploy/compose/docker-compose.yml logs kafka-init
```

## Подключение

| Что | Строка подключения |
|---|---|
| Приложение | `jdbc:postgresql://localhost:5432/rabbot`, пользователь `rabbot_app` |
| Миграции / IDE | `jdbc:postgresql://localhost:5432/rabbot`, пользователь `rabbot_migrator` |
| Redis | `redis://:<REDIS_PASSWORD>@localhost:6379` |
| Kafka с хоста | `localhost:29092` |
| Kafka из контейнеров | `kafka:9092` |
| Kafka UI | http://localhost:8085 |
| pgAdmin | http://localhost:5050 (сервер «Rabbot (local)» уже добавлен) |

## Частые операции

```bash
# повторно прогнать миграции после добавления новой
docker compose -f deploy/compose/docker-compose.yml run --rm flyway migrate

# состояние миграций
docker compose -f deploy/compose/docker-compose.yml run --rm flyway info

# консоль psql
docker exec -it rabbot-postgres psql -U rabbot_migrator -d rabbot

# остановить
docker compose -f deploy/compose/docker-compose.yml --profile tools down

# остановить и удалить все данные
docker compose -f deploy/compose/docker-compose.yml --profile tools down -v
```