package dev.rabbot.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Изменение поста. version — та версия, которую клиент получил в GET.
// Если пост успели изменить, версия в БД уже другая, и сервер вернёт 409.
public record PostUpdateRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @NotNull
        Integer version
) {
}