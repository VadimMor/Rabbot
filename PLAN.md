# План разработки Rabbot

## Этап 1. Инфраструктура

- [ ] Kubernetes-кластер (k3s) с поддержкой GPU
- [ ] Argo CD
- [ ] Kafka
- [ ] PostgreSQL
- [ ] ClickHouse
- [ ] Qdrant
- [ ] Redis
- [ ] MinIO
- [ ] Temporal
- [ ] Keycloak
- [ ] Vault
- [ ] Prometheus, Grafana, Loki, Tempo, OpenTelemetry Collector
- [ ] Langfuse
- [ ] vLLM и TEI
- [ ] Docker Compose для локальной разработки
- [ ] CI в GitHub Actions

## Этап 2. Каркас приложения

- [ ] Gradle multi-project
- [ ] Библиотека контрактов событий
- [ ] Библиотека transactional outbox
- [ ] Библиотека безопасности (OIDC, tenant)
- [ ] Сервисы: gateway, api, ingestor, enricher, generator, llm-gateway, scheduler, publisher, analytics
- [ ] Модель данных и миграции Flyway
- [ ] Helm-чарт
- [ ] Frontend: React + Vite + SCSS, дизайн-токены, layout, вход через Keycloak

## Этап 3. Публикация

- [ ] Планировщик на временных бакетах
- [ ] Publisher и адаптер Telegram
- [ ] Идемпотентность публикаций
- [ ] Ретраи и DLQ
- [ ] Rate limits площадок
- [ ] FakeGram (эмулятор API соцсети)
- [ ] Нагрузочный тест Gatling: 1 млн отложенных постов
- [ ] UI: календарь публикаций

## Этап 4. LLM

- [ ] llm-gateway: роутинг задач по моделям
- [ ] Подключение vLLM, TEI и облачной модели
- [ ] Fallback и circuit breaker
- [ ] Бюджеты и лимиты
- [ ] Маскирование персональных данных
- [ ] Семантический кэш
- [ ] GPU-воркер
- [ ] Temporal-пайплайн генерации поста
- [ ] Шаблоны промптов
- [ ] UI: редактор поста и очередь одобрения

## Этап 5. Бренд

- [ ] Профиль бренда
- [ ] Загрузка примеров постов в Qdrant
- [ ] RAG по примерам
- [ ] Brand-checker
- [ ] Факт-чек
- [ ] UI: экран «Бренд и LLM»

## Этап 6. Тренды

- [ ] Источники: RSS, Hacker News, Reddit, YouTube, Wordstat
- [ ] Эмбеддинги и дедупликация
- [ ] Кластеризация
- [ ] Скоринг трендов
- [ ] Хранение в ClickHouse и Qdrant
- [ ] Нагрузочный тест: 10 млн элементов в сутки
- [ ] UI: лента трендов

## Этап 7. Reels и клипы

- [ ] Генерация сценария по сценам
- [ ] Субтитры (Whisper)
- [ ] Рендер видео 9:16 (FFmpeg)
- [ ] Адаптации под Instagram, TikTok, VK
- [ ] Адаптеры публикации видео
- [ ] UI: редактор Reels

## Этап 8. Соцсети, аккаунт, аналитика

- [ ] Подключение соцсетей через OAuth
- [ ] Адаптеры VK, LinkedIn, Instagram, TikTok
- [ ] Настройки аккаунта и уведомления
- [ ] Сбор метрик постов
- [ ] Обратная связь в скоринг и примеры бренда
- [ ] UI: дашборд, «Соцсети», «Настройки аккаунта», «Администрирование»

## Этап 9. Прод-готовность

- [ ] HA-конфигурация Helm
- [ ] Автоскейлинг KEDA
- [ ] Хаос-тесты (Chaos Mesh)
- [ ] SLO и алерты
- [ ] Бэкапы и восстановление
- [ ] Runbooks
- [ ] Результаты тестов в README
- [ ] Демо-видео
