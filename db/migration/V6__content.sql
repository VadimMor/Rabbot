-- Посты, варианты под площадки, ревизии, медиа, сцены Reels, ревью и проверки бренда.

-- Создание таблицы post (посты).
CREATE TABLE post
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит пост.
    tenant_id   uuid        NOT NULL,
    -- UUID бренда, к которому принадлежит пост.
    brand_id    uuid        NOT NULL,
    -- UUID тренда, к которому принадлежит пост (null, если пост не привязан к тренду).
    trend_id    uuid        REFERENCES trend (id) ON DELETE SET NULL,
    -- Тип поста: TEXT или REEL (клип).
    kind        text        NOT NULL DEFAULT 'TEXT' CHECK (kind IN ('TEXT', 'REEL')),
    -- Заголовок поста (для отображения в интерфейсе).
    title       text        NOT NULL,
    -- Статус поста: DRAFT, GENERATING, IN_REVIEW, CHANGES_REQUESTED, APPROVED, SCHEDULED, PUBLISHED, PARTIALLY_PUBLISHED, FAILED или ARCHIVED.
    status      text        NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT', 'GENERATING', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED',
                                              'SCHEDULED', 'PUBLISHED', 'PARTIALLY_PUBLISHED', 'FAILED', 'ARCHIVED')),
    -- Оценка соответствия бренду (0–100) от LLM-судьи (null, если пост ещё не проверен).
    brand_score smallint    CHECK (brand_score BETWEEN 0 AND 100),
    -- Дата и время создания записи.
    created_by  uuid        REFERENCES app_user (id) ON DELETE SET NULL,
    -- Идентификатор пользователя, который одобрил пост (null, если пост ещё не одобрен).
    approved_by uuid        REFERENCES app_user (id) ON DELETE SET NULL,
    -- Дата и время одобрения поста (null, если пост ещё не одобрен).
    approved_at timestamptz,
    -- Версия записи для оптимистичной блокировки (JPA @Version).
    version     integer     NOT NULL DEFAULT 0, -- optimistic locking (JPA @Version)
    -- Дата и время создания записи.
    created_at  timestamptz NOT NULL DEFAULT now(),
    -- Дата и время последнего обновления записи.
    updated_at  timestamptz NOT NULL DEFAULT now(),
    -- Уникальные ограничения на комбинацию tenant_id и id.
    UNIQUE (tenant_id, id),
    -- Внешний ключ на комбинацию tenant_id и brand_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, brand_id) REFERENCES brand (tenant_id, id) ON DELETE CASCADE
);

-- Создание индекса для ускорения поиска постов по brand_id и status.
CREATE INDEX post_brand_status_idx ON post (brand_id, status, updated_at DESC);
-- Создание индекса для ускорения поиска постов в очереди на ревью (status = 'IN_REVIEW').
CREATE INDEX post_review_queue_idx ON post (tenant_id, updated_at) WHERE status = 'IN_REVIEW';

-- Создание триггера для обновления поля updated_at в таблице post.
CREATE TRIGGER post_set_updated_at
    -- Триггер для автоматического обновления поля updated_at.
    BEFORE UPDATE ON post FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Создание таблицы post_variant (варианты поста под площадки).
CREATE TABLE post_variant
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id          uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит пост.
    tenant_id   uuid            NOT NULL,
    -- UUID поста, к которому принадлежит вариант.
    post_id     uuid            NOT NULL,
    -- Площадка соцсети, для которой предназначен вариант.
    platform    social_platform NOT NULL,
    -- Номер варианта (1, 2, 3 и т. д.).
    variant_no  smallint        NOT NULL DEFAULT 1 CHECK (variant_no > 0),
    -- Текст варианта поста (для отображения в интерфейсе).
    body        text            NOT NULL DEFAULT '',
    -- Список хэштегов варианта поста (для отображения в интерфейсе).
    hashtags    text[]          NOT NULL DEFAULT '{}',
    -- Количество символов в тексте варианта поста (для отображения в интерфейсе).
    char_count  integer         GENERATED ALWAYS AS (char_length(body)) STORED,
    -- Выбран ли вариант для публикации на площадке (true, если выбран, false — если не выбран).
    is_selected boolean         NOT NULL DEFAULT false,
    -- Черновая версия модели, которая сгенерировала вариант поста (для отображения в интерфейсе).
    draft_model text,
    -- Финальная версия модели, которая сгенерировала вариант поста (для отображения в интерфейсе).
    final_model text,
    -- Дата и время создания записи.
    created_at  timestamptz     NOT NULL DEFAULT now(),
    -- Дата и время последнего обновления записи.
    updated_at  timestamptz     NOT NULL DEFAULT now(),
    -- Уникальные ограничения на комбинацию post_id, platform и variant_no, а также на комбинацию tenant_id и id.
    UNIQUE (post_id, platform, variant_no),
    -- Уникальные ограничения на комбинацию tenant_id и id.
    UNIQUE (tenant_id, id),
    -- Внешний ключ на комбинацию tenant_id и post_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, post_id) REFERENCES post (tenant_id, id) ON DELETE CASCADE
);

