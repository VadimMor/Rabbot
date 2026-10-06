-- Тенанты (организации) и пользователи.
-- Пользователь аутентифицируется в Keycloak; здесь — профиль, роль и настройки.

CREATE TABLE tenant
(
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            citext      NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,62}$'),
    name            text        NOT NULL,
    plan            text        NOT NULL DEFAULT 'FREE' CHECK (plan IN ('FREE', 'PRO', 'BUSINESS')),
    plan_expires_at timestamptz,
    status          text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('TRIAL', 'ACTIVE', 'SUSPENDED')),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER tenant_set_updated_at
    BEFORE UPDATE ON tenant FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE app_user
(
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        uuid        NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    keycloak_id      uuid        UNIQUE,
    email            citext      NOT NULL,
    first_name       text,
    last_name        text,
    phone            text,
    role             text        NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'EDITOR', 'REVIEWER', 'ANALYST')),
    status           text        NOT NULL DEFAULT 'INVITED' CHECK (status IN ('INVITED', 'ACTIVE', 'BLOCKED')),
    locale           text        NOT NULL DEFAULT 'ru',
    timezone         text        NOT NULL DEFAULT 'Europe/Moscow',
    quiet_hours_from time,
    quiet_hours_to   time,
    last_login_at    timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, email),
    UNIQUE (tenant_id, id)
);

CREATE INDEX app_user_tenant_status_idx ON app_user (tenant_id, status);

CREATE TRIGGER app_user_set_updated_at
    BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Матрица уведомлений: событие × канал доставки.
CREATE TABLE notification_setting
(
    user_id    uuid    NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    event_type text    NOT NULL CHECK (event_type IN ('POST_NEEDS_REVIEW', 'PUBLICATION_FAILED',
                                                      'LLM_BUDGET_EXCEEDED', 'TRENDS_DIGEST', 'PRODUCT_NEWS')),
    channel    text    NOT NULL CHECK (channel IN ('EMAIL', 'TELEGRAM', 'PUSH')),
    enabled    boolean NOT NULL DEFAULT true,
    PRIMARY KEY (user_id, event_type, channel)
);
