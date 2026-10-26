package dev.rabbot.api.enums;

// Жизненный цикл поста. Совпадает с CHECK в таблице post (V6).
public enum PostStatus {
    DRAFT,                // черновик
    GENERATING,           // LLM пишет текст
    IN_REVIEW,            // ждёт одобрения
    CHANGES_REQUESTED,    // ревьюер попросил правки
    APPROVED,             // одобрен
    SCHEDULED,            // стоит в расписании
    PUBLISHED,            // опубликован на всех площадках
    PARTIALLY_PUBLISHED,  // опубликован не везде
    FAILED,               // публикация упала
    ARCHIVED              // в архиве
}