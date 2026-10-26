package dev.rabbot.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// Тело PATCH: частичное изменение настроек канала.
// Все поля необязательные: null значит "не менять".
// Поэтому типы-обёртки (Boolean, Integer), а не примитивы.
public record ChannelSettingsRequest(

        Boolean autoPublish,

        @Min(1)
        @Max(100)
        Integer dailyPostLimit,

        // Например "Europe/Moscow". Корректность проверяет сервис.
        @Size(max = 64)
        String timezone
) {
}