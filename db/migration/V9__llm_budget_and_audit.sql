-- Бюджет на облачные LLM и журнал аудита.
-- Детальный лог каждого LLM-вызова пишется в ClickHouse; здесь — агрегат для быстрых проверок лимита.

CREATE TABLE llm_budget
(
    tenant_id    uuid           NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    period_month date           NOT NULL CHECK (extract(day FROM period_month) = 1),
    limit_usd    numeric(10, 2) NOT NULL CHECK (limit_usd >= 0),
    spent_usd    numeric(12, 4) NOT NULL DEFAULT 0 CHECK (spent_usd >= 0),
    on_exceeded  text           NOT NULL DEFAULT 'LOCAL_ONLY' CHECK (on_exceeded IN ('LOCAL_ONLY', 'BLOCK')),
    updated_at   timestamptz    NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, period_month)
);

CREATE TRIGGER llm_budget_set_updated_at
    BEFORE UPDATE ON llm_budget FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE audit_log
(
    id          bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   uuid        NOT NULL,
    actor_id    uuid,
    action      text        NOT NULL,
    entity_type text        NOT NULL,
    entity_id   text,
    details     jsonb       NOT NULL DEFAULT '{}'::jsonb,
    ip          inet,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX audit_log_tenant_time_idx ON audit_log (tenant_id, created_at DESC);
