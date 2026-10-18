-- Бюджет на облачные LLM и журнал аудита.
-- Детальный лог каждого LLM-вызова пишется в ClickHouse; здесь — агрегат для быстрых проверок лимита.

-- Создание таблицы llm_budget (бюджет на LLM).
CREATE TABLE llm_budget
(
    -- UUID организации, к которой принадлежит бюджет.
    tenant_id    uuid           NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    -- Месяц, за который ведётся учёт бюджета (первое число месяца).
    period_month date           NOT NULL CHECK (extract(day FROM period_month) = 1),
    -- Лимит в долларах США на месяц.
    limit_usd    numeric(10, 2) NOT NULL CHECK (limit_usd >= 0),
    -- Потрачено в долларах США за месяц.
    spent_usd    numeric(12, 4) NOT NULL DEFAULT 0 CHECK (spent_usd >= 0),
    -- Действие при превышении лимита: LOCAL_ONLY (только локальные LLM) или BLOCK (блокировать все LLM).
    on_exceeded  text           NOT NULL DEFAULT 'LOCAL_ONLY' CHECK (on_exceeded IN ('LOCAL_ONLY', 'BLOCK')),
    -- Дата и время создания записи.
    updated_at   timestamptz    NOT NULL DEFAULT now(),
    -- Ключ для уникальности: tenant_id + period_month, чтобы не было двух бюджетов на один месяц для одной организации.
    PRIMARY KEY (tenant_id, period_month)
);

-- Создание триггера для обновления поля updated_at в таблице llm_budget.
CREATE TRIGGER llm_budget_set_updated_at
    -- Триггер для автоматического обновления поля updated_at.
    BEFORE UPDATE ON llm_budget FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Создание таблицы audit_log (журнал аудита).
CREATE TABLE audit_log
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id          bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- UUID организации, к которой принадлежит запись аудита.
    tenant_id   uuid        NOT NULL,
    -- UUID пользователя, который совершил действие (null, если действие совершено системой).
    actor_id    uuid,
    -- Действие, которое совершил пользователь (например, "CREATE_POST", "DELETE_BRAND" и т. п.).
    action      text        NOT NULL,
    -- Тип сущности, к которой относится действие (например, "POST", "BRAND" и т. п.).
    entity_type text        NOT NULL,
    -- UUID сущности, к которой относится действие (например, UUID поста, бренда и т. п.).
    entity_id   text,
    -- Дополнительные данные о действии в формате JSON (например, старые и новые значения полей).
    details     jsonb       NOT NULL DEFAULT '{}'::jsonb,
    -- IP-адрес пользователя, который совершил действие.
    ip          inet,
    -- Дата и время создания записи.
    created_at  timestamptz NOT NULL DEFAULT now()
);

-- Создание индекса для ускорения поиска записей аудита по tenant_id и created_at.
CREATE INDEX audit_log_tenant_time_idx ON audit_log (tenant_id, created_at DESC);