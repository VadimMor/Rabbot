-- Rabbot — открытый проект: платных тарифов и подписок нет.
-- Удаляем план подписки организации и пробный период.

-- Колонки тарифа больше не нужны (CHECK на plan удалится вместе с колонкой).
ALTER TABLE tenant
    DROP COLUMN plan,
    DROP COLUMN plan_expires_at;

-- TRIAL был пробным периодом платного тарифа. Переводим такие организации в ACTIVE.
UPDATE tenant SET status = 'ACTIVE' WHERE status = 'TRIAL';

-- Статус организации: ACTIVE или SUSPENDED (заблокирована администратором).
ALTER TABLE tenant DROP CONSTRAINT tenant_status_check;
ALTER TABLE tenant
    ADD CONSTRAINT tenant_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED'));