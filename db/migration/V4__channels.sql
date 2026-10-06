-- Подключённые аккаунты соцсетей.
-- OAuth-токены в БД не хранятся: secret_ref — путь к секрету в Vault.

CREATE TABLE channel
(
    id                  uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           uuid            NOT NULL,
    brand_id            uuid            NOT NULL,
    platform            social_platform NOT NULL,
    external_account_id text            NOT NULL,
    handle              text,
    display_name        text            NOT NULL,
    status              text            NOT NULL DEFAULT 'CONNECTED'
                                        CHECK (status IN ('CONNECTED', 'NEEDS_REAUTH', 'ERROR', 'DISCONNECTED')),
    auto_publish        boolean         NOT NULL DEFAULT true,
    daily_post_limit    integer         NOT NULL DEFAULT 10 CHECK (daily_post_limit > 0),
    timezone            text            NOT NULL DEFAULT 'Europe/Moscow',
    secret_ref          text,
    token_expires_at    timestamptz,
    scopes              text[]          NOT NULL DEFAULT '{}',
    audience_size       integer,
    connected_by        uuid            REFERENCES app_user (id) ON DELETE SET NULL,
    connected_at        timestamptz     NOT NULL DEFAULT now(),
    last_error          text,
    created_at          timestamptz     NOT NULL DEFAULT now(),
    updated_at          timestamptz     NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, platform, external_account_id),
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, brand_id) REFERENCES brand (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX channel_brand_idx ON channel (brand_id);
-- Для фоновой задачи «обновить токены, которые скоро истекут».
CREATE INDEX channel_token_expiry_idx ON channel (token_expires_at) WHERE status = 'CONNECTED';

CREATE TRIGGER channel_set_updated_at
    BEFORE UPDATE ON channel FOR EACH ROW EXECUTE FUNCTION set_updated_at();
