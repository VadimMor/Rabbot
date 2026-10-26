package dev.rabbot.api.dto.request;

import dev.rabbot.api.enums.SocialPlatform;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// Текст варианта под одну площадку.
public record VariantRequest(

        @NotNull
        SocialPlatform platform,

        // Пустой текст допустим: вариант-заготовка, который потом допишут.
        @NotNull
        @Size(max = 10_000)
        String body,

        // Хэштег: "#" и потом буквы/цифры/подчёркивание, без пробелов.
        @Size(max = 30)
        List<@Pattern(regexp = "^#[\\p{L}\\p{N}_]{1,60}$", message = "хэштег должен быть вида #слово") String> hashtags
) {
}