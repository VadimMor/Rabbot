-- Создание базы данных для сервиса авторизации и воркспейсов
CREATE DATABASE rabbot_auth;

-- Создание базы данных для контента (тренды, черновики, аккаунты)
CREATE DATABASE rabbot_content;

-- Создание базы данных для ИИ-агента
CREATE DATABASE rabbot_ai_memory;

-- Подключаемся к ИИ базе и активируем расширение для векторного поиска
\c rabbot_ai_memory;
CREATE EXTENSION IF NOT EXISTS pgvector;