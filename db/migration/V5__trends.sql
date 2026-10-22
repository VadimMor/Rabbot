-- Тренды: кластеры, найденные enricher'ом. Общие для всех тенантов.
-- Сырые элементы (миллионы в сутки) живут в ClickHouse, векторы — в Qdrant.

-- Создание таблицы trend (тренд).
CREATE TABLE trend
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id            uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Хэш кластера тренда, чтобы можно было быстро находить одинаковые тренды.
    cluster_key   text          NOT NULL UNIQUE,
    -- Заголовок тренда (для отображения в интерфейсе).
    title         text          NOT NULL,
    -- Краткое описание тренда (для отображения в интерфейсе).
    summary       text,
    -- Язык тренда (для генерации контента).
    language      text,
    -- Текст, на основе которого был найден тренд (для отображения в интерфейсе).
    sources       text[]        NOT NULL DEFAULT '{}',
    -- Количество упоминаний тренда в источниках (для отображения в интерфейсе).
    mention_count integer       NOT NULL DEFAULT 0,
    -- Рост тренда в процентах за последние 7 дней (для отображения в интерфейсе).
    growth_pct    numeric(8, 2),
    -- Релевантность тренда для брендов (для отображения в интерфейсе).
    score         numeric(5, 2) NOT NULL DEFAULT 0 CHECK (score BETWEEN 0 AND 100),
    -- Статус тренда: ACTIVE, FADING или ARCHIVED.
    status        text          NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FADING', 'ARCHIVED')),
    -- Дата и время первого появления тренда в источниках.
    first_seen_at timestamptz   NOT NULL DEFAULT now(),
    -- Дата и время последнего появления тренда в источниках.
    last_seen_at  timestamptz   NOT NULL DEFAULT now(),
    -- Дата и время создания записи.
    created_at    timestamptz   NOT NULL DEFAULT now(),
    -- Дата и время последнего обновления записи.
    updated_at    timestamptz   NOT NULL DEFAULT now()
);

-- Создание индекса для ускорения поиска трендов по score и status.
CREATE INDEX trend_active_score_idx ON trend (score DESC) WHERE status = 'ACTIVE';
-- Создание индекса для ускорения поиска трендов по title с использованием pg_trgm.
CREATE INDEX trend_title_trgm_idx ON trend USING gin (title gin_trgm_ops);

-- Создание триггера для обновления поля updated_at в таблице trend.
CREATE TRIGGER trend_set_updated_at
    -- Триггер для автоматического обновления поля updated_at.
    BEFORE UPDATE ON trend FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Релевантность тренда конкретному бренду и предложенный угол подачи.
-- Создание таблицы brand_trend (тренд для бренда).
CREATE TABLE brand_trend
(
    -- UUID организации, к которой принадлежит бренд.
    tenant_id  uuid          NOT NULL,
    -- UUID бренда, к которому принадлежит тренд.
    brand_id   uuid          NOT NULL,
    -- UUID тренда, к которому принадлежит бренд.
    trend_id   uuid          NOT NULL REFERENCES trend (id) ON DELETE CASCADE,
    -- Релевантность тренда для бренда (для отображения в интерфейсе).
    relevance  numeric(5, 4) NOT NULL CHECK (relevance BETWEEN 0 AND 1),
    -- Предложенный угол подачи тренда для бренда (для отображения в интерфейсе).
    angle      text,
    -- Статус dismissed: true, если бренд отклонил тренд, false — если тренд ещё актуален для бренда.
    dismissed  boolean       NOT NULL DEFAULT false,
    -- Дата и время создания записи.
    created_at timestamptz   NOT NULL DEFAULT now(),
    -- Идентификатор пользователя, который отклонил тренд (null, если тренд ещё актуален для бренда).
    PRIMARY KEY (brand_id, trend_id),
    -- Внешний ключ на комбинацию tenant_id и brand_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, brand_id) REFERENCES brand (tenant_id, id) ON DELETE CASCADE
);

-- Создание индекса для ускорения поиска трендов по brand_id и relevance, исключая отклонённые тренды.
CREATE INDEX brand_trend_feed_idx ON brand_trend (brand_id, relevance DESC) WHERE NOT dismissed;