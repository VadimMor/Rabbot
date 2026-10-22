package dev.rabbot.api.utils;

// Нарушено бизнес-правило (например, имя бренда занято) → HTTP 409.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}