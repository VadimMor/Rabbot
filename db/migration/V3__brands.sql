-- Бренды и примеры постов для RAG.
-- Сами векторы лежат в Qdrant, здесь — исходный текст и ссылка на точку.

CREATE TABLE brand
(
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       uuid        NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    name            text        NOT NULL,
    audience        text,
    language        text        NOT NULL DEFAULT 'ru',
    tone            text[]      NOT NULL DEFAULT '{}',
    forbidden_words text[]      NOT NULL DEFAULT '{}',
    default_cta     text,
    style_rules     jsonb       NOT NULL DEFAULT '{}'::jsonb, -- эмодзи, хэштеги, длина и т. п.
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name),
    UNIQUE (tenant_id, id)
);

CREATE TRIGGER brand_set_updated_at
    BEFORE UPDATE ON brand FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE brand_example
(
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       uuid        NOT NULL,
    brand_id        uuid        NOT NULL,
    platform        social_platform,
    body            text        NOT NULL,
    source          text        NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'IMPORTED', 'TOP_PERFORMER')),
    engagement_rate numeric(6, 4),
    vector_id       uuid, -- id точки в Qdrant
    created_at      timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (tenant_id, brand_id) REFERENCES brand (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX brand_example_brand_idx ON brand_example (brand_id);
