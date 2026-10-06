-- Общие расширения, домены и функции.
-- citext и pg_trgm — trusted extensions: их может создать владелец БД (rabbot_migrator).

CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Площадки, с которыми работает Rabbot.
CREATE DOMAIN social_platform AS text
    CHECK (VALUE IN ('TELEGRAM', 'VK', 'INSTAGRAM', 'TIKTOK', 'LINKEDIN', 'YOUTUBE', 'DZEN', 'OK'));

-- Автообновление updated_at.
CREATE FUNCTION set_updated_at() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END
$$;
