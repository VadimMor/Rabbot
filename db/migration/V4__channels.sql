-- Подключённые аккаунты соцсетей.
-- OAuth-токены в БД не хранятся: secret_ref — путь к секрету в Vault.

-- Типы платформ соцсетей.
CREATE TABLE channel
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id                  uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит аккаунт.
    tenant_id           uuid            NOT NULL,
    -- UUID бренда, к которому принадлежит аккаунт.
    brand_id            uuid            NOT NULL,
    -- Площадка соцсети, к которой подключён аккаунт.
    platform            social_platform NOT NULL,
    -- ID аккаунта в соцсети.
    external_account_id text            NOT NULL,
    -- Имя пользователя в соцсети (для отображения в интерфейсе).
    handle              text,
    -- Название аккаунта в соцсети (для отображения в интерфейсе).
    display_name        text            NOT NULL,
    -- Статус подключения аккаунта: CONNECTED, NEEDS_REAUTH, ERROR или DISCONNECTED.
    status              text            NOT NULL DEFAULT 'CONNECTED'
                                        CHECK (status IN ('CONNECTED', 'NEEDS_REAUTH', 'ERROR', 'DISCONNECTED')),
    -- Автоматическая публикация постов в соцсети: true или false.
    auto_publish        boolean         NOT NULL DEFAULT true,
    -- Лимит постов в день для автоматической публикации.
    daily_post_limit    integer         NOT NULL DEFAULT 10 CHECK (daily_post_limit > 0),
    -- Временной пояс аккаунта (для отображения в интерфейсе).
    timezone            text            NOT NULL DEFAULT 'Europe/Moscow',
    -- Ссылка на секрет в Vault, где хранится OAuth-токен.
    secret_ref          text,
    -- Дата и время истечения OAuth-токена.
    token_expires_at    timestamptz,
    -- Список областей доступа (scopes) OAuth-токена.
    scopes              text[]          NOT NULL DEFAULT '{}',
    -- Размер аудитории аккаунта (для отображения в интерфейсе).
    audience_size       integer,
    -- UUID пользователя, который подключил аккаунт (null, если аккаунт подключен через сервисный токен).
    connected_by        uuid            REFERENCES app_user (id) ON DELETE SET NULL,
    -- Дата и время подключения аккаунта.
    connected_at        timestamptz     NOT NULL DEFAULT now(),
    -- Последняя ошибка при попытке публикации поста в соцсети (null, если ошибок не было).
    last_error          text,
    -- Дата и время создания записи.
    created_at          timestamptz     NOT NULL DEFAULT now(),
    -- Дата и время последнего обновления записи.
    updated_at          timestamptz     NOT NULL DEFAULT now(),
    -- Уникальные ограничения на комбинацию tenant_id, platform и external_account_id.
    UNIQUE (tenant_id, platform, external_account_id),
    -- Уникальные ограничения на комбинацию tenant_id и id.
    UNIQUE (tenant_id, id),
    -- Внешний ключ на комбинацию tenant_id и brand_id, с каскадным удалением.
    FOREIGN KEY (tenant_id, brand_id) REFERENCES brand (tenant_id, id) ON DELETE CASCADE
);

-- Создание индекса для ускорения поиска аккаунтов по brand_id.
CREATE INDEX channel_brand_idx ON channel (brand_id);
-- Для фоновой задачи «обновить токены, которые скоро истекут».
-- Создание частичного индекса для ускорения поиска аккаунтов с истекающими токенами.
CREATE INDEX channel_token_expiry_idx ON channel (token_expires_at) WHERE status = 'CONNECTED';

-- Создание триггера для обновления поля updated_at в таблице channel.
CREATE TRIGGER channel_set_updated_at
    -- Триггер для автоматического обновления поля updated_at.
    BEFORE UPDATE ON channel FOR EACH ROW EXECUTE FUNCTION set_updated_at();