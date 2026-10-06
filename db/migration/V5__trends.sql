-- Тренды: кластеры, найденные enricher'ом. Общие для всех тенантов.
-- Сырые элементы (миллионы в сутки) живут в ClickHouse, векторы — в Qdrant.

CREATE TABLE trend
(
    id            uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    cluster_key   text          NOT NULL UNIQUE,
    title         text          NOT NULL,
    summary       text,
    language      text,
    sources       text[]        NOT NULL DEFAULT '{}',
    mention_count integer       NOT NULL DEFAULT 0,
    growth_pct    numeric(8, 2),
    score         numeric(5, 2) NOT NULL DEFAULT 0 CHECK (score BETWEEN 0 AND 100),
    status        text          NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FADING', 'ARCHIVED')),
    first_seen_at timestamptz   NOT NULL DEFAULT now(),
    last_seen_at  timestamptz   NOT NULL DEFAULT now(),
    created_at    timestamptz   NOT NULL DEFAULT now(),
    updated_at    timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX trend_active_score_idx ON trend (score DESC) WHERE status = 'ACTIVE';
CREATE INDEX trend_title_trgm_idx ON trend USING gin (title gin_trgm_ops);

CREATE TRIGGER trend_set_updated_at
    BEFORE UPDATE ON trend FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Релевантность тренда конкретному бренду и предложенный угол подачи.
CREATE TABLE brand_trend
(
    tenant_id  uuid          NOT NULL,
    brand_id   uuid          NOT NULL,
    trend_id   uuid          NOT NULL REFERENCES trend (id) ON DELETE CASCADE,
    relevance  numeric(5, 4) NOT NULL CHECK (relevance BETWEEN 0 AND 1),
    angle      text,
    dismissed  boolean       NOT NULL DEFAULT false,
    created_at timestamptz   NOT NULL DEFAULT now(),
    PRIMARY KEY (brand_id, trend_id),
    FOREIGN KEY (tenant_id, brand_id) REFERENCES brand (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX brand_trend_feed_idx ON brand_trend (brand_id, relevance DESC) WHERE NOT dismissed;
