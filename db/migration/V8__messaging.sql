-- Transactional outbox и идемпотентные консьюмеры.

-- Событие пишется в одной транзакции с изменением бизнес-данных,
-- relay отправляет его в Kafka и проставляет published_at.
CREATE TABLE outbox_event
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id             uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит событие.
    tenant_id      uuid,
    -- Тип агрегата, который изменился (например, "post", "channel" и т. п.).
    aggregate_type text        NOT NULL,
    -- ID агрегата, который изменился (например, UUID поста, UUID канала и т. п.).
    aggregate_id   text        NOT NULL,
    -- Тип события (например, "post_created", "channel_connected" и т. п.).
    event_type     text        NOT NULL,
    -- Тема Kafka, в которую будет отправлено событие.
    topic          text        NOT NULL,
    -- Ключ Kafka, по которому будет определяться партиция.
    message_key    text,
    -- Полезная нагрузка события (JSON).
    payload        jsonb       NOT NULL,
    -- Заголовки Kafka (JSON).
    headers        jsonb       NOT NULL DEFAULT '{}'::jsonb,
    -- Дата и время создания записи.
    created_at     timestamptz NOT NULL DEFAULT now(),
    -- Дата и время публикации события в Kafka (null, если событие ещё не было опубликовано).
    published_at   timestamptz,
    -- Количество попыток публикации события в Kafka.
    attempts       integer     NOT NULL DEFAULT 0,
    -- Последняя ошибка при попытке публикации события в Kafka (null, если ошибок не было).
    last_error     text
);

-- Создание индекса для ускорения поиска непубликованных событий по created_at.
CREATE INDEX outbox_event_pending_idx ON outbox_event (created_at) WHERE published_at IS NULL;

-- Консьюмер записывает id обработанного сообщения в той же транзакции,
-- поэтому повторная доставка из Kafka не приводит к повторной обработке.
CREATE TABLE processed_message
(
    -- Имя консьюмера, который обработал сообщение (например, "post_service", "channel_service" и т. п.).
    consumer     text        NOT NULL,
    -- UUID сообщения, которое было обработано.
    message_id   uuid        NOT NULL,
    -- Дата и время обработки сообщения.
    processed_at timestamptz NOT NULL DEFAULT now(),
    -- Ключевые поля для уникальности: consumer + message_id, чтобы один и тот же консьюмер не обрабатывал одно и то же сообщение дважды.
    PRIMARY KEY (consumer, message_id)
);
