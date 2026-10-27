package dev.rabbot.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Запрос правок: без комментария автору непонятно, что исправлять.
public record RequestChangesRequest(
        @NotBlank
        @Size(max = 2000)
        String comment
) {
}