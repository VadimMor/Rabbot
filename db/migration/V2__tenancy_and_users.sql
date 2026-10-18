-- Тенанты (организации) и пользователи.
-- Пользователь аутентифицируется в Keycloak; здесь — профиль, роль и настройки.

-- Создание таблицы tenant (организация).
CREATE TABLE tenant
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Человеко-читаемый идентификатор, который можно использовать в URL.
    slug            citext      NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,62}$'),
    -- Название организации.
    name            text        NOT NULL,
    -- План подписки: FREE, PRO или BUSINESS.
    plan            text        NOT NULL DEFAULT 'FREE' CHECK (plan IN ('FREE', 'PRO', 'BUSINESS')),
    -- Дата окончания текущего плана подписки (null, если план FREE).
    plan_expires_at timestamptz,
    -- Статус организации: TRIAL, ACTIVE или SUSPENDED.
    status          text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('TRIAL', 'ACTIVE', 'SUSPENDED')),
    -- Дата создания записи
    created_at      timestamptz NOT NULL DEFAULT now(),
    -- Дата последнего обновления записи
    updated_at      timestamptz NOT NULL DEFAULT now()
);

-- Создание триггера для обновления поля updated_at в таблице tenant.
CREATE TRIGGER tenant_set_updated_at
    -- Триггер для автоматического обновления поля updated_at.
    BEFORE UPDATE ON tenant FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Создание таблицы app_user (пользователь).
CREATE TABLE app_user
(
    -- UUID, чтобы можно было использовать его в Keycloak.
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UUID организации, к которой принадлежит пользователь.
    tenant_id        uuid        NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    -- UUID пользователя в Keycloak (null, если пользователь ещё не зарегистрирован в Keycloak).
    keycloak_id      uuid        UNIQUE,
    -- Электронная почта пользователя (используется для входа в Keycloak).
    email            citext      NOT NULL,
    -- Имя пользователя (для отображения в интерфейсе).
    first_name       text,
    -- Фамилия пользователя (для отображения в интерфейсе).
    last_name        text,
    -- Телефон пользователя (для отображения в интерфейсе).
    phone            text,
    -- Роль пользователя в организации: OWNER, ADMIN, EDITOR, REVIEWER или ANALYST.
    role             text        NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'EDITOR', 'REVIEWER', 'ANALYST')),
    -- Статус пользователя: INVITED, ACTIVE или BLOCKED.
    status           text        NOT NULL DEFAULT 'INVITED' CHECK (status IN ('INVITED', 'ACTIVE', 'BLOCKED')),
    -- Местоположение пользователя (для отображения в интерфейсе).
    locale           text        NOT NULL DEFAULT 'ru',
    -- Часовой пояс пользователя (для отображения в интерфейсе).
    timezone         text        NOT NULL DEFAULT 'Europe/Moscow',
    -- Время начала "тихого часа" (не присылать уведомления в это время).
    quiet_hours_from time,
    -- Время окончания "тихого часа" (не присылать уведомления в это время).
    quiet_hours_to   time,
    -- Дата и время последнего входа пользователя в систему.
    last_login_at    timestamptz,
    -- Дата и время создания записи.
    created_at       timestamptz NOT NULL DEFAULT now(),
    -- Дата и время последнего обновления записи.
    updated_at       timestamptz NOT NULL DEFAULT now(),
    -- Уникальные ограничения на комбинацию tenant_id и email, а также на комбинацию tenant_id и id.
    UNIQUE (tenant_id, email),
    -- Уникальные ограничения на комбинацию tenant_id и id.
    UNIQUE (tenant_id, id)
);

-- Создание индекса для ускорения поиска пользователей по tenant_id и status.
CREATE INDEX app_user_tenant_status_idx ON app_user (tenant_id, status);

-- Создание триггера для обновления поля updated_at в таблице app_user.
CREATE TRIGGER app_user_set_updated_at
    -- Триггер для автоматического обновления поля updated_at.
    BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Матрица уведомлений: событие × канал доставки.
CREATE TABLE notification_setting
(
    -- UUID пользователя, которому принадлежат настройки уведомлений.
    user_id    uuid    NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    -- Тип события, на которое пользователь хочет получать уведомления.
    event_type text    NOT NULL CHECK (event_type IN ('POST_NEEDS_REVIEW', 'PUBLICATION_FAILED',
                                                      'LLM_BUDGET_EXCEEDED', 'TRENDS_DIGEST', 'PRODUCT_NEWS')),
    -- Канал доставки уведомлений: EMAIL, TELEGRAM или PUSH.
    channel    text    NOT NULL CHECK (channel IN ('EMAIL', 'TELEGRAM', 'PUSH')),
    -- Включено ли уведомление для данного события и канала.
    enabled    boolean NOT NULL DEFAULT true,
    -- Уникальный составной первичный ключ на комбинацию user_id, event_type и channel.
    PRIMARY KEY (user_id, event_type, channel)
);