package dev.rabbot.api.enums;

// Решение ревью. Совпадает с CHECK в таблице review (V6).
public enum ReviewDecision {
    APPROVED,
    CHANGES_REQUESTED,
    REJECTED    // зарезервировано: отклонение без права доработки
}