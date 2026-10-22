package dev.rabbot.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

// Тело запроса для POST (создать) и PUT (заменить) бренда.
// record — неизменяемый класс: поля, конструктор, геттеры, equals/hashCode.
// Аннотации валидации проверяются, когда в контроллере стоит @Valid.
public record BrandRequest(

        // Название обязательно, до 100 символов.
        @NotBlank
        @Size(max = 100)
        String name,

        // Описание аудитории, необязательно.
        @Size(max = 1000)
        String audience,

        // Код языка: "ru", "en" и т. п. Если не передан, сервис поставит "ru".
        @Pattern(regexp = "^[a-z]{2}$", message = "должен быть двухбуквенный код языка")
        String language,

        // До 10 слов тона, каждое непустое и до 40 символов.
        @Size(max = 10)
        List<@NotBlank @Size(max = 40) String> tone,

        // До 200 запрещённых слов.
        @Size(max = 200)
        List<@NotBlank @Size(max = 60) String> forbiddenWords,

        @Size(max = 200)
        String defaultCta,

        // Произвольные правила стиля: {"emoji": true, "maxLength": 800}.
        Map<String, Object> styleRules
) {
}