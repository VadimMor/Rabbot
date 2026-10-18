-- Публикации — самая нагруженная таблица.
-- Партиционирование по месяцам (scheduled_at): планировщик читает только «горячую» партицию,
-- старые партиции отсоединяются/архивируются без VACUUM всей таблицы.

-- Создание таблицы publication (публикации постов в каналах).
CREATE TABLE publication
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id               uuid        NOT NULL DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит публикация.
    tenant_id        uuid        NOT NULL,
    -- UUID варианта поста, который будет опубликован.
    post_variant_id  uuid        NOT NULL,
    -- UUID канала, в котором будет опубликован пост.
    channel_id       uuid        NOT NULL,
    -- Дата и время, когда публикация должна быть опубликована.
    scheduled_at     timestamptz NOT NULL,
    -- Статус публикации: SCHEDULED, DISPATCHED, PUBLISHING, RETRY_WAIT, PUBLISHED, FAILED или CANCELLED.
    status           text        NOT NULL DEFAULT 'SCHEDULED'
                                 CHECK (status IN ('SCHEDULED', 'DISPATCHED', 'PUBLISHING', 'RETRY_WAIT',
                                                   'PUBLISHED', 'FAILED', 'CANCELLED')),
    -- Количество попыток публикации (для отображения в интерфейсе).
    attempts         smallint    NOT NULL DEFAULT 0,
    -- Дата и время следующей попытки публикации (для отображения в интерфейсе).
    next_attempt_at  timestamptz,
    -- ID поста в соцсети (null, если публикация ещё не была опубликована).
    external_post_id text,
    -- Ссылка на опубликованный пост в соцсети (null, если публикация ещё не была опубликована).
    external_url     text,
    -- Последняя ошибка при попытке публикации (null, если ошибок не было).
    last_error       text,
    -- Дата и время публикации (null, если публикация ещё не была опубликована).
    published_at     timestamptz,
    -- Дата и время создания записи.
    created_at       timestamptz NOT NULL DEFAULT now(),
    -- Дата и время последнего обновления записи.
    updated_at       timestamptz NOT NULL DEFAULT now(),
    -- Ключевые поля для партиционирования и уникальности: id + scheduled_at, чтобы можно было делать UPSERT по id, и уникальность по post_variant_id + channel_id + scheduled_at, чтобы один и тот же вариант нельзя было поставить в один канал на одно время.
    PRIMARY KEY (id, scheduled_at),
    -- Уникальность по post_variant_id + channel_id + scheduled_at, чтобы один и тот же вариант нельзя было поставить в один канал на одно время.
    UNIQUE (post_variant_id, channel_id, scheduled_at),
    -- Внешний ключ на комбинацию tenant_id и post_variant_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, post_variant_id) REFERENCES post_variant (tenant_id, id) ON DELETE CASCADE,
    -- Внешний ключ на комбинацию tenant_id и channel_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, channel_id) REFERENCES channel (tenant_id, id) ON DELETE CASCADE
) PARTITION BY RANGE (scheduled_at);

-- Выборка «созревших» публикаций планировщиком (FOR UPDATE SKIP LOCKED).
CREATE INDEX publication_due_idx ON publication (scheduled_at) WHERE status = 'SCHEDULED';
-- Повторные попытки.
CREATE INDEX publication_retry_idx ON publication (next_attempt_at) WHERE status = 'RETRY_WAIT';
-- Календарь канала и лимит публикаций в сутки.
CREATE INDEX publication_channel_idx ON publication (channel_id, scheduled_at DESC);
CREATE INDEX publication_tenant_idx ON publication (tenant_id, scheduled_at DESC);

-- Создание триггера для обновления поля updated_at в таблице publication.
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

-- Вызов функции ensure_publication_partitions из миграции, чтобы создать партиции на ближайшие 12 месяцев.
SELECT ensure_publication_partitions(date '2026-09-01', 12);

-- Страховка: строки вне созданных диапазонов не теряются.
CREATE TABLE publication_default PARTITION OF publication DEFAULT;

-- Попытки публикации. idempotency_key передаётся площадке/проверяется перед ретраем,
-- чтобы пост не вышел дважды.
CREATE TABLE publication_attempt
(
    -- UUID публикации, к которой относится попытка.
    id                       bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- UUID организации, к которой принадлежит публикация.
    tenant_id                uuid        NOT NULL,
    -- UUID публикации, к которой относится попытка.
    publication_id           uuid        NOT NULL,
    -- Дата и время, когда публикация была запланирована.
    publication_scheduled_at timestamptz NOT NULL,
    -- Номер попытки публикации (1, 2, 3 и т. д.).
    attempt_no               smallint    NOT NULL CHECK (attempt_no > 0),
    -- UUID попытки публикации, чтобы можно было использовать его в Keycloak.
    idempotency_key          uuid        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    -- Дата и время начала попытки публикации.
    started_at               timestamptz NOT NULL DEFAULT now(),
    -- Дата и время окончания попытки публикации.
    finished_at              timestamptz,
    -- Результат попытки публикации: SUCCESS, RETRYABLE_ERROR или FATAL_ERROR.
    outcome                  text        CHECK (outcome IN ('SUCCESS', 'RETRYABLE_ERROR', 'FATAL_ERROR')),
    -- HTTP-статус ответа от соцсети (null, если публикация не была отправлена).
    http_status              smallint,
    -- Текст ошибки от соцсети (null, если публикация не была отправлена).
    error                    text,
    -- ID поста в соцсети (null, если публикация не была отправлена).
    external_post_id         text,
    -- Уникальность по комбинации publication_id и attempt_no, чтобы не было двух одинаковых попыток публикации для одной публикации.
    UNIQUE (publication_id, attempt_no),
    -- Внешний ключ на комбинацию publication_id и publication_scheduled_at, с каскадным удалением и обновлением.
    FOREIGN KEY (publication_id, publication_scheduled_at)
        REFERENCES publication (id, scheduled_at) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Создание индекса для ускорения поиска попыток публикации по publication_id.
CREATE INDEX publication_attempt_pub_idx ON publication_attempt (publication_id);