-- Для каждой площадки выбран не больше одного варианта.
-- Создание частичного уникального индекса для обеспечения уникальности выбранного варианта поста на площадке.
CREATE UNIQUE INDEX post_variant_one_selected_idx ON post_variant (post_id, platform) WHERE is_selected;

-- Создание индекса для ускорения поиска вариантов постов по post_id и platform.
CREATE TRIGGER post_variant_set_updated_at
    -- Триггер для автоматического обновления поля updated_at.
    BEFORE UPDATE ON post_variant FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Создание таблицы post_revision (ревизии постов).
CREATE TABLE post_revision
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id              bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- UUID организации, к которой принадлежит пост.
    tenant_id       uuid        NOT NULL,
    -- UUID варианта поста, к которому принадлежит ревизия.
    post_variant_id uuid        NOT NULL,
    -- Текст ревизии поста (для отображения в интерфейсе).
    body            text        NOT NULL,
    -- Автор ревизии: USER (человек) или LLM (модель).
    author_type     text        NOT NULL CHECK (author_type IN ('USER', 'LLM')),
    -- UUID автора ревизии (null, если автор — LLM).
    author_id       uuid,
    -- Черновая версия модели, которая сгенерировала ревизию поста (для отображения в интерфейсе).
    model           text,
    -- Дата и время создания записи.
    created_at      timestamptz NOT NULL DEFAULT now(),
    -- Внешний ключ на комбинацию tenant_id и post_variant_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, post_variant_id) REFERENCES post_variant (tenant_id, id) ON DELETE CASCADE
);

-- Создание индекса для ускорения поиска ревизий постов по post_variant_id.
CREATE INDEX post_revision_variant_idx ON post_revision (post_variant_id, created_at DESC);

-- Создание таблицы media_asset (медиа-ресурсы поста).
CREATE TABLE media_asset
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит медиа-ресурс.
    tenant_id   uuid        NOT NULL,
    -- UUID поста, к которому принадлежит медиа-ресурс.
    post_id     uuid        NOT NULL,
    -- Тип медиа-ресурса: IMAGE, VIDEO, AUDIO, SUBTITLES или COVER.
    kind        text        NOT NULL CHECK (kind IN ('IMAGE', 'VIDEO', 'AUDIO', 'SUBTITLES', 'COVER')),
    -- Ключ объекта в S3 / MinIO (для отображения в интерфейсе).
    storage_key text        NOT NULL UNIQUE, -- ключ объекта в S3 / MinIO
    -- MIME-тип медиа-ресурса (для отображения в интерфейсе).
    mime_type   text        NOT NULL,
    -- Размеры медиа-ресурса (для отображения в интерфейсе).
    width       integer,
    height      integer,
    -- Продолжительность медиа-ресурса в миллисекундах (для отображения в интерфейсе).
    duration_ms integer,
    -- Размер медиа-ресурса в байтах (для отображения в интерфейсе).
    size_bytes  bigint,
    -- Статус медиа-ресурса: UPLOADING, PROCESSING, READY или FAILED.
    status      text        NOT NULL DEFAULT 'READY' CHECK (status IN ('UPLOADING', 'PROCESSING', 'READY', 'FAILED')),
    -- Дата и время создания записи.
    created_at  timestamptz NOT NULL DEFAULT now(),
    -- Внешний ключ на комбинацию tenant_id и post_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, post_id) REFERENCES post (tenant_id, id) ON DELETE CASCADE
);

