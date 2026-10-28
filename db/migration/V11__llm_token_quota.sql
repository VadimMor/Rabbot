-- Rabbot — бесплатный открытый проект. Денег в системе нет:
-- лимит на облачные LLM считаем в токенах, а не в долларах.

-- 1. Таблица: llm_budget → llm_quota.
ALTER TABLE llm_budget RENAME TO llm_quota;

-- Имена ограничений и триггера тоже приводим к новому имени таблицы,
-- чтобы в ошибках и в \d не было "budget".
ALTER TABLE llm_quota RENAME CONSTRAINT llm_budget_pkey TO llm_quota_pkey;
ALTER TABLE llm_quota RENAME CONSTRAINT llm_budget_tenant_id_fkey TO llm_quota_tenant_id_fkey;
ALTER TABLE llm_quota RENAME CONSTRAINT llm_budget_period_month_check TO llm_quota_period_month_check;
ALTER TABLE llm_quota RENAME CONSTRAINT llm_budget_on_exceeded_check TO llm_quota_on_exceeded_check;
ALTER TRIGGER llm_budget_set_updated_at ON llm_quota RENAME TO llm_quota_set_updated_at;

-- 2. Доллары → токены. Перевести суммы в токены нельзя (цена зависит от модели),
--    поэтому старые колонки удаляем, новые начинаются с разумных значений.
ALTER TABLE llm_quota
    DROP COLUMN limit_usd,
    DROP COLUMN spent_usd;

ALTER TABLE llm_quota
    -- Лимит токенов облачных моделей на месяц (вход + выход). 0 — облако выключено.
    ADD COLUMN limit_tokens bigint NOT NULL DEFAULT 5000000 CHECK (limit_tokens >= 0),
    -- Использовано токенов облачных моделей за месяц. Локальные модели не считаются.
    ADD COLUMN used_tokens  bigint NOT NULL DEFAULT 0       CHECK (used_tokens >= 0);

COMMENT ON TABLE llm_quota IS
    'Месячный лимит токенов облачных LLM. Локальные модели (vLLM/Ollama) не ограничиваются.';

-- 3. Событие уведомления: LLM_BUDGET_EXCEEDED → LLM_LIMIT_EXCEEDED.
--    Сначала снимаем CHECK, переименовываем значения, потом ставим новый CHECK.
ALTER TABLE notification_setting DROP CONSTRAINT notification_setting_event_type_check;

UPDATE notification_setting
SET event_type = 'LLM_LIMIT_EXCEEDED'
WHERE event_type = 'LLM_BUDGET_EXCEEDED';

ALTER TABLE notification_setting
    ADD CONSTRAINT notification_setting_event_type_check
        CHECK (event_type IN ('POST_NEEDS_REVIEW', 'PUBLICATION_FAILED',
                              'LLM_LIMIT_EXCEEDED', 'TRENDS_DIGEST', 'PRODUCT_NEWS'));