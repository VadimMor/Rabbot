-- Публикации — самая нагруженная таблица.
-- Партиционирование по месяцам (scheduled_at): планировщик читает только «горячую» партицию,
-- старые партиции отсоединяются/архивируются без VACUUM всей таблицы.

CREATE TABLE publication
(
    id               uuid        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id        uuid        NOT NULL,
    post_variant_id  uuid        NOT NULL,
    channel_id       uuid        NOT NULL,
    scheduled_at     timestamptz NOT NULL,
    status           text        NOT NULL DEFAULT 'SCHEDULED'
                                 CHECK (status IN ('SCHEDULED', 'DISPATCHED', 'PUBLISHING', 'RETRY_WAIT',
                                                   'PUBLISHED', 'FAILED', 'CANCELLED')),
    attempts         smallint    NOT NULL DEFAULT 0,
    next_attempt_at  timestamptz,
    external_post_id text,
    external_url     text,
    last_error       text,
    published_at     timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (id, scheduled_at),
    -- один и тот же вариант нельзя дважды поставить в один канал на одно время
    UNIQUE (post_variant_id, channel_id, scheduled_at),
    FOREIGN KEY (tenant_id, post_variant_id) REFERENCES post_variant (tenant_id, id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id, channel_id) REFERENCES channel (tenant_id, id) ON DELETE CASCADE
) PARTITION BY RANGE (scheduled_at);

-- Выборка «созревших» публикаций планировщиком (FOR UPDATE SKIP LOCKED).
CREATE INDEX publication_due_idx ON publication (scheduled_at) WHERE status = 'SCHEDULED';
-- Повторные попытки.
CREATE INDEX publication_retry_idx ON publication (next_attempt_at) WHERE status = 'RETRY_WAIT';
-- Календарь канала и лимит публикаций в сутки.
CREATE INDEX publication_channel_idx ON publication (channel_id, scheduled_at DESC);
CREATE INDEX publication_tenant_idx ON publication (tenant_id, scheduled_at DESC);

CREATE TRIGGER publication_set_updated_at
    BEFORE UPDATE ON publication FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Создаёт месячные партиции (UTC). Идемпотентна — вызывается и из миграции, и из планировщика.
CREATE FUNCTION ensure_publication_partitions(p_from date, p_months integer) RETURNS integer
    LANGUAGE plpgsql AS
$$
DECLARE
    m       date;
    part    text;
    created integer := 0;
BEGIN
    FOR i IN 0 .. p_months - 1
        LOOP
            m := (date_trunc('month', p_from) + make_interval(months => i))::date;
            part := format('publication_%s', to_char(m, 'YYYY_MM'));
            IF to_regclass(part) IS NULL THEN
                EXECUTE format('CREATE TABLE %I PARTITION OF publication FOR VALUES FROM (%L) TO (%L)',
                               part, m::text || ' 00:00:00+00', (m + interval '1 month')::date::text || ' 00:00:00+00');
                created := created + 1;
            END IF;
        END LOOP;
    RETURN created;
END
$$;

SELECT ensure_publication_partitions(date '2026-09-01', 12);

-- Страховка: строки вне созданных диапазонов не теряются.
CREATE TABLE publication_default PARTITION OF publication DEFAULT;

-- Попытки публикации. idempotency_key передаётся площадке/проверяется перед ретраем,
-- чтобы пост не вышел дважды.
CREATE TABLE publication_attempt
(
    id                       bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id                uuid        NOT NULL,
    publication_id           uuid        NOT NULL,
    publication_scheduled_at timestamptz NOT NULL,
    attempt_no               smallint    NOT NULL CHECK (attempt_no > 0),
    idempotency_key          uuid        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    started_at               timestamptz NOT NULL DEFAULT now(),
    finished_at              timestamptz,
    outcome                  text        CHECK (outcome IN ('SUCCESS', 'RETRYABLE_ERROR', 'FATAL_ERROR')),
    http_status              smallint,
    error                    text,
    external_post_id         text,
    UNIQUE (publication_id, attempt_no),
    FOREIGN KEY (publication_id, publication_scheduled_at)
        REFERENCES publication (id, scheduled_at) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX publication_attempt_pub_idx ON publication_attempt (publication_id);
