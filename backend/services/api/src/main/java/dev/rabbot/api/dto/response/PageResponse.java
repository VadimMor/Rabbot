package dev.rabbot.api.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

// Единый формат постраничного ответа для всех сервисов.
// Свой формат, а не Page из Spring: так JSON стабилен и не зависит от версии Spring Data.
public record PageResponse<T>(
        List<T> items,       // элементы текущей страницы
        int page,            // номер страницы, с 0
        int size,            // размер страницы
        long totalElements,  // всего элементов
        int totalPages       // всего страниц
) {

    // Превращает Page из Spring Data в формат.
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}