-- Демо-данные для локальной разработки (повторяемая миграция Flyway).
-- Подключается только через FLYWAY_LOCATIONS=...,filesystem:/flyway/seed — в проде не используется.
-- Идемпотентна: фиксированные id + ON CONFLICT DO NOTHING.

-- Тенант
INSERT INTO tenant (id, slug, name, status)
VALUES ('00000000-0000-0000-0000-000000000001', 'rabbot-coffee', 'Rabbot Coffee', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- Пользователи
INSERT INTO app_user (id, tenant_id, email, first_name, last_name, role, status, last_login_at)
VALUES ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000001', 'blue.rabbit@rabbot.dev', 'Blue', 'Rabbit', 'OWNER', 'ACTIVE', now()),
       ('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000001', 'anna@rabbot.dev', 'Анна', 'Кузнецова', 'EDITOR', 'ACTIVE', now() - interval '12 minutes'),
       ('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000001', 'ilya@rabbot.dev', 'Илья', 'Смирнов', 'REVIEWER', 'ACTIVE', now() - interval '2 hours'),
       ('00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0000-000000000001', 'dmitry@rabbot.dev', 'Дмитрий', 'Орлов', 'ANALYST', 'INVITED', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO notification_setting (user_id, event_type, channel, enabled)
SELECT '00000000-0000-0000-0001-000000000001', e, c, NOT (e = 'PRODUCT_NEWS' OR (e = 'TRENDS_DIGEST' AND c = 'EMAIL'))
FROM unnest(ARRAY ['POST_NEEDS_REVIEW','PUBLICATION_FAILED','LLM_LIMIT_EXCEEDED','TRENDS_DIGEST','PRODUCT_NEWS']) e,
     unnest(ARRAY ['EMAIL','TELEGRAM']) c
ON CONFLICT DO NOTHING;

-- Бренд
INSERT INTO brand (id, tenant_id, name, audience, tone, forbidden_words, default_cta, style_rules)
VALUES ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000001', 'Rabbot Coffee',
        'Городские профессионалы 25–40, любят кофе и технологии',
        ARRAY ['дружелюбный','экспертный','с лёгким юмором'],
        ARRAY ['дешёвый','лучший в мире','гарантируем'],
        'Заходите на чашку, расскажем больше',
        '{"maxEmoji": 2, "maxHashtags": 3, "capsLock": false}')
ON CONFLICT (id) DO NOTHING;

INSERT INTO brand_example (id, tenant_id, brand_id, platform, body, source, engagement_rate)
VALUES ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001', 'TELEGRAM',
        'Утро начинается не с кофе, а с выбора зерна. Сегодня у нас Эфиопия с нотами черники — заходите попробовать.', 'MANUAL', 0.0640),
       ('00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001', 'VK',
        'Раф с халвой вернулся. Да, тот самый. Да, по субботам.', 'TOP_PERFORMER', 0.0910)
ON CONFLICT (id) DO NOTHING;

-- Каналы (токены — только ссылки на Vault)
INSERT INTO channel (id, tenant_id, brand_id, platform, external_account_id, handle, display_name, status,
                     secret_ref, token_expires_at, scopes, audience_size, connected_by)
VALUES ('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001',
        'TELEGRAM', '-1001234567890', '@rabbot_coffee', 'Rabbot Coffee', 'CONNECTED',
        'secret/rabbot/channels/tg-rabbot-coffee', NULL, ARRAY ['post'], 12400, '00000000-0000-0000-0001-000000000001'),
       ('00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001',
        'VK', '-123456789', 'rabbot_coffee', 'Rabbot Coffee', 'CONNECTED',
        'secret/rabbot/channels/vk-rabbot-coffee', '2026-12-12', ARRAY ['wall','photos','video'], 8100, '00000000-0000-0000-0001-000000000001'),
       ('00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001',
        'INSTAGRAM', '17841400000000000', '@rabbot.coffee', 'Rabbot Coffee', 'NEEDS_REAUTH',
        'secret/rabbot/channels/ig-rabbot-coffee', now() + interval '5 days', ARRAY ['content_publish','insights'], 5600, '00000000-0000-0000-0001-000000000001')
ON CONFLICT (id) DO NOTHING;

