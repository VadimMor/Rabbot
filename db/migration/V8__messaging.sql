-- Transactional outbox и идемпотентные консьюмеры.

-- Событие пишется в одной транзакции с изменением бизнес-данных,
-- relay отправляет его в Kafka и проставляет published_at.
CREATE TABLE outbox_event
(
    id             uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      uuid,
    aggregate_type text        NOT NULL,
    aggregate_id   text        NOT NULL,
    event_type     text        NOT NULL,
    topic          text        NOT NULL,
    message_key    text,
    payload        jsonb       NOT NULL,
    headers        jsonb       NOT NULL DEFAULT '{}'::jsonb,
    created_at     timestamptz NOT NULL DEFAULT now(),
    published_at   timestamptz,
    attempts       integer     NOT NULL DEFAULT 0,
    last_error     text
);

CREATE INDEX outbox_event_pending_idx ON outbox_event (created_at) WHERE published_at IS NULL;

-- Консьюмер записывает id обработанного сообщения в той же транзакции,
-- поэтому повторная доставка из Kafka не приводит к повторной обработке.
CREATE TABLE processed_message
(
    consumer     text        NOT NULL,
    message_id   uuid        NOT NULL,
    processed_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, message_id)
);
