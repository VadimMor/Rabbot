-- Общие расширения, домены и функции.
-- citext и pg_trgm — trusted extensions: их может создать владелец БД (rabbot_migrator).

-- Создание расширений citext и pg_trgm, если они ещё не созданы.
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Площадки, с которыми работает Rabbot.
CREATE DOMAIN social_platform AS text
    -- Проверка, что значение принадлежит одному из допустимых вариантов.
    CHECK (VALUE IN ('TELEGRAM', 'VK', 'INSTAGRAM', 'TIKTOK', 'LINKEDIN', 'YOUTUBE', 'DZEN', 'OK'));

-- Автообновление updated_at.
CREATE FUNCTION set_updated_at() RETURNS trigger
    -- Функция для автоматического обновления поля updated_at при изменении записи.
    LANGUAGE plpgsql AS
$$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END
$$;