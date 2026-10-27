package dev.rabbot.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// Новый текст варианта
public record VariantTextRequest(

        @NotNull
        @Size(max = 10_000)
        String body,

        @Size(max = 30)
        List<@Pattern(regexp = "^#[\\p{L}\\p{N}_]{1,60}$", message = "хэштег должен быть вида #слово") String> hashtags
) {
}