-- Бренды и примеры постов для RAG.
-- Сами векторы лежат в Qdrant, здесь — исходный текст и ссылка на точку.

-- Создание таблицы brand (бренд).
CREATE TABLE brand
(
    -- UUID, чтобы можно было бренда его в Keycloak.
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит бренд.
    tenant_id       uuid        NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    -- Название бренда (для отображения в интерфейсе).
    name            text        NOT NULL,
    -- Описание бренда (для отображения в интерфейсе).
    audience        text,
    -- Язык бренда (для генерации контента).
    language        text        NOT NULL DEFAULT 'ru',
    -- Тон бренда (для генерации контента).
    tone            text[]      NOT NULL DEFAULT '{}',
    -- Запрещённые слова (для генерации контента).
    forbidden_words text[]      NOT NULL DEFAULT '{}',
    -- Призыв к действию по умолчанию (для генерации контента).
    default_cta     text,
    -- Правила стиля бренда (для генерации контента).
    style_rules     jsonb       NOT NULL DEFAULT '{}'::jsonb, -- эмодзи, хэштеги, длина и т. п.
    -- Дата создания записи.
    created_at      timestamptz NOT NULL DEFAULT now(),
    -- Дата последнего обновления записи.
    updated_at      timestamptz NOT NULL DEFAULT now(),
    -- Уникальные ограничения на комбинацию tenant_id и name, а также на комбинацию tenant_id и id.
    UNIQUE (tenant_id, name),
    -- Уникальные ограничения на комбинацию tenant_id и id.
    UNIQUE (tenant_id, id)
);

-- Создание триггера для обновления поля updated_at в таблице brand.
CREATE TRIGGER brand_set_updated_at
    -- Триггер для автоматического обновления поля updated_at.
    BEFORE UPDATE ON brand FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Создание таблицы brand_example (пример поста для бренда).
CREATE TABLE brand_example
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит бренд.
    tenant_id       uuid        NOT NULL,
    -- UUID бренда, к которому принадлежит пример поста.
    brand_id        uuid        NOT NULL,
    -- Площадка, на которой был опубликован пример поста.
    platform        social_platform,
    -- Исходный текст примера поста.
    body            text        NOT NULL,
    -- Источник примера поста: MANUAL, IMPORTED или TOP_PERFORMER.
    source          text        NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'IMPORTED', 'TOP_PERFORMER')),
    -- Дата публикации примера поста.
    engagement_rate numeric(6, 4),
    -- ID точки в Qdrant, где хранится вектор примера поста.
    vector_id       uuid, -- id точки в Qdrant
    -- Дата создания записи.
    created_at      timestamptz NOT NULL DEFAULT now(),
    -- Внешний ключ на комбинацию tenant_id и brand_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, brand_id) REFERENCES brand (tenant_id, id) ON DELETE CASCADE
);

-- Создание индекса для ускорения поиска примеров постов по brand_id.
CREATE INDEX brand_example_brand_idx ON brand_example (brand_id);