-- Создание индекса для ускорения поиска медиа-ресурсов по post_id.
CREATE INDEX media_asset_post_idx ON media_asset (post_id);

-- Сцены сценария для Reels / клипов.
CREATE TABLE reel_scene
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id        uuid     PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит сцена.
    tenant_id uuid     NOT NULL,
    -- UUID поста, к которому принадлежит сцена.
    post_id   uuid     NOT NULL,
    -- Порядковый номер сцены в сценарии (1, 2, 3 и т. д.).
    ord       smallint NOT NULL CHECK (ord > 0),
    -- Роль сцены в сценарии: HOOK, PROBLEM, SOLUTION, CTA или OTHER.
    role      text     NOT NULL CHECK (role IN ('HOOK', 'PROBLEM', 'SOLUTION', 'CTA', 'OTHER')),
    -- Время начала сцены в миллисекундах (для отображения в интерфейсе).
    start_ms  integer  NOT NULL CHECK (start_ms >= 0),
    -- Время окончания сцены в миллисекундах (для отображения в интерфейсе).
    end_ms    integer  NOT NULL,
    -- Текст сценария сцены (для отображения в интерфейсе).
    script    text     NOT NULL,
    -- Подзаголовок сцены (для отображения в интерфейсе).
    subtitle  text,
    -- Проверка, что время окончания сцены больше времени начала сцены.
    CHECK (end_ms > start_ms),
    -- Уникальные ограничения на комбинацию post_id и ord.
    UNIQUE (post_id, ord),
    -- Внешний ключ на комбинацию tenant_id и post_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, post_id) REFERENCES post (tenant_id, id) ON DELETE CASCADE
);

-- Созданипе таблицы review (ревью постов).
CREATE TABLE review
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит ревью.
    tenant_id   uuid        NOT NULL,
    -- UUID поста, к которому принадлежит ревью.
    post_id     uuid        NOT NULL,
    -- UUID пользователя, который оставил ревью (null, если ревью оставлено LLM-судьёй).
    reviewer_id uuid        REFERENCES app_user (id) ON DELETE SET NULL,
    -- Решение ревью: APPROVED, CHANGES_REQUESTED или REJECTED.
    decision    text        NOT NULL CHECK (decision IN ('APPROVED', 'CHANGES_REQUESTED', 'REJECTED')),
    -- Комментарий ревью (для отображения в интерфейсе).
    comment     text,
    -- Дата и время создания записи.
    created_at  timestamptz NOT NULL DEFAULT now(),
    -- Внешний ключ на комбинацию tenant_id и post_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, post_id) REFERENCES post (tenant_id, id) ON DELETE CASCADE
);

-- Создание индекса для ускорения поиска ревью по post_id.
CREATE INDEX review_post_idx ON review (post_id, created_at DESC);

-- Результат LLM-судьи: оценка соответствия бренду и замечания.
CREATE TABLE brand_check
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит проверка.
    tenant_id       uuid        NOT NULL,
    -- UUID варианта поста, к которому принадлежит проверка.
    post_variant_id uuid        NOT NULL,
    -- Оценка соответствия бренду (0–100) от LLM-судьи.
    score           smallint    NOT NULL CHECK (score BETWEEN 0 AND 100),
    -- JSON с замечаниями LLM-судьи (для отображения в интерфейсе).
    verdict         jsonb       NOT NULL,
    -- Черновая версия модели, которая сгенерировала проверку поста (для отображения в интерфейсе).
    model           text        NOT NULL,
    -- Дата и время создания записи.
    created_at      timestamptz NOT NULL DEFAULT now(),
    -- Внешний ключ на комбинацию tenant_id и post_variant_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, post_variant_id) REFERENCES post_variant (tenant_id, id) ON DELETE CASCADE
);

-- Создание индекса для ускорения поиска проверок бренда по post_variant_id.
CREATE INDEX brand_check_variant_idx ON brand_check (post_variant_id, created_at DESC);