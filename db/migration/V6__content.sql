-- Посты, варианты под площадки, ревизии, медиа, сцены Reels, ревью и проверки бренда.

CREATE TABLE post
(
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid        NOT NULL,
    brand_id    uuid        NOT NULL,
    trend_id    uuid        REFERENCES trend (id) ON DELETE SET NULL,
    kind        text        NOT NULL DEFAULT 'TEXT' CHECK (kind IN ('TEXT', 'REEL')),
    title       text        NOT NULL,
    status      text        NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT', 'GENERATING', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED',
                                              'SCHEDULED', 'PUBLISHED', 'PARTIALLY_PUBLISHED', 'FAILED', 'ARCHIVED')),
    brand_score smallint    CHECK (brand_score BETWEEN 0 AND 100),
    created_by  uuid        REFERENCES app_user (id) ON DELETE SET NULL,
    approved_by uuid        REFERENCES app_user (id) ON DELETE SET NULL,
    approved_at timestamptz,
    version     integer     NOT NULL DEFAULT 0, -- optimistic locking (JPA @Version)
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, brand_id) REFERENCES brand (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX post_brand_status_idx ON post (brand_id, status, updated_at DESC);
CREATE INDEX post_review_queue_idx ON post (tenant_id, updated_at) WHERE status = 'IN_REVIEW';

CREATE TRIGGER post_set_updated_at
    BEFORE UPDATE ON post FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE post_variant
(
    id          uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid            NOT NULL,
    post_id     uuid            NOT NULL,
    platform    social_platform NOT NULL,
    variant_no  smallint        NOT NULL DEFAULT 1 CHECK (variant_no > 0),
    body        text            NOT NULL DEFAULT '',
    hashtags    text[]          NOT NULL DEFAULT '{}',
    char_count  integer         GENERATED ALWAYS AS (char_length(body)) STORED,
    is_selected boolean         NOT NULL DEFAULT false,
    draft_model text,
    final_model text,
    created_at  timestamptz     NOT NULL DEFAULT now(),
    updated_at  timestamptz     NOT NULL DEFAULT now(),
    UNIQUE (post_id, platform, variant_no),
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, post_id) REFERENCES post (tenant_id, id) ON DELETE CASCADE
);

-- Для каждой площадки выбран не больше одного варианта.
CREATE UNIQUE INDEX post_variant_one_selected_idx ON post_variant (post_id, platform) WHERE is_selected;

CREATE TRIGGER post_variant_set_updated_at
    BEFORE UPDATE ON post_variant FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE post_revision
(
    id              bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id       uuid        NOT NULL,
    post_variant_id uuid        NOT NULL,
    body            text        NOT NULL,
    author_type     text        NOT NULL CHECK (author_type IN ('USER', 'LLM')),
    author_id       uuid,
    model           text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (tenant_id, post_variant_id) REFERENCES post_variant (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX post_revision_variant_idx ON post_revision (post_variant_id, created_at DESC);

CREATE TABLE media_asset
(
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid        NOT NULL,
    post_id     uuid        NOT NULL,
    kind        text        NOT NULL CHECK (kind IN ('IMAGE', 'VIDEO', 'AUDIO', 'SUBTITLES', 'COVER')),
    storage_key text        NOT NULL UNIQUE, -- ключ объекта в S3 / MinIO
    mime_type   text        NOT NULL,
    width       integer,
    height      integer,
    duration_ms integer,
    size_bytes  bigint,
    status      text        NOT NULL DEFAULT 'READY' CHECK (status IN ('UPLOADING', 'PROCESSING', 'READY', 'FAILED')),
    created_at  timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (tenant_id, post_id) REFERENCES post (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX media_asset_post_idx ON media_asset (post_id);

-- Сцены сценария для Reels / клипов.
CREATE TABLE reel_scene
(
    id        uuid     PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id uuid     NOT NULL,
    post_id   uuid     NOT NULL,
    ord       smallint NOT NULL CHECK (ord > 0),
    role      text     NOT NULL CHECK (role IN ('HOOK', 'PROBLEM', 'SOLUTION', 'CTA', 'OTHER')),
    start_ms  integer  NOT NULL CHECK (start_ms >= 0),
    end_ms    integer  NOT NULL,
    script    text     NOT NULL,
    subtitle  text,
    CHECK (end_ms > start_ms),
    UNIQUE (post_id, ord),
    FOREIGN KEY (tenant_id, post_id) REFERENCES post (tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE review
(
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid        NOT NULL,
    post_id     uuid        NOT NULL,
    reviewer_id uuid        REFERENCES app_user (id) ON DELETE SET NULL,
    decision    text        NOT NULL CHECK (decision IN ('APPROVED', 'CHANGES_REQUESTED', 'REJECTED')),
    comment     text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (tenant_id, post_id) REFERENCES post (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX review_post_idx ON review (post_id, created_at DESC);

-- Результат LLM-судьи: оценка соответствия бренду и замечания.
CREATE TABLE brand_check
(
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       uuid        NOT NULL,
    post_variant_id uuid        NOT NULL,
    score           smallint    NOT NULL CHECK (score BETWEEN 0 AND 100),
    verdict         jsonb       NOT NULL,
    model           text        NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (tenant_id, post_variant_id) REFERENCES post_variant (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX brand_check_variant_idx ON brand_check (post_variant_id, created_at DESC);
