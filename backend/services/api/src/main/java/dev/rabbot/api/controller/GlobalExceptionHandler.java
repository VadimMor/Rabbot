package dev.rabbot.api.controller;

import dev.rabbot.api.utils.ConflictException;
import dev.rabbot.api.utils.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Перехватывает исключения из всех контроллеров и превращает их
// в ответ формата RFC 9457 (ProblemDetail):
// {"type":"about:blank","title":"Not Found","status":404,"detail":"...","instance":"/api/v1/brands/..."}
//
// Родитель ResponseEntityExceptionHandler уже умеет отвечать на стандартные
// ошибки Spring: невалидное тело (400), неверный JSON (400), ResponseStatusException и т. д.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Нет такого ресурса → 404.
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Конфликт бизнес-правил (дубликат имени) → 409.
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Ограничение БД всё равно сработало: гонка двух запросов
    // или несуществующий tenant_id (внешний ключ) → тоже 409.
    // Текст ошибки БД наружу не отдаём: в нём видна структура таблиц.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Данные нарушают ограничения базы данных");
    }

    // Гонка: два запроса одновременно прочитали версию 3 и оба сохраняют.
    // Первый выполнит UPDATE ... WHERE version = 3, второй не найдёт строку
    // с version = 3, и Hibernate бросит это исключение → 409.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Данные изменил кто-то другой. Обновите страницу и повторите.");
    }
}