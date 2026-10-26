package dev.rabbot.api.dto.request;

import dev.rabbot.api.enums.PostKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

// Создание поста. Пост всегда создаётся черновиком (DRAFT).
public record PostCreateRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @NotNull
        PostKind kind,

        // Необязательно: тренд, из которого родилась идея.
        UUID trendId,

        // @Valid — проверить и каждый вложенный VariantRequest.
        @Valid
        @Size(max = 20)
        List<VariantRequest> variants
) {
}