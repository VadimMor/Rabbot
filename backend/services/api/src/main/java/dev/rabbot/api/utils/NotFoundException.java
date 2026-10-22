package dev.rabbot.api.utils;

// Ресурс не найден. GlobalExceptionHandler превращает её в HTTP 404.
// RuntimeException не нужно объявлять в throws, и транзакция откатится.
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}