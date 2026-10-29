package dev.rabbot.events;

// Итог попытки публикации.
public enum PublishOutcome {
    PUBLISHED,   // пост вышел в соцсети
    RETRY,       // временная ошибка (429, 5xx, таймаут) — scheduler повторит позже
    FAILED       // окончательно не вышел (неверный токен, текст отклонён) — в DLQ
}