-- Тренды
INSERT INTO trend (id, cluster_key, title, summary, language, sources, mention_count, growth_pct, score)
VALUES ('00000000-0000-0000-0005-000000000001', 'local-llm-small-business', 'Локальные LLM для малого бизнеса',
        'Малый бизнес переносит генерацию текстов на собственные GPU ради экономии и приватности.', 'ru', ARRAY ['reddit','habr'], 42, 312.00, 94.00),
       ('00000000-0000-0000-0005-000000000002', 'java-25-virtual-threads', 'Java 25: виртуальные потоки в проде',
        'Команды делятся опытом перехода на виртуальные потоки.', 'ru', ARRAY ['hackernews','rss'], 31, 187.00, 88.00),
       ('00000000-0000-0000-0005-000000000003', 'coffee-ai-barista', 'Кофейни и ИИ-бариста: кейсы',
        'Кофейни пробуют ИИ для меню, соцсетей и программ лояльности.', 'ru', ARRAY ['youtube'], 19, 96.00, 77.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO brand_trend (tenant_id, brand_id, trend_id, relevance, angle)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0005-000000000001', 0.9100, 'Как кофейня сэкономила на ИИ, перенеся его на свой GPU'),
       ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0005-000000000003', 0.8700, 'Наш бариста-бот и голос бренда')
ON CONFLICT DO NOTHING;

-- Пост и варианты
INSERT INTO post (id, tenant_id, brand_id, trend_id, kind, title, status, brand_score, created_by, approved_by, approved_at)
VALUES ('00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001',
        '00000000-0000-0000-0005-000000000001', 'TEXT', 'Свой GPU вместо подписки', 'SCHEDULED', 92,
        '00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0001-000000000003', now() - interval '1 hour'),
       ('00000000-0000-0000-0006-000000000002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001',
        '00000000-0000-0000-0005-000000000003', 'REEL', 'Платите за ИИ больше, чем за кофе?', 'IN_REVIEW', 88,
        '00000000-0000-0000-0001-000000000002', NULL, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO post_variant (id, tenant_id, post_id, platform, body, hashtags, is_selected, draft_model, final_model)
VALUES ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0006-000000000001', 'TELEGRAM',
        'Кофейня на углу тратила 9 000 руб. в месяц на ИИ-сервисы. Мы перенесли черновики на локальную модель: расходы упали на 40%.',
        ARRAY ['#кофе','#ИИ'], true, 'qwen3-14b', 'cloud/default'),
       ('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0006-000000000001', 'VK',
        'Свой GPU вместо подписки: как кофейня сэкономила 40% на ИИ.', ARRAY ['#кофе','#ИИ'], true, 'qwen3-14b', 'cloud/default'),
       ('00000000-0000-0000-0007-000000000003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0006-000000000002', 'INSTAGRAM',
        'Свой GPU вместо подписки. Как кофейня сэкономила 40% на ИИ', ARRAY ['#кофе','#ИИ','#reels'], true, 'qwen3-14b', 'cloud/default')
ON CONFLICT (id) DO NOTHING;

INSERT INTO reel_scene (id, tenant_id, post_id, ord, role, start_ms, end_ms, script, subtitle)
VALUES ('00000000-0000-0000-0008-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0006-000000000002', 1, 'HOOK', 0, 3000, 'Вы платите за ИИ больше, чем за кофе?', 'Платите за ИИ больше, чем за кофе?'),
       ('00000000-0000-0000-0008-000000000002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0006-000000000002', 2, 'PROBLEM', 3000, 8000, 'Подписки на ИИ-сервисы съедают бюджет кофейни.', NULL),
       ('00000000-0000-0000-0008-000000000003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0006-000000000002', 3, 'SOLUTION', 8000, 18000, 'Rabbot пишет посты на локальной модели прямо на вашем GPU.', NULL),
       ('00000000-0000-0000-0008-000000000004', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0006-000000000002', 4, 'CTA', 18000, 22000, 'Напишите «GPU» в комментариях: пришлём чек-лист.', NULL)
ON CONFLICT (id) DO NOTHING;

-- Публикации: создаём, только если у демо-поста их ещё нет
INSERT INTO publication (tenant_id, post_variant_id, channel_id, scheduled_at, status, published_at, external_post_id)
SELECT '00000000-0000-0000-0000-000000000001', v.variant_id::uuid, v.channel_id::uuid, v.at, v.status, v.published_at, v.ext
FROM (VALUES ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0004-000000000001', now() - interval '3 hours', 'PUBLISHED', now() - interval '3 hours', '4021'),
             ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0004-000000000001', now() + interval '2 hours', 'SCHEDULED', NULL::timestamptz, NULL),
             ('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0004-000000000002', now() + interval '5 hours', 'SCHEDULED', NULL::timestamptz, NULL))
         AS v(variant_id, channel_id, at, status, published_at, ext)
WHERE NOT EXISTS (SELECT 1 FROM publication p WHERE p.tenant_id = '00000000-0000-0000-0000-000000000001');

-- Лимит токенов облачных LLM на текущий месяц
INSERT INTO llm_quota (tenant_id, period_month, limit_tokens, used_tokens)
VALUES ('00000000-0000-0000-0000-000000000001', date_trunc('month', now())::date, 5000000, 1250000)
ON CONFLICT DO NOTHING;